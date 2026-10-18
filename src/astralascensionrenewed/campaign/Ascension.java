package astralascensionrenewed.campaign;

import java.awt.Color;
import java.util.Random;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.PlanetSpecAPI;
import com.fs.starfarer.api.campaign.comm.CommMessageAPI.MessageClickAction;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.econ.AICoreAdmin;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.impl.campaign.intel.MessageIntel;
import com.fs.starfarer.api.util.Misc;

/**
 * Colony sizes past 10: who may have them, what they look like, and what happens when they lose
 * the Omega core that holds them together.
 */
public final class Ascension {

    /** The largest size the base game knows. Past it, only an Omega core can govern. */
    public static final int VANILLA_MAX = 10;
    public static final int MAX_SIZE = 15;
    /** Where the Great Civil War leaves a colony. */
    public static final int CIVIL_WAR_SIZE = 8;

    public static final String ARCOLOGY_CONDITION = "aar_arcology_world";
    public static final String ECUMENOPOLIS_CONDITION = "aar_ecumenopolis";
    public static final String ARCOLOGY_PLANET = "aar_arcology";
    public static final String ECUMENOPOLIS_PLANET = "aar_ecumenopolis";

    private static final String LOOK = "$aar_look_";
    private static final String LOOK_STAGE = LOOK + "stage";
    private static final String CIVIL_WAR_ID = "aar_great_civil_war";

    private Ascension() {}

    public static boolean isOmegaGoverned(MarketAPI market) {
        PersonAPI admin = market.getAdmin();
        return admin != null && Commodities.OMEGA_CORE.equals(admin.getAICoreId());
    }

    /** Too big for anything but an Omega core. */
    public static boolean needsOmega(MarketAPI market) {
        return market.getSize() > VANILLA_MAX;
    }

    private static final String ASCENDED = "$aar_ascended";

    /**
     * Grew past size 10 under an Omega core. Only these colonies get stages, looks and the Great Civil
     * War; a big world placed by another mod is not this mod's business.
     */
    public static boolean isAscended(MarketAPI market) {
        return market.getMemoryWithoutUpdate().getBoolean(ASCENDED);
    }

    static void markAscended(MarketAPI market) {
        market.getMemoryWithoutUpdate().set(ASCENDED, true);
    }

    // ------------------------------------------------------------------------- size

    /** Sets the size the same way the base game grows and shrinks colonies, but for any size. */
    public static void setSize(MarketAPI market, int size) {
        size = Math.max(1, Math.min(MAX_SIZE, size));
        for (int i = 0; i <= MAX_SIZE; i++) {
            market.removeCondition("population_" + i);
        }
        market.addCondition("population_" + size);
        market.setSize(size);
        market.getPopulation().setWeight(Misc.getImmigrationPlugin(market).getWeightForMarketSize(size));
        market.getPopulation().normalize();
        market.reapplyConditions();
        market.reapplyIndustries();
        updateStage(market);
    }

    // ------------------------------------------------------------------------- stages and looks

    /** The stage condition and the planet's look follow the size. Cheap to call every frame. */
    public static void updateStage(MarketAPI market) {
        int size = market.getSize();
        String want = size >= 13 ? ECUMENOPOLIS_CONDITION : size > VANILLA_MAX ? ARCOLOGY_CONDITION : null;
        boolean changed = false;
        for (String stage : new String[] {ARCOLOGY_CONDITION, ECUMENOPOLIS_CONDITION}) {
            if (!stage.equals(want) && market.hasCondition(stage)) {
                market.removeCondition(stage);
                changed = true;
            }
        }
        if (want != null && !market.hasCondition(want)) {
            market.addCondition(want);
            changed = true;
        }
        if (changed) {
            market.reapplyConditions();
            market.reapplyIndustries();
        }

        String look = ECUMENOPOLIS_CONDITION.equals(want) ? ECUMENOPOLIS_PLANET
                : ARCOLOGY_CONDITION.equals(want) ? ARCOLOGY_PLANET : null;
        MemoryAPI memory = market.getMemoryWithoutUpdate();
        if (want == null) memory.unset(ASCENDED);
        String current = memory.contains(LOOK_STAGE) ? memory.getString(LOOK_STAGE) : null;
        if (look == null ? current == null : look.equals(current)) return;
        if (look == null) {
            restoreLook(market);
        } else {
            applyLook(market, look);
        }
    }

    private static void applyLook(MarketAPI market, String type) {
        MemoryAPI memory = market.getMemoryWithoutUpdate();
        memory.set(LOOK_STAGE, type);
        PlanetAPI planet = market.getPlanetEntity();
        if (planet == null || planet.isStar()) return;
        if (!memory.contains(LOOK + "type")) snapshot(planet, memory);

        PlanetSpecAPI before = planet.getSpec();
        float tilt = before.getTilt();
        float pitch = before.getPitch();
        planet.changeType(type, new Random(market.getId().hashCode()));
        if (!type.equals(planet.getTypeId())) return;
        // Keep the planet turned the way it was; only the surface changes.
        planet.getSpec().setTilt(tilt);
        planet.getSpec().setPitch(pitch);
        planet.applySpecChanges();
    }

    /**
     * Gives the planet back exactly the look it had before it grew past size 10, custom textures
     * from other mods included.
     */
    private static void restoreLook(MarketAPI market) {
        MemoryAPI memory = market.getMemoryWithoutUpdate();
        memory.unset(LOOK_STAGE);
        PlanetAPI planet = market.getPlanetEntity();
        if (planet == null || !memory.contains(LOOK + "type")) return;

        planet.changeType(memory.getString(LOOK + "type"), new Random(market.getId().hashCode()));
        PlanetSpecAPI spec = planet.getSpec();
        spec.setTexture(string(memory, "texture"));
        spec.setCloudTexture(string(memory, "cloudTexture"));
        spec.setGlowTexture(string(memory, "glowTexture"));
        spec.setGlowColor(color(memory, "glowColor"));
        spec.setUseReverseLightForGlow(memory.getBoolean(LOOK + "reverseGlow"));
        spec.setPlanetColor(color(memory, "planetColor"));
        spec.setAtmosphereColor(color(memory, "atmosphereColor"));
        spec.setAtmosphereThickness(memory.getFloat(LOOK + "atmosphereThickness"));
        spec.setAtmosphereThicknessMin(memory.getFloat(LOOK + "atmosphereThicknessMin"));
        spec.setCloudColor(color(memory, "cloudColor"));
        spec.setCloudRotation(memory.getFloat(LOOK + "cloudRotation"));
        spec.setIconColor(color(memory, "iconColor"));
        spec.setTilt(memory.getFloat(LOOK + "tilt"));
        spec.setPitch(memory.getFloat(LOOK + "pitch"));
        spec.setRotation(memory.getFloat(LOOK + "rotation"));
        planet.applySpecChanges();

        for (String key : new String[] {"type", "texture", "cloudTexture", "glowTexture", "glowColor", "reverseGlow",
                "planetColor", "atmosphereColor", "atmosphereThickness", "atmosphereThicknessMin", "cloudColor",
                "cloudRotation", "iconColor", "tilt", "pitch", "rotation"}) {
            memory.unset(LOOK + key);
        }
    }

    private static void snapshot(PlanetAPI planet, MemoryAPI memory) {
        PlanetSpecAPI spec = planet.getSpec();
        memory.set(LOOK + "type", planet.getTypeId());
        memory.set(LOOK + "texture", spec.getTexture());
        memory.set(LOOK + "cloudTexture", spec.getCloudTexture());
        memory.set(LOOK + "glowTexture", spec.getGlowTexture());
        memory.set(LOOK + "glowColor", rgb(spec.getGlowColor()));
        memory.set(LOOK + "reverseGlow", spec.isUseReverseLightForGlow());
        memory.set(LOOK + "planetColor", rgb(spec.getPlanetColor()));
        memory.set(LOOK + "atmosphereColor", rgb(spec.getAtmosphereColor()));
        memory.set(LOOK + "atmosphereThickness", spec.getAtmosphereThickness());
        memory.set(LOOK + "atmosphereThicknessMin", spec.getAtmosphereThicknessMin());
        memory.set(LOOK + "cloudColor", rgb(spec.getCloudColor()));
        memory.set(LOOK + "cloudRotation", spec.getCloudRotation());
        memory.set(LOOK + "iconColor", rgb(spec.getIconColor()));
        memory.set(LOOK + "tilt", spec.getTilt());
        memory.set(LOOK + "pitch", spec.getPitch());
        memory.set(LOOK + "rotation", spec.getRotation());
    }

    private static Integer rgb(Color c) {
        return c == null ? null : c.getRGB();
    }

    private static Color color(MemoryAPI memory, String key) {
        Object value = memory.get(LOOK + key);
        return value instanceof Integer ? new Color((Integer) value, true) : null;
    }

    private static String string(MemoryAPI memory, String key) {
        Object value = memory.get(LOOK + key);
        return value instanceof String ? (String) value : null;
    }

    // ------------------------------------------------------------------------- the Great Civil War

    /**
     * A colony past size 10 without an Omega core to govern it tears itself apart: it ends at size 8
     * with an armed, ungoverned population in the ruins, and half a year of unrest.
     */
    public static void greatCivilWar(MarketAPI market) {
        String name = market.getName();
        int from = market.getSize();
        if (from > CIVIL_WAR_SIZE) setSize(market, CIVIL_WAR_SIZE);
        if (!market.hasCondition(Conditions.DECIVILIZED_SUBPOP)) market.addCondition(Conditions.DECIVILIZED_SUBPOP);
        market.getStability().addTemporaryModFlat(180f, CIVIL_WAR_ID, "Great Civil War", -5f);
        market.reapplyConditions();
        market.reapplyIndustries();
        updateStage(market);

        MessageIntel intel = new MessageIntel("Great Civil War - " + name, Misc.getNegativeHighlightColor());
        intel.addLine(BaseIntelPlugin.BULLET + "Size %s to %s", Misc.getTextColor(),
                new String[] {"" + from, "" + market.getSize()}, Misc.getHighlightColor(), Misc.getHighlightColor());
        intel.addLine(BaseIntelPlugin.BULLET + "An ungoverned population is left in the ruins");
        intel.setIcon(Global.getSettings().getSpriteName("intel", "hostilities"));
        intel.setSound(BaseIntelPlugin.getSoundMajorPosting());
        Global.getSector().getCampaignUI().addMessage(intel, MessageClickAction.COLONY_INFO, market);
    }

    /**
     * The game decides whether an AI administrator may simply be unplugged through the market's
     * hidden ai_core_admin condition. A colony that had an Alpha core before this mod was installed
     * still carries the base game's version of it, so it is swapped for this mod's.
     */
    static void makeOmegaAware(MarketAPI market) {
        if (AICoreAdmin.get(market) instanceof OmegaAwareAICoreAdmin) return;
        market.removeCondition(Conditions.AI_CORE_ADMIN);
        market.addCondition(Conditions.AI_CORE_ADMIN);
    }
}
