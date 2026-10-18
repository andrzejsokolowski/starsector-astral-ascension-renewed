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
 * rules.csv script: the player confirmed removing the Omega core. The war is fought before the core
 * leaves, so the colony is already down to size 8 when the administrator picker takes it out.
 */
public class AarGreatCivilWar extends BaseCommandPlugin {

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params,
                           Map<String, MemoryAPI> memoryMap) {
        MarketAPI market = AarOmegaMegacity.market(dialog);
        if (market == null) return false;
        Ascension.greatCivilWar(market);
        return true;
    }
}
