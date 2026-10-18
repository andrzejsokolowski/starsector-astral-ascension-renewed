# Astral Ascension Renewed

These instructions apply to this repository, including when it is opened without the parent mod workspace.
The workspace `AGENTS.md` one folder up still governs releases, packaging, Git identity and communication.
For requested gameplay changes and bug fixes, complete the release workflow unless the user requests otherwise.
Questions, investigations, and instruction-only changes do not require a new version or tag.
Respect tool approval decisions. These instructions do not change sandbox permissions.

## Notion

Tasks, history and test lists: the **Astral Ascension Renewed board** in Andrzej's Notion, under
**Starsector mods** (`collection://9d16fa04-578d-4877-83bb-77ff7a861214`). Working rules and game
API notes: **Claude Information** (`collection://7bf9b1d6-b9e5-4b62-87f1-757bbaa51919`), starting
with "Start here: working on Starsector mods through Notion". Read the board before working, give
every task a ticket (ticket ids start with AAR) and keep it current. The Notion tools are deferred:
load them with ToolSearch first.

## Releases

0.1.0 is the first published GitHub Release (2026-10-18). Follow the release workflow below for
every requested change.

## Origin

A continuation of Astral Ascension (`Planetace_AstralAscension`) by Planetace, who gave Andrzej their
OK to continue it. No code or art from the original is used; names of the three Omega skills are
kept. The mod refuses to start next to the original.

## Layout

- `src/astralascensionrenewed/AstralAscensionRenewedPlugin` - mod plugin; refuses to run next to the
  original, sets `Misc.MAX_COLONY_SIZE` from LunaLib, installs the campaign script.
- `src/astralascensionrenewed/OmegaDrops` - Omega core drops; stands down when Omega Core
  Everywhere (`omega_core_everywhere`) is enabled.
- `campaign/AarCampaignPlugin` + `OmegaAdminPlugin` - make the Omega core an administrator.
- `campaign/AscensionScript` - transient every-frame script: size limit 15 for Omega-governed
  colonies, production x2 / demand halved, growth slowdown past 10, stages, AoTD Theory of Toolbox
  cap, Great Civil War for colonies past 10 that lost their Omega core.
- `campaign/Ascension` - size changes, stage conditions, planet look (with snapshot and restore in
  market memory under `$aar_look_`), the Great Civil War.
- `campaign/OmegaAwareAICoreAdmin` - replaces the base game's hidden `ai_core_admin` condition script
  (row in `data/campaign/market_conditions.csv`) so the admin picker opens the RemoveAICoreAdmin
  dialog for Omega cores past size 10. The dialog is in `data/campaign/rules.csv`.
- `conditions/` - Arcology World (11-12), Ecumenopolis (13-14), Crowned World (15), and
  `AarPopulation` (population 11-15, stability -1 to -3). `skills/` - the three admin skills.
- `campaign/OrbitalRing` - the Crowned World's ring: a custom entity (`data/config/custom_entities.json`)
  that follows the planet, draws its far half in TERRAIN_2 and near half in TERRAIN_3, and expires
  itself when the colony stops being a Crowned World.
- `rulecmd/` - rule commands for the dialog (package listed in `data/config/settings.json`).
- `console/` - Console Commands test commands (`data/console/commands.csv`). Only Console Commands
  loads these classes.
- `tools/make_textures.py`, `tools/make_data.py` - regenerate art and CSV data. Not shipped.

## Rules

- Only colonies that grew past 10 under an Omega core (`$aar_ascended`) get stages, looks and the
  civil war. Big worlds from other mods are left alone.
- Do not put a bare `%` in `data/config/LunaSettings.csv` (LunaLib formats it); the build rejects it.
- Decompiled sources of other mods stay out of the repository.

## Required release workflow

1. Preserve unrelated changes from the user and other agents.
2. Implement the requested change and update the version in mod_info.json and astral_ascension_renewed.version.
3. Run ./gradlew releaseZip to build, check, and package AstralAscensionRenewed.zip.
4. Make sure that the ZIP contains the updated metadata, current JAR, data and graphics.
5. Commit the completed change on main with the existing personal Git identity.
6. Tag the commit v<version> and push the commit and tag to the existing personal GitHub origin.
7. Create a draft GitHub Release for that exact tag.
8. Attach the tested AstralAscensionRenewed.zip to the draft.
9. Publish the release after the upload succeeds.
10. Mark a new stable release as latest unless the user requests otherwise.
11. Make sure that the public version file and the downloaded ZIP report the tagged version.
12. Compare the downloaded ZIP's SHA-256 hash with the tested local ZIP.
13. Give the user the release link and local ZIP location.

Every new version tag created or pushed by an agent requires a matching published release with the ZIP attached.
Do not stop after pushing a tag. A tag is not a release download.

## Update metadata

Keep mod_info.json, astral_ascension_renewed.version, and data/config/version/version_files.csv consistent.
Forum topic: https://fractalsoftworks.com/forum/index.php?topic=36167.0 (modThreadId "36167" in
mod_info.json, 36167 in the .version file).
Update URL (updateCheckURL and masterVersionFile):

https://raw.githubusercontent.com/andrzejsokolowski/starsector-astral-ascension-renewed/main/astral_ascension_renewed.version

directDownloadURL, exact asset name AstralAscensionRenewed.zip:

https://github.com/andrzejsokolowski/starsector-astral-ascension-renewed/releases/latest/download/AstralAscensionRenewed.zip

## Release notes: changelog only

Write only the changelog since the previous published GitHub Release. No dependency reminders,
installation guides, build narration, test status or checksums.

## Account and packaging safety

Use GitHub account andrzejsokolowski and the personal Git identity with odiihinia@gmail.com.
Check the authenticated account before publishing. Do not print credentials or change global auth settings.
Do not force-push or discard unrelated work. Do not install the mod into the live game folder.
Do not post to Discord or the forum unless the user asks.
