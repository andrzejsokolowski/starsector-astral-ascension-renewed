package astralascensionrenewed.conditions;

import com.fs.starfarer.api.impl.campaign.econ.Population;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

/** The population condition for sizes 11 to 15: the base game's, plus the unrest of too many people. */
public class AarPopulation extends Population {

    private static final String PREFIX = "population_";

    /** Stability at each size past 10. */
    static int stability(int size) {
        if (size >= 14) return -3;
        if (size >= 12) return -2;
        if (size >= 11) return -1;
        return 0;
    }

    private int size() {
        String id = condition.getId();
        try {
            return Integer.parseInt(id.substring(PREFIX.length()));
        } catch (RuntimeException e) {
            return market.getSize();
        }
    }

    @Override
    public void apply(String id) {
        super.apply(id);
        int stability = stability(size());
        if (stability != 0) market.getStability().modifyFlat(id, stability, "Population size");
    }

    @Override
    public void unapply(String id) {
        super.unapply(id);
        market.getStability().unmodifyFlat(id);
    }

    @Override
    protected void createTooltipAfterDescription(TooltipMakerAPI tooltip, boolean expanded) {
        super.createTooltipAfterDescription(tooltip, expanded);
        int stability = stability(size());
        if (stability != 0) {
            tooltip.addPara("%s stability", 10f, Misc.getNegativeHighlightColor(), "" + stability);
        }
    }
}
