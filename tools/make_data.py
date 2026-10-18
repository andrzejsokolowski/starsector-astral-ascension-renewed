"""Writes the CSV data files whose quoting is easy to get wrong by hand.

    python tools/make_data.py
"""
import csv
import io
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PKG = "astralascensionrenewed"


def write(path, header, rows):
    out = io.StringIO()
    w = csv.writer(out, lineterminator="\n")
    w.writerow(header)
    w.writerows(rows)
    full = os.path.join(ROOT, path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    with open(full, "w", encoding="utf-8", newline="") as f:
        f.write(out.getvalue())


POPULATION = {
    11: "$MarketName is home to hundreds of billions of people. Its cities ran into each other long ago, "
        "and the arcologies that hold them now reach the upper atmosphere.",
    12: "A trillion souls live on $marketName, more than Old Earth ever held.",
    13: "$MarketName houses tens of trillions. Every surface is filled with concrete, and the city goes as far "
        "down as it goes up.",
    14: "Hundreds of trillions live on $marketName. People that are born here will never see nature.",
    15: "$MarketName holds a quadrillion lives. The Omega core that runs it is the only mind in the Sector "
        "that can keep everyone from endless warfare.",
}

write("data/campaign/market_conditions.csv",
      ["name", "id", "tags", "planetary", "decivRemove", "script", "desc", "icon", "order"],
      [["Population", "population_%d" % n, "", "", "TRUE", PKG + ".conditions.AarPopulation",
        desc, "graphics/icons/markets/aar_population%d.png" % n, "20"] for n, desc in POPULATION.items()]
      + [
          ["Arcology World", "aar_arcology_world", "", "", "TRUE", PKG + ".conditions.ArcologyWorld",
           "Arcologies kilometres tall have swallowed $marketName's old cities whole. What land is left between "
           "them is kept as parkland, because nothing else is needed there.",
           "graphics/icons/markets/aar_arcology_world.png", "21"],
          ["Ecumenopolis", "aar_ecumenopolis", "", "", "TRUE", PKG + ".conditions.Ecumenopolis",
           "$MarketName is a single city from pole to pole. Its oceans run through pipes, its weather is "
           "scheduled, and its sky is lit all night by its own streets.",
           "graphics/icons/markets/aar_ecumenopolis.png", "21"],
          ["Crowned World", "aar_crowned_world", "", "", "TRUE", PKG + ".conditions.CrownedWorld",
           "An orbital ring circles $marketName, carried on its own spin and tied to the surface by hundreds of "
           "space elevators. Its docks and shipyards run the whole way round, and from below it cuts the sky "
           "in two.",
           "graphics/icons/markets/aar_crowned_world.png", "21"],
          # Same row as the base game, with a script that knows an Omega core cannot simply be unplugged
          # from a colony that only it can run.
          ["AI Core Admin (hidden)", "ai_core_admin", "", "", "TRUE", PKG + ".campaign.OmegaAwareAICoreAdmin",
           "Hidden condition to track when the core becomes un-removable. Does not need an icon.",
           "graphics/icons/markets/rogue_ai.png", "1000"],
      ])

write("data/strings/descriptions.csv",
      ["id", "type", "text1", "text2", "text3", "text4", "text5", "notes"],
      [
          ["aar_arcology", "PLANET",
           "A world grown into a city. Arcologies kilometres tall cover nearly all of its land and spill out "
           "over the coasts, and at night the planet's whole dark side is lit.", "", "", "", "", ""],
          ["aar_ecumenopolis", "PLANET",
           "A single city covering the whole planet, built in layers from the bedrock to the edge of orbit. "
           "Its night side never goes dark.", "", "", "", "", ""],
      ])

SKILL_HEADER = ["id", "name", "order", "tier", "reqPoints", "reqPointsPerExtraSkill", "description", "author",
                "combat officer", "admiral", "admin", "tags", "icon", ""]
write("data/characters/skills/skill_data.csv", SKILL_HEADER, [
    ["aar_ethereal_enlightenment", "Ethereal Enlightenment", "36000", "0", "", "",
     "It does not manage the colony. It simply sees no reason for anything to be wasted.",
     "Report to the Hegemony Directorate on AI-run worlds", "", "", "TRUE", "npc_only, ai_core_only",
     "graphics/icons/skills/aar_ethereal_enlightenment.png", ""],
    ["aar_acumenious_oracle", "Acumenious Oracle", "36001", "0", "", "",
     "Our raiders were met at every landing site, by defenders who had arrived before we chose where to land.",
     "Debrief of a failed pirate raid", "", "", "TRUE", "npc_only, ai_core_only",
     "graphics/icons/skills/aar_acumenious_oracle.png", ""],
    ["aar_delphic_optimiser", "Delphic Optimiser", "36002", "0", "", "",
     "Every freighter is loaded before it lands. Every contract is signed before it is offered.",
     "A trader on the docks of an Omega-run world", "", "", "TRUE", "npc_only, ai_core_only",
     "graphics/icons/skills/aar_delphic_optimiser.png", ""],
])

RULES_HEADER = ["id", "trigger", "conditions", "script", "text", "options", "notes"]
write("data/campaign/rules.csv", RULES_HEADER, [
    ["aar_civilwar_init", "RemoveAICoreAdmin", "AarOmegaMegacity", "ShowPic ai_core_uninstall",
     "You give the order to disconnect the Omega core governing $marketName.\n\n"
     "The reply arrives before the order has finished sending. It is not a refusal. It is a forecast: how many "
     "people $marketName holds, how many decisions a second it takes to keep them fed, housed and alive, and how "
     "many minds in the Sector can make them. The last number is one.\n\n"
     "A colony this size is too big to be governed by a lesser mind. Without the Omega core, $marketName will "
     "fall to anarchy.",
     "aar_civilwar_remove:Order the Omega core removed anyway\n"
     "aar_civilwar_keep:Leave the Omega core in control",
     "Shown instead of the base game's rogue AI dialog when the colony is past size 10."],
    ["aar_civilwar_remove", "DialogOptionSelected", "$option == aar_civilwar_remove", "",
     "The forecast is specific. The districts turn on each other within days. The Great Civil War that follows "
     "will leave $marketName at size 8, with an armed, ungoverned population living in the ruins.\n\n"
     "The Omega core itself will be returned to you.",
     "aar_civilwar_confirm:Confirm the order\n"
     "aar_civilwar_keep:Leave the Omega core in control", ""],
    ["aar_civilwar_confirm", "DialogOptionSelected", "$option == aar_civilwar_confirm",
     "AarGreatCivilWar\nDismissDialog override", "", "",
     "override: the base game returns the core and adds no rogue AI."],
    ["aar_civilwar_keep", "DialogOptionSelected", "$option == aar_civilwar_keep", "DismissDialog ignore", "", "", ""],
])

write("data/console/commands.csv", ["command", "class", "tags", "syntax", "help"], [
    ["aar_size", PKG + ".console.AarSize", "astral,cheat,market", "aar_size <3-15> [colony name]",
     "Sets a colony's size directly, up to 15 (Console Commands' own setmarketsize stops at 10). Updates the "
     "population condition, the Arcology World, Ecumenopolis or Crowned World stage and the planet's look. Past 10 the colony "
     "needs an Omega core administrator first. Uses the colony you are docked at if no name is given."],
    ["aar_growth", PKG + ".console.AarGrowth", "astral,cheat,market", "aar_growth <0-100> [colony name]",
     "Sets how far a colony has grown towards its next size, in percent. 99 makes it grow within days, "
     "if it is allowed to grow."],
    ["aar_status", PKG + ".console.AarStatus", "astral,market", "aar_status [colony name]",
     "Shows a colony's size, size limit, growth, administrator, stage and AoTD scaling."],
])
print("data files written")
