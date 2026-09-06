# Changelog

All notable changes to Build a Spell are documented here.

## v1.0.3 — Fill rises, and Conjure builds what you are holding

Fill's reach limit was acting as a wall of its own, so a room wider than the limit got one flat layer across its floor and nothing above it. Running out of reach no longer ends a cast: the limit simply marks the edge of what one cast can touch. Alongside it: Conjure builds with the block in your off hand, Fortunate Son raises the Looting level of the loot roll instead of multiplying the finished drops, and Echo and Double are repriced against the spell they repeat rather than charging a flat fee.

### Changes

- **Running out of reach no longer ends a Fill**: a layer that ran out to the limit used to count as one that had escaped into the open, and the cast stopped where it stood. Reaching the limit now only stops that one layer spreading.
- **The reach limit is a distance in every direction, and a cast has a budget**: thirty blocks out, up and down from where the spell landed, and four thousand and ninety-six blocks placed at most. Both are server-configurable.
- **Fill judges the surface by what is overhead, not by how far it got**: a layer closed in by walls is held however wide it is, and a hole in a ceiling is plugged on the way past. A ceiling the fill could never reach does not count as one, so a cast on the Nether's open lava flats places nothing.
- **Conjure builds with the block in your off hand and never consumes it**: cobblestone in hand builds cobblestone, and mana is the whole cost. It used to copy whatever block the cast landed on, so the material changed with your aim.
- **What Conjure may build with is the block tag `#buildaspell:conjurable`**: common ground material only, extendable by datapack. The `conjureAllowedBlocks` setting is gone, having matched a block's full name against a list of bare ones, so out of the box nothing matched.
- **Fortunate Son raises the Looting level of the loot roll**: it only increases what vanilla would let Looting increase, so a guaranteed drop such as a Wither's nether star is no longer multiplied, and a bare-handed cast grants it just as an armed one does. Reap does the same with Fortune.
- **Echo and Double charge four fifths of what the rest of the spell costs**, in place of a flat fifty and forty, and stacks compound rather than add. Each takes a `totalCostMultiplier` in `modifiers.toml`, and a server still holding the old flat fees is moved off them on the first world load.
- **Sunder multiplies the spell's damage instead of adding a flat figure of its own**, by an amount that follows how heavily armored the target is, and it stops at three times the spell's damage however many copies you attach. `bonusFractionPerLevel`, `maxMultiplier` and `fullArmorValue` in `modifiers.toml` tune it.
- **Stacking one modifier onto one effect costs more with each stack**, counted per effect, with delivery modifiers forming one further group. Server-configurable; 1 restores flat pricing.
- **Fortunate Son stacks as far as you can pay for**: the cap of three used to charge you for a fourth and give you nothing back. The escalating cost is the limit now, the cap survives as a backstop at 25, and a server still on the old three is moved up on the first world load.
- **The Spell Builder's build area scrolls sideways**, since modifiers extend their effect's row to the right rather than wrapping, and a spell holds a hundred entries in each of its two lists, up from thirty. `maxSpellComponents` in `general.toml` sets a server's own figure.
- **Iron's Spells 'n Spellbooks can keep the mana**: on the 1.21.1 build, `deferManaToIronsSpellbooks` in `general.toml` spends and refills out of Iron's pool instead of running a second one beside it, so gear granting `irons_spellbooks:max_mana` moves both mods together. Iron's then sets the pool and its refill rate, so the altar stops offering Mana Pool and Mana Regeneration. Off by default.

### Fixes

- Summoned skeletons and vindicators arrive armed, roughly half bows and half swords at a server-configurable split and an iron axe respectively, and a summon drops neither loot nor experience nor the weapon it was given. Both used to spawn bare-handed, and an Iron Golem paid back the iron it was made from.
- The Tracking delivery holds one living target instead of chasing the nearest entity of any kind, and steers at the speed it was fired rather than accelerating. `homingStrength` is a share of the way from heading to target, so its range is nought to one and a larger setting is returned to the default.
- Component tooltips in the Spell Builder quote the price you will actually pay, including the escalating cost of repeats, any server cost settings, and datapack-added effects that the total used to count as free.
- A large spell is saved whole. The effect chain and the delivery's modifiers are two separately limited lists, but saving sent both as one and the server measured it against the chain's limit alone, so the tail was dropped without a word and the loss only showed at the next login.
- A full mana bar reads as full. Mana is sent to your client once a second and only when the figure has changed, but the comparison was against the figure at the start of that same second rather than against what your client held, so the last step of regeneration was never sent.
- A stack of Blank Runes fills one rune at a time: essence was recorded on the stack, so a pile charged together and then only one transformed, leaving the rest stuck at full. Runes stranded that way are cleared first, one for each essence you gather.
- The 1.21.1 build names 1.21.1 exactly rather than everything below 1.22, so a newer game refuses it up front with a message naming the version it wants rather than loading it and crashing part-way through startup. The startup log also reports the mod's real version, having announced a hardcoded one that was wrong on every branch.
- On Minecraft 26.2, Duration lengthens a summoned host again: summoned skeletons and vindicators ignored the modifier and always lasted the configured time. Vexes and the other Minecraft versions were unaffected.
- The Arcane Altar enchants to level 255 rather than stopping at 20, which is what the guidebook and the creative tab already described. The per-enchantment settings in `general.toml` cap how far a level scales the bonus, not how high one may be bought, and the cost curve already priced levels past fifteen in netherite.
- The creative search files one Build a Spell enchanted book per enchantment rather than one per level, and the mod's menus no longer look blurred, every centred heading having carried a drop shadow that doubled each letter a pixel down and to the right.

### Documentation

- The guidebook and the component reference are brought up to this release: Fill's reach limit and per-cast budget, Conjure's off-hand rule and block tag, Echo's and Double's share of the spell, Sunder's multiplier and what it does to an unarmored target, Tracking's single living target, and a summoned host that arrives armed and leaves nothing behind.
- The mana-cost pages price repeats, in the guidebook and in the read-me, the documentation home page and the configuration reference alike: each further copy of an effect costs half again as much as the one before, and a modifier is priced against the effect it sits on. The configuration page points at the block tag rather than a Conjure block list.
- The guidebook is identical on every Minecraft version. One version's copy still published combo recipes in full and described several mechanics it no longer had, among them a Cast Speed attribute that does not exist, a cross-dimensional Recall, Nullify stopping Slowness and Weakness, and a Rune that triggered on contact rather than on a timer.
- The cross-mod reference gives mana regeneration per second rather than per tick, a twentyfold difference, and drops the mod's former name. The read-me names all seven delivery methods, Getting Started lists every optional integration, and the Geyser page no longer calls a combo that never lifts its caster a mobility tool.

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

- **Minecraft 1.21.1** — NeoForge 21.1.224 or newer, Java 21
- **Minecraft 26.1.2** — NeoForge 26.1.2 or newer, Java 25
- **Minecraft 26.2** — NeoForge 26.2.0.77 or newer, Java 25

### Optional integrations

- **NeoPortals** — enhanced portal rendering when installed.
- **NeoOrigins** — origin powers can build and cast spells through the public API, charging the origin system's own resource.
