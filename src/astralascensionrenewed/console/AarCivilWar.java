package astralascensionrenewed.console;

import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;

import com.fs.starfarer.api.campaign.econ.MarketAPI;

import astralascensionrenewed.campaign.Ascension;

/** aar_civilwar [colony]: runs the Great Civil War at once. The administrator is left in place. */
public class AarCivilWar implements BaseCommand {

    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (!context.isInCampaign()) return CommandResult.WRONG_CONTEXT;
        MarketAPI market = Target.market(args, context);
        if (market == null) return CommandResult.ERROR;
        Ascension.greatCivilWar(market);
        Console.showMessage("Great Civil War on " + market.getName() + ": now size " + market.getSize() + ".");
        return CommandResult.SUCCESS;
    }
}
