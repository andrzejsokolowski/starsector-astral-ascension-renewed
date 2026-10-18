package astralascensionrenewed.campaign;

import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignEngineLayers;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.BaseCustomEntityPlugin;

/**
 * The orbital ring around a Hyper-Ecumenopolis. A separate entity that follows its planet: the far half of
 * the ring is drawn before the planet and the near half after it, so the planet sits inside the ring.
 * The ring removes itself when the colony stops being a Hyper-Ecumenopolis, however that happens.
 */
public class OrbitalRing extends BaseCustomEntityPlugin {

    public static final String ENTITY_TYPE = "aar_orbital_ring";

    private static final int SEGMENTS = 192;
    /** Middle of the ring, in planet radii. */
    private static final float MIDDLE = 1.55f;
    /** Half the ring's width, in planet radii. */
    private static final float HALF_WIDTH = 0.17f;
    /** Seconds for the ring's structure to go once round. */
    private static final float TURN_SECONDS = 240f;

    private PlanetAPI planet;
    private float turn;

    private transient SpriteAPI structure;
    private transient SpriteAPI lights;

    @Override
    public void init(SectorEntityToken entity, Object params) {
        super.init(entity, params);
        if (params instanceof PlanetAPI) planet = (PlanetAPI) params;
        follow();
    }

    public PlanetAPI getPlanet() {
        return planet;
    }

    @Override
    public void advance(float amount) {
        if (!stillHyperEcumenopolis()) {
            entity.setExpired(true);
            return;
        }
        follow();
        turn = (turn + amount / TURN_SECONDS) % 1f;
    }

    private boolean stillHyperEcumenopolis() {
        if (planet == null || planet.isExpired() || planet.getContainingLocation() != entity.getContainingLocation()) {
            return false;
        }
        MarketAPI market = planet.getMarket();
        return market != null && !market.isPlanetConditionMarketOnly()
                && market.hasCondition(Ascension.HYPER_ECUMENOPOLIS_CONDITION);
    }

    private void follow() {
        if (planet != null) entity.getLocation().set(planet.getLocation());
    }

    @Override
    public float getRenderRange() {
        return planet == null ? 1000f : planet.getRadius() * (MIDDLE + HALF_WIDTH) + 500f;
    }

    @Override
    public void render(CampaignEngineLayers layer, ViewportAPI viewport) {
        if (planet == null) return;
        float alpha = viewport.getAlphaMult();
        if (alpha <= 0f) return;
        if (structure == null) {
            structure = Global.getSettings().getSprite("aar", "orbital_ring");
            lights = Global.getSettings().getSprite("aar", "orbital_ring_lights");
        }
        // The far half (local y above the ring's long axis) goes behind the planet.
        boolean far = layer == CampaignEngineLayers.TERRAIN_2;
        draw(far, alpha);
    }

    private void draw(boolean far, float alpha) {
        float r = planet.getRadius();
        float inner = r * (MIDDLE - HALF_WIDTH);
        float outer = r * (MIDDLE + HALF_WIDTH);
        // Seen from the same angle as the planet's equator.
        float pitch = planet.getSpec() == null ? 20f : Math.abs(planet.getSpec().getPitch());
        float flat = clamp((float) Math.sin(Math.toRadians(pitch)), 0.26f, 0.55f);
        float tilt = planet.getSpec() == null ? 0f : (float) Math.toRadians(planet.getSpec().getTilt());
        float cosT = (float) Math.cos(tilt);
        float sinT = (float) Math.sin(tilt);

        Vector2f center = planet.getLocation();
        Vector2f toStar = new Vector2f(-1f, 0f);
        SectorEntityToken star = planet.getLightSource();
        if (star != null) {
            Vector2f.sub(star.getLocation(), center, toStar);
            if (toStar.lengthSquared() > 1f) toStar.normalise();
            else toStar.set(-1f, 0f);
        }

        float[] ix = new float[SEGMENTS + 1], iy = new float[SEGMENTS + 1];
        float[] ox = new float[SEGMENTS + 1], oy = new float[SEGMENTS + 1];
        float[] lit = new float[SEGMENTS + 1];
        float start = turn * (float) Math.PI * 2f;
        for (int k = 0; k <= SEGMENTS; k++) {
            float a = start + k * (float) Math.PI * 2f / SEGMENTS;
            float c = (float) Math.cos(a);
            float s = (float) Math.sin(a) * flat;
            ix[k] = center.x + (c * cosT - s * sinT) * inner;
            iy[k] = center.y + (c * sinT + s * cosT) * inner;
            ox[k] = center.x + (c * cosT - s * sinT) * outer;
            oy[k] = center.y + (c * sinT + s * cosT) * outer;
            lit[k] = light((ix[k] + ox[k]) * 0.5f - center.x, (iy[k] + oy[k]) * 0.5f - center.y, toStar, r);
        }

        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);

        structure.bindTexture();
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        quads(far, start, ix, iy, ox, oy, lit, structure, alpha, false);

        lights.bindTexture();
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        quads(far, start, ix, iy, ox, oy, lit, lights, alpha, true);

        GL11.glPopMatrix();
    }

    private static void quads(boolean far, float start, float[] ix, float[] iy, float[] ox, float[] oy,
                              float[] lit, SpriteAPI sprite, float alpha, boolean night) {
        float tw = sprite.getTextureWidth();
        float th = sprite.getTextureHeight();
        GL11.glBegin(GL11.GL_QUADS);
        for (int k = 0; k < SEGMENTS; k++) {
            float mid = start + (k + 0.5f) * (float) Math.PI * 2f / SEGMENTS;
            if ((Math.sin(mid) > 0) != far) continue;
            float u0 = tw * k / SEGMENTS;
            float u1 = tw * (k + 1) / SEGMENTS;
            vertex(lit[k], alpha, night, u0, th, ox[k], oy[k]);
            vertex(lit[k], alpha, night, u0, 0f, ix[k], iy[k]);
            vertex(lit[k + 1], alpha, night, u1, 0f, ix[k + 1], iy[k + 1]);
            vertex(lit[k + 1], alpha, night, u1, th, ox[k + 1], oy[k + 1]);
        }
        GL11.glEnd();
    }

    private static void vertex(float lit, float alpha, boolean night, float u, float v, float x, float y) {
        if (night) {
            // City lights show on the dark side and fade out in full sunlight.
            GL11.glColor4f(1f, 1f, 1f, alpha * (1f - 0.75f * lit));
        } else {
            float b = 0.18f + 0.82f * lit;
            GL11.glColor4f(b, b, b, alpha);
        }
        GL11.glTexCoord2f(u, v);
        GL11.glVertex2f(x, y);
    }

    /** 1 in sunlight, low inside the planet's shadow. */
    private static float light(float px, float py, Vector2f toStar, float radius) {
        float along = px * toStar.x + py * toStar.y;
        if (along >= 0f) return 1f;
        float perpX = px - along * toStar.x;
        float perpY = py - along * toStar.y;
        float perp = (float) Math.sqrt(perpX * perpX + perpY * perpY);
        float edge = clamp((perp - radius * 0.85f) / (radius * 0.3f), 0f, 1f);
        return 0.15f + 0.85f * edge;
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}
