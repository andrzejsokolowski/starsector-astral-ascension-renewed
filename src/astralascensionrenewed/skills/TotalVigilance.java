package astralascensionrenewed.skills;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;

import astralascensionrenewed.skills.ZeroWaste.AdminEffect;

/** Omega core administrator skill: stability, ground defenses and fleet size. */
public class TotalVigilance {

    public static final String ID = "aar_acumenious_oracle"; // saved in games, never change
    public static final String NAME = "Total Vigilance";

    public static class Level1 extends AdminEffect {
        public void apply(MarketAPI market, String id, float level) {
            market.getStability().modifyFlat(id, 3f, NAME);
        }

        public void unapply(MarketAPI market, String id) {
            market.getStability().unmodifyFlat(id);
        }

        public String getEffectDescription(float level) {
            return "+3 stability";
        }
    }

    public static class Level2 extends AdminEffect {
        public void apply(MarketAPI market, String id, float level) {
            market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD).modifyMult(id, 3f, NAME);
        }

        public void unapply(MarketAPI market, String id) {
            market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD).unmodifyMult(id);
        }

        public String getEffectDescription(float level) {
            return "Ground defenses x3";
        }
    }

    public static class Level3 extends AdminEffect {
        public void apply(MarketAPI market, String id, float level) {
            market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT).modifyMult(id, 2f, NAME);
        }

        public void unapply(MarketAPI market, String id) {
            market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT).unmodifyMult(id);
        }

        public String getEffectDescription(float level) {
            return "Fleet size x2";
        }
    }
}
