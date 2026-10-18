package astralascensionrenewed.skills;

import com.fs.starfarer.api.campaign.econ.MarketAPI;

import astralascensionrenewed.skills.EtherealEnlightenment.AdminEffect;

/** Omega core administrator skill: income, upkeep and accessibility. */
public class DelphicOptimiser {

    public static final String ID = "aar_delphic_optimiser";
    public static final String NAME = "Delphic Optimiser";

    public static class Level1 extends AdminEffect {
        public void apply(MarketAPI market, String id, float level) {
            market.getIncomeMult().modifyMult(id, 1.25f, NAME);
        }

        public void unapply(MarketAPI market, String id) {
            market.getIncomeMult().unmodifyMult(id);
        }

        public String getEffectDescription(float level) {
            return "Colony income x1.25";
        }
    }

    public static class Level2 extends AdminEffect {
        public void apply(MarketAPI market, String id, float level) {
            market.getUpkeepMult().modifyMult(id, 0.75f, NAME);
        }

        public void unapply(MarketAPI market, String id) {
            market.getUpkeepMult().unmodifyMult(id);
        }

        public String getEffectDescription(float level) {
            return "Colony upkeep x0.75";
        }
    }

    public static class Level3 extends AdminEffect {
        public void apply(MarketAPI market, String id, float level) {
            market.getAccessibilityMod().modifyFlat(id, 0.5f, NAME);
        }

        public void unapply(MarketAPI market, String id) {
            market.getAccessibilityMod().unmodifyFlat(id);
        }

        public String getEffectDescription(float level) {
            return "+50% accessibility";
        }
    }
}
