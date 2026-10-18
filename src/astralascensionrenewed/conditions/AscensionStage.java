package astralascensionrenewed.conditions;

import java.awt.Color;

import com.fs.starfarer.api.impl.campaign.econ.BaseMarketConditionPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

/** A colony past size 10. The bonuses grow with the stage; the stage follows the size. */
public abstract class AscensionStage extends BaseMarketConditionPlugin {

    protected abstract int industries();

    protected abstract float accessibility();

    protected abstract float incomeMult();

    protected abstract float groundDefenseMult();

    protected abstract float fleetSize();

    protected abstract float hazard();

    @Override
    public void apply(String id) {
        super.apply(id);
        String name = condition.getName();
        market.getStats().getDynamic().getMod(Stats.MAX_INDUSTRIES).modifyFlat(id, industries(), name);
        market.getAccessibilityMod().modifyFlat(id, accessibility(), name);
        market.getIncomeMult().modifyMult(id, incomeMult(), name);
        market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD).modifyMult(id, groundDefenseMult(), name);
        market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT).modifyFlat(id, fleetSize(), name);
        market.getHazard().modifyFlat(id, hazard(), name);
    }

    @Override
    public void unapply(String id) {
        super.unapply(id);
        market.getStats().getDynamic().getMod(Stats.MAX_INDUSTRIES).unmodifyFlat(id);
        market.getAccessibilityMod().unmodifyFlat(id);
        market.getIncomeMult().unmodifyMult(id);
        market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD).unmodifyMult(id);
        market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT).unmodifyFlat(id);
        market.getHazard().unmodifyFlat(id);
    }

    @Override
    protected void createTooltipAfterDescription(TooltipMakerAPI tooltip, boolean expanded) {
        super.createTooltipAfterDescription(tooltip, expanded);
        Color h = Misc.getHighlightColor();
        float pad = 10f;
        tooltip.addPara("%s maximum industries", pad, h, "+" + industries());
        tooltip.addPara("%s accessibility", 3f, h, "+" + percent(accessibility()) + "%");
        tooltip.addPara("Colony income %s", 3f, h, "x" + times(incomeMult()));
        tooltip.addPara("Ground defenses %s", 3f, h, "x" + times(groundDefenseMult()));
        tooltip.addPara("%s fleet size", 3f, h, "+" + percent(fleetSize()) + "%");
        tooltip.addPara("%s hazard rating", 3f, h, percent(hazard()) + "%");
        tooltip.addPara("Needs an Omega core as administrator. Without one, the colony falls into a Great Civil "
                + "War and is left at size %s.", pad, Misc.getNegativeHighlightColor(), "8");
    }

    private static String percent(float fraction) {
        return "" + Math.round(fraction * 100f);
    }

    private static String times(float mult) {
        return Misc.getRoundedValueMaxOneAfterDecimal(mult);
    }
}
