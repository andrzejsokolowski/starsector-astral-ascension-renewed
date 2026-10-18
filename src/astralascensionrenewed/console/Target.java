package astralascensionrenewed.console;

import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.CommandUtils;
import org.lazywizard.console.Console;

import com.fs.starfarer.api.campaign.econ.MarketAPI;

/** Which colony a test command is about: the one named, or the one you are docked at. */
final class Target {

    private Target() {}

    static MarketAPI market(String name, CommandContext context) {
        MarketAPI market = null;
        if (name != null && !name.trim().isEmpty()) {
            market = CommandUtils.findBestMarketMatch(name.trim());
            if (market == null) Console.showMessage("No colony found matching \"" + name.trim() + "\".");
        } else if (context.isInMarket()) {
            market = context.getMarket();
        } else {
            Console.showMessage("Name a colony, or dock at one first.");
        }
        return market;
    }

    /** Everything after the first word. */
    static String rest(String args) {
        String trimmed = args == null ? "" : args.trim();
        int space = trimmed.indexOf(' ');
        return space < 0 ? "" : trimmed.substring(space + 1);
    }

    static String first(String args) {
        String trimmed = args == null ? "" : args.trim();
        int space = trimmed.indexOf(' ');
        return space < 0 ? trimmed : trimmed.substring(0, space);
    }
}
