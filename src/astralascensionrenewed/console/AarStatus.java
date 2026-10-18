package astralascensionrenewed.console;

import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.util.Misc;

import astralascensionrenewed.Settings;
import astralascensionrenewed.campaign.Ascension;

/** aar_status [colony]: everything this mod decides about a colony, in one place. */
public class AarStatus implements BaseCommand {

    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (!context.isInCampaign()) return CommandResult.WRONG_CONTEXT;
        MarketAPI market = Target.market(args, context);
        if (market == null) return CommandResult.ERROR;

        StringBuilder out = new StringBuilder();
        out.append(market.getName()).append(" (").append(market.getFactionId()).append(")\n");
        out.append("  Size ").append(market.getSize()).append(", limit ").append(Misc.getMaxMarketSize(market))
                .append(" (without an Omega core: ").append(Misc.MAX_COLONY_SIZE).append(")\n");
        out.append("  Growth towards next size: ").append(Math.round(Misc.getMarketSizeProgress(market) * 100f))
                .append(" percent, ").append(Math.round(market.getIncoming().getWeightValue()))
                .append(" points a month\n");
        String admin = market.getAdmin() == null ? "none" : market.getAdmin().getNameString();
        out.append("  Administrator: ").append(admin)
                .append(Ascension.isOmegaGoverned(market) ? " (Omega core)" : "").append('\n');
        String stage = market.hasCondition(Ascension.HYPER_ECUMENOPOLIS_CONDITION) ? "Hyper-Ecumenopolis"
                : market.hasCondition(Ascension.ECUMENOPOLIS_CONDITION) ? "Ecumenopolis"
                : market.hasCondition(Ascension.ARCOLOGY_CONDITION) ? "Arcology World" : "none";
        out.append("  Stage: ").append(stage);
        if (market.getPlanetEntity() != null) out.append(", planet type ").append(market.getPlanetEntity().getTypeId());
        out.append('\n');
        if (Ascension.needsOmega(market) && Settings.growthStrain() > 1f) {
            out.append("  Growth slowed by ").append(Misc.getRoundedValueMaxOneAfterDecimal(
                    (float) Math.pow(Settings.growthStrain(), market.getSize() - (Ascension.VANILLA_MAX - 1))))
                    .append("x past size ").append(Ascension.VANILLA_MAX).append('\n');
        }
        boolean toolbox = Global.getSettings().getModManager().isModEnabled("aotd_theory_of_toolbox");
        out.append("  AoTD Theory of Toolbox: ").append(toolbox ? "installed" : "not installed");
        if (toolbox) out.append(", doubling held at size 10: ").append(Settings.capToolboxScaling() ? "yes" : "no");
        out.append('\n');
        Industry population = market.getIndustry(Industries.POPULATION);
        if (population != null) {
            out.append("  Population demand: food ")
                    .append(population.getDemand(Commodities.FOOD).getQuantity().getModifiedInt())
                    .append(", domestic goods ")
                    .append(population.getDemand(Commodities.DOMESTIC_GOODS).getQuantity().getModifiedInt())
                    .append('\n');
        }
        Console.showMessage(out.toString());
        return CommandResult.SUCCESS;
    }
}
