package astralascensionrenewed.console;

import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommandUtils;
import org.lazywizard.console.Console;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.ImmigrationPlugin;
import com.fs.starfarer.api.util.Misc;

/** aar_growth &lt;0-100&gt; [colony]: how far the colony has grown towards its next size. */
public class AarGrowth implements BaseCommand {

    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (!context.isInCampaign()) return CommandResult.WRONG_CONTEXT;
        String number = Target.first(args);
        if (!CommandUtils.isFloat(number) && !CommandUtils.isInteger(number)) return CommandResult.BAD_SYNTAX;
        float percent = Math.max(0f, Math.min(100f, Float.parseFloat(number)));
        MarketAPI market = Target.market(Target.rest(args), context);
        if (market == null) return CommandResult.ERROR;

        ImmigrationPlugin plugin = Misc.getImmigrationPlugin(market);
        float min = plugin.getWeightForMarketSize(market.getSize());
        float max = plugin.getWeightForMarketSize(market.getSize() + 1);
        market.getPopulation().setWeight(min + (max - min) * percent / 100f);
        market.getPopulation().normalize();
        Console.showMessage(market.getName() + ": growth towards size " + (market.getSize() + 1) + " set to "
                + Math.round(percent) + " percent. Size limit: " + Misc.getMaxMarketSize(market) + ".");
        return CommandResult.SUCCESS;
    }
}
