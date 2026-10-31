package astralascensionrenewed.campaign;

import org.apache.log4j.Logger;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.util.IntervalUtil;

import data.scripts.campaign.econ.conditions.DomainMajority;
import data.scripts.events.AwakeningEventPlugin;

/**
 * AoTD - Dreams of the Past adds its Domain Majority condition only when a cryosleeper or ark
 * awakening grows the colony, and reads it back without a check when the awakening ends. The
 * awakening grows a colony to 7 (cryosleeper) or 8 (ark) at most, so a colony that is already that
 * big - possible only because this mod lifts the size limit - never gets the condition and the
 * game crashes when the awakening finishes. This adds it the way Dreams of the Past would have,
 * stability penalty included, to every colony with an awakening running that lacks it.
 *
 * Only {@link #install()} may be called from outside, and only with Dreams of the Past enabled:
 * this class names its classes and must not load without them.
 */
public final class AwakeningGuard implements EveryFrameScript {

    public static final String DREAMS_OF_THE_PAST_ID = "Cryo_but_better";

    /** Dreams of the Past's condition id. */
    private static final String DOMAIN_MAJORITY = "aotd_domain_majority";

    private static final Logger log = Global.getLogger(AwakeningGuard.class);

    private final IntervalUtil interval = new IntervalUtil(1f, 1f);
    private boolean failed;

    private AwakeningGuard() {}

    /** Called on every game load, before the campaign advances: a save made just before an awakening ends loads safely. */
    public static void install() {
        SectorAPI sector = Global.getSector();
        sector.removeTransientScriptsOfClass(AwakeningGuard.class);
        AwakeningGuard guard = new AwakeningGuard();
        guard.check();
        sector.addTransientScript(guard);
    }

    @Override
    public boolean isDone() {
        return failed;
    }

    @Override
    public boolean runWhilePaused() {
        return false;
    }

    @Override
    public void advance(float amount) {
        interval.advance(Global.getSector().getClock().convertToDays(amount));
        if (interval.intervalElapsed()) check();
    }

    private void check() {
        if (failed) return;
        try {
            for (IntelInfoPlugin intel : Global.getSector().getIntelManager().getIntel(AwakeningEventPlugin.class)) {
                AwakeningEventPlugin awakening = (AwakeningEventPlugin) intel;
                MarketAPI market = awakening.tiedMarket;
                if (market == null || awakening.isEnded() || market.hasCondition(DOMAIN_MAJORITY)) continue;
                market.addCondition(DOMAIN_MAJORITY);
                MarketConditionAPI condition = market.getFirstCondition(DOMAIN_MAJORITY);
                if (condition != null && condition.getPlugin() instanceof DomainMajority) {
                    ((DomainMajority) condition.getPlugin()).setShouldReduceStability(true);
                }
                log.info("[AAR] Added Domain Majority to " + market.getName() + " (size " + market.getSize()
                        + "), where the awakening could not grow the colony to add it");
            }
        } catch (RuntimeException | LinkageError e) {
            // A Dreams of the Past update that changed these classes must not take the campaign down.
            failed = true;
            log.error("Astral Ascension Renewed stopped its Dreams of the Past awakening check", e);
        }
    }
}
