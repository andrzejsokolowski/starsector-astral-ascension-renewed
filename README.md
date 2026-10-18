# Astral Ascension Renewed

A continuation of Planetace's Astral Ascension, made with the original author's blessing. An Omega core can govern a
colony, and only an Omega core can take a colony past size 10.

## Omega core administrator

Assign an Omega core as a colony's administrator the same way as an Alpha core: it shows up in the
administrator list when you have one in your cargo or the colony's storage. It brings three skills:

- **Ethereal Enlightenment:** hazard rating -25%, ship quality +25%, every building's production x2
  and demand halved (rounded down), +1 maximum industries.
- **Acumenious Oracle:** stability +3, ground defenses x3, fleet size x2.
- **Delphic Optimiser:** colony income x1.25, colony upkeep x0.75, accessibility +50%.

## Colonies past size 10

Colonies grow to size 10 on their own (the base game stops at 6; this is a setting). Past 10, only a
colony governed by an Omega core keeps growing, up to size 15. Each size past 10 takes 1.5 times
longer to reach than it normally would, compounding, so size 15 is a lifetime's work.

| Size | Stage | Bonuses |
| --- | --- | --- |
| 11-12 | Arcology World | +1 maximum industries, accessibility +20%, colony income x1.25, ground defenses x1.5, fleet size +25%, hazard rating -25% |
| 13-15 | Ecumenopolis | +2 maximum industries, accessibility +40%, colony income x1.5, ground defenses x2, fleet size +50%, hazard rating -50% |

The planet itself turns into a city, day and night. It gets its old look back if the colony shrinks.

## The Great Civil War

A colony past size 10 is too big to be governed by a lesser mind. Trying to remove its Omega core
opens a warning, the same way the base game warns about a rogue Alpha core. Go through with it and
the colony falls into a Great Civil War: it drops to size 8, gains a decivilized subpopulation and
loses 5 stability for half a year. You get the Omega core back.

## AoTD

AoTD - Theory of Toolbox doubles a colony's food demand, domestic goods demand and farm output with
every size. Past size 10 this mod holds that doubling at its size-10 amount, so a big colony keeps
growing like everything else instead of outweighing the whole Sector. Below size 11 nothing changes.
This can be switched off in the settings.

## Omega core drops

Destroyed Omega ships can drop their Omega core. If Omega Core Everywhere is installed, its drop
settings are used instead.

## Settings

Open LunaLib's settings (`Shift+F2` in the campaign) and pick **Astral Ascension Renewed**: the size
limit without an Omega core, how much slower growth past 10 is, the AoTD cap, and Omega core drops.

## Console commands

With Console Commands installed:

| Command | What it does |
| --- | --- |
| `aar_size <3-15> [colony]` | Sets a colony's size, with its stage and planet look. Past 10 it also installs an Omega core administrator. |
| `aar_omegaadmin [colony]` | Installs an Omega core administrator without using up a core. |
| `aar_civilwar [colony]` | Runs the Great Civil War on a colony at once. |
| `aar_growth <0-100> [colony]` | Sets how far a colony has grown towards its next size, in percent. |
| `aar_status [colony]` | Shows size, size limit, growth, administrator, stage and AoTD scaling. |

Leave out the colony name to use the colony you are docked at.

## Compatibility

- Replaces Astral Ascension. Do not run both; the game refuses to start if you try.
- Saves made with the original Astral Ascension do not load with this mod.
- Cannot be removed from a save once a colony has an Omega administrator or has grown past size 10.
- Big worlds placed by other mods are left alone unless you give them an Omega core administrator.

## Requirements

- [LunaLib](https://fractalsoftworks.com/forum/index.php?topic=25658.0)

## Building

Set `starsectorPath` in `gradle.properties`, then run `gradlew releaseZip`. The build runs every
check and writes `AstralAscensionRenewed.zip` next to this file, ready for a mod manager. Compiling
needs Console Commands installed in the game's `mods` folder; playing does not.

`tools/make_textures.py` regenerates the planet textures, city lights and icons, and
`tools/make_data.py` the CSV data files.
