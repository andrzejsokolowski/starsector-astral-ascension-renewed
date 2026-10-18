package astralascensionrenewed.skills;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.MarketSkillEffect;
import com.fs.starfarer.api.impl.campaign.ids.Stats;

/** Omega core administrator skill: hazard, ship quality, production and industry slots. */
public class EtherealEnlightenment {

    public static final String ID = "aar_ethereal_enlightenment";
    public static final String NAME = "Ethereal Enlightenment";

    public static class Level1 extends AdminEffect {
        public void apply(MarketAPI market, String id, float level) {
            market.getHazard().modifyFlat(id, -0.25f, NAME);
        }

        public void unapply(MarketAPI market, String id) {
            market.getHazard().unmodifyFlat(id);
        }

        public String getEffectDescription(float level) {
            return "-25% hazard rating";
        }
    }

    public static class Level2 extends AdminEffect {
        public void apply(MarketAPI market, String id, float level) {
            market.getStats().getDynamic().getMod(Stats.PRODUCTION_QUALITY_MOD).modifyFlat(id, 0.25f, NAME);
        }

        public void unapply(MarketAPI market, String id) {
            market.getStats().getDynamic().getMod(Stats.PRODUCTION_QUALITY_MOD).unmodifyFlat(id);
        }

        public String getEffectDescription(float level) {
            return "+25% ship quality";
        }
    }

    /** Carried out every frame by AscensionScript: a skill can only add flat amounts to the colony. */
    public static class Level3 extends AdminEffect {
        public void apply(MarketAPI market, String id, float level) {
        }

        public void unapply(MarketAPI market, String id) {
        }

        public String getEffectDescription(float level) {
            return "Every building's production x2 and demand halved, rounded down";
        }
    }

    public static class Level4 extends AdminEffect {
        public void apply(MarketAPI market, String id, float level) {
            market.getStats().getDynamic().getMod(Stats.MAX_INDUSTRIES).modifyFlat(id, 1f, NAME);
        }

        public void unapply(MarketAPI market, String id) {
            market.getStats().getDynamic().getMod(Stats.MAX_INDUSTRIES).unmodifyFlat(id);
        }

        public String getEffectDescription(float level) {
            return "+1 maximum industries";
        }
    }

    /** Shared plumbing for administrator skill effects. */
    abstract static class AdminEffect implements MarketSkillEffect {
        public String getEffectPerLevelDescription() {
            return null;
        }

        public ScopeDescription getScopeDescription() {
            return ScopeDescription.GOVERNED_OUTPOST;
        }
    }
}
