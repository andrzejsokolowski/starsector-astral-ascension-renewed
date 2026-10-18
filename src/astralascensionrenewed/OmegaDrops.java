package astralascensionrenewed;

import java.util.Set;

import org.apache.log4j.Logger;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Tags;

/**
 * Lets a destroyed Omega ship drop the Omega core that commanded it, so Omega administrators can be
 * had at all.
 *
 * Vanilla already has the whole drop path: FleetEncounterContext rolls
 * drop_prob_officer_[core id], scaled by hull size, for every destroyed AI-captained ship whose core
 * commodity lacks the no_drop tag. The Omega core is the only core with that tag and without a drop
 * chance, so removing the tag and setting the chance is all it takes. The tag lives on the shared
 * commodity spec, which the game builds once per session, so it is set again on every game load and
 * settings change.
 */
final class OmegaDrops {

    /** Omega Core Everywhere does the same thing with its own settings; it is left in charge. */
    static final String OMEGA_CORE_EVERYWHERE_ID = "omega_core_everywhere";
    static final String DROP_CHANCE_KEY = "drop_prob_officer_" + Commodities.OMEGA_CORE;

    private static final Logger log = Global.getLogger(OmegaDrops.class);

    private OmegaDrops() {}

    static void apply() {
        if (Global.getSettings().getModManager().isModEnabled(OMEGA_CORE_EVERYWHERE_ID)) {
            log.info("Omega Core Everywhere is enabled; leaving Omega core drops to it");
            return;
        }

        CommoditySpecAPI spec = Global.getSettings().getCommoditySpec(Commodities.OMEGA_CORE);
        if (spec == null) return;

        boolean enabled = Settings.dropsEnabled();
        float chance = Settings.dropChance();
        Set<String> tags = spec.getTags();
        if (enabled) {
            tags.remove(Tags.NO_DROP);
        } else {
            tags.add(Tags.NO_DROP);
        }
        Global.getSettings().setFloat(DROP_CHANCE_KEY, chance);
        log.info("Omega core drops " + (enabled ? "on, chance " + chance : "off"));
    }
}
