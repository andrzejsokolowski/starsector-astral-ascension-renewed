package astralascensionrenewed.campaign;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketImmigrationModifier;
import com.fs.starfarer.api.campaign.econ.MutableCommodityQuantity;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.MutableStat.StatMod;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.population.PopulationComposition;
import com.fs.starfarer.api.util.Misc;

import astralascensionrenewed.Settings;

/**
 * Runs every frame, also while paused, so a change made in the colony screen takes hold at once.
 *
 * For every colony governed by an Omega core it lifts the size limit to 15, doubles production and
 * halves demand (Ethereal Enlightenment), and slows growth past size 10. For every colony past size
 * 10 it keeps the stage and planet look in step with the size, caps AoTD's size doubling, and starts
 * the Great Civil War if the Omega core is gone.
 */
public final class AscensionScript implements EveryFrameScript {

    static final String TOOLBOX_ID = "aotd_theory_of_toolbox";

    private static final String SIZE_LIMIT = "aar_omega_size_limit";
    private static final String PRODUCTION = "aar_omega_production";
    private static final String DEMAND = "aar_omega_demand";
    private static final String TOOLBOX_CAP = "aar_toolbox_cap";
    private static final String OMEGA_ADMIN = "Omega core administrator";

    /** Theory of Toolbox's own modifier ids. */
    private static final String TOOLBOX_SCALE = "aotd_new_scale";
    private static final String TOOLBOX_FOOD = "aotd_toolbox_food_prod";

    private static final Logger log = Global.getLogger(AscensionScript.class);

    private final Ledger ledger = new Ledger();
    private final MarketImmigrationModifier strain = new GrowthStrain();
    private final boolean toolbox = Global.getSettings().getModManager().isModEnabled(TOOLBOX_ID);
    private boolean failed;

    private AscensionScript() {}

    /** Called on every game load. Script and plugin are transient and are rebuilt each time. */
    public static void install() {
        SectorAPI sector = Global.getSector();
        sector.removeTransientScriptsOfClass(AscensionScript.class);
        sector.unregisterPlugin(AarCampaignPlugin.ID);
        sector.registerPlugin(new AarCampaignPlugin());
        sector.addTransientScript(new AscensionScript());
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return true;
    }

    @Override
    public void advance(float amount) {
        if (failed) return;
        try {
            tick();
        } catch (RuntimeException e) {
            // One bad frame must not take the campaign down with it. Logged once, then this mod
            // stands still for the rest of the session.
            failed = true;
            log.error("Astral Ascension Renewed stopped after an error", e);
            Global.getSector().getCampaignUI().addMessage(
                    "Astral Ascension Renewed hit an error and stopped until the game is loaded again. "
                            + "See starsector.log.", Misc.getNegativeHighlightColor());
        }
    }

    private void tick() {
        boolean capToolbox = toolbox && Settings.capToolboxScaling();
        for (MarketAPI market : new ArrayList<>(Global.getSector().getEconomy().getMarketsCopy())) {
            boolean omega = Ascension.isOmegaGoverned(market);
            if (omega && Ascension.needsOmega(market)) Ascension.markAscended(market);
            // Big worlds from other mods never grew under an Omega core here and are left alone.
            if (!Ascension.isAscended(market)) {
                if (omega) governWithOmega(market);
                continue;
            }
            if (Ascension.needsOmega(market) && !omega) {
                Ascension.greatCivilWar(market);
                continue;
            }
            if (omega) governWithOmega(market);
            Ascension.updateStage(market);
            if (capToolbox && Ascension.needsOmega(market)) capToolbox(market);
        }
        ledger.sweep();
    }

    private void governWithOmega(MarketAPI market) {
        Ascension.makeOmegaAware(market);
        liftSizeLimit(market);
        doubleProduction(market);
        if (market.getSize() >= Ascension.VANILLA_MAX) {
            // Re-added every frame to stay behind the buildings, which re-add themselves whenever the
            // colony is reapplied.
            market.removeTransientImmigrationModifier(strain);
            market.addTransientImmigrationModifier(strain);
            ledger.hold(market, "aar_growth_strain", () -> market.removeTransientImmigrationModifier(strain));
        }
    }

    /** An Omega core lets a colony grow to 15, unless something else holds it below the usual limit. */
    private void liftSizeLimit(MarketAPI market) {
        StatBonus limit = market.getStats().getDynamic().getMod(Stats.MAX_MARKET_SIZE);
        Float wanted = sizeLimitFlat(limit, Misc.MAX_COLONY_SIZE, Ascension.MAX_SIZE);
        if (wanted != null) ledger.flat(limit, SIZE_LIMIT, wanted, OMEGA_ADMIN, market);
    }

    /**
     * The flat bonus that brings the colony's size limit to the target, or null when something else
     * holds the colony below the usual limit (AoTD's Archeo-Site and Floating City do, on purpose).
     */
    static Float sizeLimitFlat(StatBonus limit, float base, int target) {
        StatMod own = limit.getFlatBonus(SIZE_LIMIT);
        float ownNow = own == null ? 0f : own.getValue();
        float mult = limit.getMult();
        if (mult <= 0f) return null;
        float beforeMult = base + base * limit.getPercentMod() / 100f + limit.getFlatBonus() - ownNow;
        if (beforeMult * mult < base - 0.01f) return null;
        float wanted = target / mult - beforeMult;
        if (own != null && Math.abs(wanted - ownNow) < 0.01f) wanted = ownNow;
        return wanted;
    }

    /**
     * Ethereal Enlightenment: every building makes twice as much and needs half as much, rounded
     * down. Production is a plain multiplier. Demand is set with a flat amount, recomputed every
     * frame, so it can round down instead of to the nearest unit.
     */
    private void doubleProduction(MarketAPI market) {
        for (Industry ind : market.getIndustries()) {
            for (MutableCommodityQuantity supply : ind.getAllSupply()) {
                ledger.mult(supply.getQuantity(), PRODUCTION, 2f, OMEGA_ADMIN);
            }
            for (CommodityOnMarketAPI commodity : market.getCommoditiesCopy()) {
                halveDemand(ind.getDemand(commodity.getId()).getQuantity());
            }
        }
    }

    private void halveDemand(MutableStat demand) {
        Float wanted = halvedDemandFlat(demand);
        if (wanted != null) ledger.flat(demand, DEMAND, wanted, OMEGA_ADMIN);
    }

    /** The flat amount that halves this demand, rounded down, or null when the entry is not in use. */
    static Float halvedDemandFlat(MutableStat demand) {
        float mult = demand.getMult();
        if (mult <= 0f) return null;
        StatMod own = demand.getFlatStatMod(DEMAND);
        float ownNow = own == null ? 0f : own.getValue();
        // What the building asks for without this modifier.
        float without = demand.getModifiedValue() - ownNow * mult;
        if (without <= 0.5f) return null;
        float target = (float) Math.floor(without / 2f + 0.001f);
        float wanted = (target - without) / mult;
        if (own != null && Math.abs(wanted - ownNow) < 0.001f) wanted = ownNow;
        return wanted;
    }

    /**
     * AoTD - Theory of Toolbox doubles a colony's food demand, domestic goods demand and farm output
     * with every size step. Past size 10 that doubling is held at its size-10 amount, so the colony
     * keeps growing linearly like everything else instead of outweighing the whole Sector.
     */
    private void capToolbox(MarketAPI market) {
        Industry population = market.getIndustry(Industries.POPULATION);
        if (population != null) {
            cap(population.getDemand(Commodities.FOOD).getQuantity(), TOOLBOX_SCALE, (float) Math.pow(2, 9));
            cap(population.getDemand(Commodities.DOMESTIC_GOODS).getQuantity(), TOOLBOX_SCALE, (float) Math.pow(2, 7));
        }
        for (Industry ind : market.getIndustries()) {
            MutableStat food = ind.getSupply(Commodities.FOOD).getQuantity();
            if (food.getFlatStatMod(TOOLBOX_FOOD) == null) continue;
            // Toolbox adds a flat amount so that output becomes base x 2^(size - 1).
            float scale = (float) Math.pow(2, Ascension.VANILLA_MAX - market.getSize());
            ledger.mult(food, TOOLBOX_CAP, scale, "Size scaling held at size " + Ascension.VANILLA_MAX);
            cap(ind.getDemand(Commodities.HEAVY_MACHINERY).getQuantity(), TOOLBOX_FOOD,
                    Math.round(0.3f * (float) Math.pow(2, 9)));
        }
    }

    /** Holds another mod's size multiplier at a ceiling by adding a correcting multiplier of our own. */
    private void cap(MutableStat stat, String theirs, float ceiling) {
        StatMod mod = stat.getMultStatMod(theirs);
        if (mod == null || mod.getValue() <= ceiling) return;
        ledger.mult(stat, TOOLBOX_CAP, ceiling / mod.getValue(), "Size scaling held at size " + Ascension.VANILLA_MAX);
    }

    /**
     * Past size 10, each size takes longer to reach than the one before, compounding: at the default
     * 1.5, size 11 takes 1.5 times as long, size 12 2.25 times. Only growth is slowed, never shrinking.
     */
    private static final class GrowthStrain implements MarketImmigrationModifier {
        @Override
        public void modifyIncoming(MarketAPI market, PopulationComposition incoming) {
            int steps = market.getSize() - (Ascension.VANILLA_MAX - 1);
            if (steps <= 0 || incoming.getWeight().getModifiedValue() <= 0f) return;
            float factor = (float) Math.pow(Settings.growthStrain(), steps);
            incoming.getWeight().modifyMult("aar_growth_strain", 1f / factor, "Growth past size " + Ascension.VANILLA_MAX);
        }
    }
}
