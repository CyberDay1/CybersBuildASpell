# Build a Spell

An advanced, component-based spell creation system for NeoForge: combine delivery methods, effects, and modifiers in the Spell Builder to design your own spells, unlock components, and discover powerful multi-component combos.

> Formerly *Cyber's Build A Spell*. The mod id, package, and resource namespace are now the single token `buildaspell`; the repository keeps its original name.

## Overview

Spells in Build a Spell are assembled from three kinds of components:

- **Delivery method** — how the spell is cast (self, line of sight, projectile, homing projectile, or a ground rune).
- **Effects** — what the spell does (damage, status effects, terrain manipulation, summons, utility, and more).
- **Modifiers** — how the effects behave (more power, larger area, longer duration, chaining, piercing, bouncing, delays, and shape modifiers).

Mana cost is the sum of every component, scaled by your Spell Power. Certain combinations of components form **combos** that override their individual behavior to produce a single, more powerful spell — black holes, tornadoes, Void Rifts (dialable, linkable portals), summon swarms, and more.

## Features

- **76 spell components**: 7 delivery methods, 47 effects, and 22 modifiers, freely combinable up to 30 components per spell.
- **19 spell combos**: special component combinations that unlock unique behavior (Black Hole, Tornado, Void Rift, Fortress, Meteor Strike, Blizzard, summon variants, and more).
- **Spell Builder GUI**: drag-and-drop component crafting with a grouped palette and live mana-cost feedback.
- **Per-spell projectile visuals**: choose a projectile's color, shape, and trail particle directly in the builder.
- **Arcane Altar**: an in-world block for enchanting and unlocking spell components.
- **Custom attributes & enchantments**: Mana Pool, Mana Regeneration, and Spell Power, with uncapped enchantment scaling.
- **Inter-player portals**: dialable, linkable portals for instant travel.
- **Deeply configurable**: deliveries, effects, modifiers, and roughly 150 individual gameplay numbers are exposed across per-category config files; individual components can be disabled (and become unobtainable) server-side.
- **Cross-mod API**: a stable public API for other mods to read/modify the attribute system and cast spells.

## Supported versions

A single jar is published per Minecraft line. Pick the one that matches your installation.

| Minecraft | Loader | Java | Notes |
|---|---|---|---|
| 1.21.1 | NeoForge 21.1.x | 21 | `1.21.1` branch |
| 26.1.x | NeoForge 26.1.x | 25 | `master` branch |
| 26.2 | NeoForge 26.2.0.0-beta | 25 | `26.2` branch — in-game guidebook temporarily disabled until Modonomicon ships a 26.2 build |

> The 1.21.11 line is not supported.

## Installation

1. Install [NeoForge](https://neoforged.net/) for your Minecraft version.
2. Download the jar matching your Minecraft version (`buildaspell-1.0.1-mc<version>.jar`).
3. Drop it into your `mods/` folder.

Optional integrations are picked up automatically when present (see below).

## Building from source

Requires the JDK matching your target branch (Java 21 for 1.21.1, Java 25 for 26.x).

```bash
git clone https://github.com/CyberDay1/CybersBuildASpell.git
cd CybersBuildASpell
git checkout <branch>   # 1.21.1 | master | 26.2
./gradlew build
```

The built jar lands in `build/libs/`. Run a development client with `./gradlew runClient`.

## Documentation

Full docs live in [`docs/`](docs/) and are published as a site via GitHub Pages.

- **[Getting Started](docs/getting-started.md)** — install, open the Spell Builder, cast your first spell.
- **[Spell Components Reference](docs/SPELL_COMPONENTS.md)** — every delivery method, effect, modifier, and combo, with costs, formulas, and interactions.
- **[Configuration](docs/configuration.md)** — per-category config files; tuning and disabling components.
- **[Cross-Mod Integration Guide](docs/CROSS_MOD_API.md)** — how other mods read and modify mana, spell power, and casting behavior.

## Integrations

- **NeoOrigins** (optional) — a public casting API lets origin powers build and cast spells, charging the origin system's own resource. See `CROSS_MOD_API.md`.

## Configuration

Server config is split by category under `config/buildaspell/` (`general`, `deliveries`, `effects`, `modifiers`), with client options in `client.toml`. Server settings sync to clients on login. Almost every gameplay number — costs, durations, ranges, damage, combo tuning — is adjustable, and any component can be disabled server-side.

## License

All Rights Reserved. The source is publicly available for reference; it is not currently released under an open-source license.
