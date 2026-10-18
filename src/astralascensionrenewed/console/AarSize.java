package astralascensionrenewed.console;

import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommandUtils;
import org.lazywizard.console.Console;

import com.fs.starfarer.api.campaign.econ.MarketAPI;

import astralascensionrenewed.campaign.Ascension;

/** aar_size &lt;3-15&gt; [colony]: sets a colony's size, with its stage and planet look. */
public class AarSize implements BaseCommand {

    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (!context.isInCampaign()) return CommandResult.WRONG_CONTEXT;
        String number = Target.first(args);
        if (!CommandUtils.isInteger(number)) return CommandResult.BAD_SYNTAX;
        int size = Integer.parseInt(number);
        if (size < 3 || size > Ascension.MAX_SIZE) {
            Console.showMessage("Size must be between 3 and " + Ascension.MAX_SIZE + ".");
            return CommandResult.BAD_SYNTAX;
        }
        MarketAPI market = Target.market(Target.rest(args), context);
        if (market == null) return CommandResult.ERROR;

        int was = market.getSize();
        if (size > Ascension.VANILLA_MAX && !Ascension.isOmegaGoverned(market)) {
            // Without one the colony would fall into the Great Civil War on the next frame.
            Ascension.installOmegaAdmin(market);
            Console.showMessage("Installed an Omega core administrator, which sizes past "
                    + Ascension.VANILLA_MAX + " need.");
        }
        Ascension.setSize(market, size);
        Console.showMessage(market.getName() + ": size " + was + " -> " + market.getSize() + ".");
        return CommandResult.SUCCESS;
    }
}
