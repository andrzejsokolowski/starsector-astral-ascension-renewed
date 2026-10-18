package astralascensionrenewed.console;

import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;

import com.fs.starfarer.api.campaign.econ.MarketAPI;

import astralascensionrenewed.campaign.Ascension;

/** aar_omegaadmin [colony]: an Omega core administrator, without spending an Omega core. */
public class AarOmegaAdmin implements BaseCommand {

    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (!context.isInCampaign()) return CommandResult.WRONG_CONTEXT;
        MarketAPI market = Target.market(args, context);
        if (market == null) return CommandResult.ERROR;
        if (Ascension.isOmegaGoverned(market)) {
            Console.showMessage(market.getName() + " is already governed by an Omega core.");
            return CommandResult.SUCCESS;
        }
        Ascension.installOmegaAdmin(market);
        Console.showMessage(market.getName() + " is now governed by an Omega core.");
        return CommandResult.SUCCESS;
    }
}
