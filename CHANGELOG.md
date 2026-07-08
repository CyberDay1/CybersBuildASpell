# Changelog

All notable changes to Build a Spell are documented here.

## v1.0.2 — Fill knows what it is standing in

Fill used to measure distance alone, which made it a solid sphere of thirty blocks' radius that reached straight through walls. It now fills the space the spell actually lands in and stops where that space stops. Alongside it: Fortunate Son no longer duplicates a player's belongings when it kills them.

### Changes

- **Fill fills the space the spell lands in** — it walks from block to touching block instead of measuring distance, so it can only reach what that space actually connects to.
- **Conjure and Create Water pour** — they settle to the floor and rise a layer at a time, stopping where the space would spill out, so a hole fills to ground level and a sealed room fills to its ceiling. Cast somewhere genuinely open, nothing holds the fill and nothing is placed.
- **Break and Evaporate Water follow the mass** — Break clears the run of blocks it struck and stops at the open air, and Evaporate drains the body of water it was cast into and stops at the shore.
- **The Fill radius is a limit rather than a shape** — still thirty blocks by default, still unaffected by Increased Area, but it now bounds how far the search travels. A new server setting caps how many blocks one cast will touch, at 4096 by default. Wall and Floor still take priority over Fill.
- The guidebook and the component reference describe the new Fill.

### Fixes

- Fortunate Son no longer duplicates a player's belongings: a player's death drops are their inventory rather than rolled loot, so multiplying them copied what the victim was carrying. Fortunate Son now leaves player drops alone and only affects the loot a creature rolls.
- Fortunate Son's bonus drops stop at an item's own stack limit, so a mob's dropped sword can no longer come back as two.
- Fortunate Son's effective level is capped at 3, at a server-configurable cap. Stacking it used to raise the underlying Fortune and Looting level without limit.

### Documentation

- Fortunate Son's guidebook page and reference entry now give the cap of three and say that a player's own belongings are left alone. On Minecraft 26.2 the page was further behind still, describing Fortunate Son as a Fortune enchantment for block-breaking spells and mentioning neither Reap nor Looting.
- Create Water's reference entry said it placed a single water source block; it has always filled a sphere of the spell's range.
- The configuration summary omitted `wands.toml` from the list of server config files and undercounted the tunable gameplay values.

## v1.0.1 — Delivery modifiers and a wand rebalance

The modifiers that shape a whole projectile now belong to the delivery rather than to a single effect, and the Spell Builder gives them their own category beside it. Chain is rebuilt to spread the projectile from whatever it strikes, and Linger settles into a lingering-potion cloud. Alongside the rework: a rebalance that brings the three wands far closer together, and the first round of fixes from reports since launch.

### Changes

- **Eleven modifiers moved to the delivery** — Double, Split, Accelerate, Pierce, Bounce, Return, Chain, Delay, Echo, Linger and Duration now act on the spell's delivery as a whole rather than binding to whichever effect they sat beside. Existing saved spells migrate the moment they load, so nothing you have built breaks.
- **Chain spreads the projectile from whatever it strikes** — it arcs between creatures on an entity hit, or hops between blocks of the same kind on a solid impact, resolving the whole spell at every target it reaches. Reach per hop and hops per stack are both server-configurable.
- **Linger renders as a lingering-potion cloud** — tinted to the spell's own colour, re-casting its effect on everything inside it for the duration.
- **A dedicated "Delivery Modifiers" palette category** — modifiers that cannot work on the chosen delivery grey out with an explanation, and one already placed is outlined in amber rather than silently doing nothing.

### Balance

- **Wand Spell Power is now +6 / +15 / +30** for Worn, Carved and Runic wands, down from +10 / +25 / +75. A plain Damage effect lands for 2 bare-handed and 8 with a Runic wand.
- **Repeating the same effect in one spell costs more each time**, at a server-configurable rate; setting that rate to 1 restores the old flat pricing exactly.
- **Stacked Damage lands as a single blow**, so armour, Protection and Resistance all measure themselves against the full total.
- Server settings written before this release are brought up to date on the first world load, leaving anything you had already tuned yourself exactly as you set it.

### Fixes

- The Arcane Altar no longer crashes when a slot holds more than 64 of a material.
- Spells and the component palette survive dying and travelling between dimensions.
- Spell Power is read from the main hand and Mana Pool and Mana Regeneration from worn armour, and the altar only offers an enchantment on an item where it will actually be read.
- Launch, Pull, Push, Yeet and Slam now move players as well as every other creature.
- The Spell Visuals screen applies again, and resizing the window no longer discards a spell you are part-way through building.
- Conjure treats every replaceable block as free space (light, grass, snow, water), matching how it already behaved with Fill, so it no longer refuses to build through its own Light effect.
- Light no longer lasts forever: the lights a cast places clear themselves after a minute, at a server-configurable duration, and the countdown is stored with the world. Break clears the permanent lights left behind by v1.0.0, and re-casting Light over them turns them into ones that expire.

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
