# Changelog

All notable changes to Build a Spell are documented here.

## v1.0.0 — First public release

The debut of Build a Spell: a component-based spell-crafting system for NeoForge. Assemble your own spells from delivery methods, effects, and modifiers in the Spell Builder, unlock components through an enchanting-style progression, and discover powerful multi-component combos.

### Highlights

- **76 spell components** — 7 delivery methods, 47 effects, and 22 modifiers, freely combinable up to 30 per spell. A spell's mana cost is the sum of its components, scaled by your Spell Power.
- **19 spell combos** — certain component combinations collapse into a single, more powerful spell: Black Hole, Tornado, Blizzard, Meteor Strike, Void Rift, Fortress, summon swarms, and more. Discovering which combinations line up is half the fun — go experiment.
- **Spell Builder GUI** — drag-and-drop crafting with a grouped component palette, live mana-cost feedback, and per-spell projectile visuals (color, shape, and trail particle).
- **Rune progression** — Blank Runes gather Essence from both combat and casting and graduate into Spell Runes that unlock new components, with a one-time starter kit so you can cast from your very first login.
- **Arcane Altar** — an in-world station for enchanting and unlocking spell components.
- **Custom attributes & enchantments** — Mana Pool, Mana Regeneration, and Spell Power, with uncapped enchantment scaling.
- **Inter-player portals** — dialable, linkable portals for instant travel.
- **Deeply configurable** — roughly 150 individual gameplay numbers across per-category server config files; any component can be disabled (and made unobtainable) server-side, and server settings sync to clients on login. A new `particleDensity` option scales the big combo-spell particle effects for servers that want to trim FPS cost.
- **Cross-mod API** — a stable public API other mods can use to read and modify the attribute system and cast spells (used by NeoOrigins).

### Compatibility

- **Minecraft 1.21.1** — NeoForge 21.1.x, Java 21
- **Minecraft 26.1.2** — NeoForge 26.1.x, Java 25
- **Minecraft 26.2** — NeoForge 26.2 beta, Java 25. The in-game guidebook is temporarily disabled on this line until Modonomicon ships a 26.2 build.

### Optional integrations

- **NeoPortals** — enhanced portal rendering when installed.
- **Create** — adds the `smelt`, `mill`, and `spin` effects, which use Create's processing mechanics.
- **NeoOrigins** — origin powers can build and cast spells through the public API, charging the origin system's own resource.
