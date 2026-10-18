package astralascensionrenewed.rulecmd;

import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc.Token;

import astralascensionrenewed.campaign.Ascension;

/**
 * rules.csv condition: the colony in this dialog is governed by an Omega core and is past size 10,
 * so taking the core away means the Great Civil War rather than the base game's rogue AI.
 */
public class AarOmegaMegacity extends BaseCommandPlugin {

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params,
                           Map<String, MemoryAPI> memoryMap) {
        MarketAPI market = market(dialog);
        return market != null && Ascension.isOmegaGoverned(market) && Ascension.needsOmega(market);
    }

    static MarketAPI market(InteractionDialogAPI dialog) {
        if (dialog == null || dialog.getInteractionTarget() == null) return null;
        return dialog.getInteractionTarget().getMarket();
    }
}
