---
title: Spell Components
parent: Reference
nav_order: 1
---

# Spell Components Reference

{: .note }
Want to retune these or add your own? See [Datapack Components]({% link datapack-components.md %})
for authoring components from a datapack.

## Delivery Methods

| ID | Base Cost | Description |
|---|---|---|
| `rune` | 20.0 | Spawns animated ground rune marker that executes after 1 second delay with particle effects |
| `sight` | 15.0 | Targets where player is looking (20 block range) |
| `self` | 5.0 | Centered on player position |
| `cast` | 25.0 | Fires spell projectile from player's eye position (1.5 speed, no gravity) |
| `tracking` | 35.0 | Fires homing projectile from player's eye position (1.5 speed, homes on nearest entity, no gravity) |
| `touch` | 15.0 | Imbues the spell into the caster's hands; the next melee hit or interaction discharges it (charge lasts a limited time) |
| `trap` | 25.0 | Places a rune trap on the targeted surface (up to 30 block reach) that triggers when an entity steps onto it |

## Spell Effects

| ID | Base Cost | Description | Damage Formula |
|---|---|---|---|
| `damage` | 5.0 | Deals magic damage to entities in range | `spellPower × 0.25` |
| `ignite` | 15.0 | Sets entities on fire, places fire blocks on solid surfaces | Power level increases duration, Prolonged extends duration |
| `freeze` | 20.0 | Applies freeze ticks to entities, converts water to ice, lava to obsidian, spawns snow layers | Power level increases duration, Prolonged extends duration |
| `teleport` | 50.0 | Teleports caster to spell origin | None |
| `pull` | 25.0 | Pulls entities toward spell origin | Power level increases strength |
| `push` | 25.0 | Pushes entities away from spell origin | Power level increases strength |
| `yeet` | 30.0 | Launches entities in their facing direction | Power level and Spell Power stat increase strength |
| `reap` | 40.0 | Auto-harvests and replants mature crops | Fortunate Son adds bonus drops, Gentleness prevents fortune multiplication |
| `explosion` | 60.0 | Creates explosion at origin | Blast power: `2.0 + powerLevel × 0.75 + areaLevel × 0.5`. Blocks break at Power ≥2 (TNT-style drops at ≥4); Nullify removes entity damage only |
| `heal` | 35.0 | Heals living entities or damages undead | Heal: `spellPower × 0.3` / Undead: `spellPower × 0.6` |
| `lightning` | 55.0 | Summons lightning strikes on entities in range, or at origin if no entities | Number of strikes: `1 + powerLevel / 10`, with the remainder giving a proportional chance of one more strike |
| `poison` | 30.0 | Applies poison effect to entities | Base duration: 80 ticks (4s), Power level increases duration, Prolonged extends duration, Amplifier: `spellPower / 15` (every 15 spell power increases tier) |
| `wither` | 40.0 | Applies wither effect to entities | Base duration: 80 ticks (4s), Power level increases duration, Prolonged extends duration, Amplifier: `spellPower / 15` (every 15 spell power increases tier) |
| `saturation` | 20.0 | Applies saturation effect to entities | Power level increases duration and amplifier, Prolonged extends duration |
| `launch` | 35.0 | Launches entities upward | Power level and Spell Power stat increase strength |
| `light` | 12.0 | Spawns invisible light source at origin | Power level increases brightness (12-15) |
| `slam` | 30.0 | Forces entities straight down at high speed | Power level and Spell Power stat increase downward velocity |
| `levitation` | 25.0 | Applies levitation effect to entities | Power level increases duration and amplifier, Prolonged extends duration |
| `slow_fall` | 15.0 | Applies slow falling effect to entities | Power level increases duration, Prolonged extends duration |
| `break` | 40.0 | Breaks blocks within range and drops items | Power level increases max hardness breakable (base 3.0 + power × 2.0), Gentleness applies silk touch, Fortunate Son applies fortune |
| `invisibility` | 30.0 | Applies invisibility effect to entities | Power level increases duration, Prolonged extends duration |
| `speed` | 25.0 | Applies movement speed effect to entities | Power level increases duration and amplifier (max III), Prolonged extends duration |
| `haste` | 25.0 | Applies haste (dig speed) effect to entities | Power level increases duration and amplifier (max III), Prolonged extends duration |
| `blink` | 35.0 | Teleports caster in look direction | Distance: 5.0 + power × 2.0 + spellPower × 0.1 (max 20 blocks), stops at solid blocks |
| `swap` | 45.0 | Swaps positions with nearest entity in range | Targets nearest living entity |
| `summon` | 50.0 | Summons tamed wolves at origin | Summons 1 + power level wolves (max 5), arranged in circle pattern |
| `create_water` | 20.0 | Places a water source block at spell origin | Replaces air or replaceable blocks only |
| `evaporate_water` | 25.0 | Removes water source blocks within range | Removes all water sources in spherical radius |
| `mark` | 30.0 | Sets player's recall mark at spell origin | One mark per player, spawns red flame particles in circle pattern |
| `recall` | 40.0 | Teleports player to their mark location | Requires mark to be set, resets fall distance, spawns portal particles |
| `pickup` | 15.0 | Collects all item entities within 5 block radius | Items are added directly to caster's inventory, only picks up items with available inventory space |
| `shield` | 45.0 | Creates protective barrier for nearby entities | Grants absorption hearts, more effective with higher spell power |
| `blind` | 25.0 | Applies blindness effect to entities | Power level increases duration, Prolonged extends duration |
| `charm` | 35.0 | Applies charm effect to entities, causing them to be passive toward caster | Power level increases duration, Prolonged extends duration |
| `cleanse` | 20.0 | Removes negative potion effects from nearby entities | Affects all living entities in spell range |
| `conjure` | 25.0 | Creates blocks at spell origin | Copies target block if allowed in config, otherwise places stone. Works with wall/floor/fill modifiers to control shape |
| `growth` | 30.0 | Accelerates plant growth and crop maturation in area | Power level increases growth rate |
| `slow` | 20.0 | Applies slowness to entities in range | Power level increases amplifier (max III) and duration, Prolonged extends duration |
| `weaken` | 25.0 | Applies weakness to entities in range | Power level increases amplifier (max III) and duration, Prolonged extends duration |
| `strengthen` | 30.0 | Applies strength to living entities in range | Power level increases amplifier (max III), Prolonged extends duration |
| `regenerate` | 35.0 | Applies regeneration to living entities in range | Power level increases amplifier (max II), Prolonged extends duration |
| `resist` | 35.0 | Applies resistance (damage reduction) to living entities in range | Power level increases amplifier (max III), Prolonged extends duration |
| `night_vision` | 15.0 | Applies night vision to living entities in range | Prolonged extends duration |
| `water_breathing` | 15.0 | Applies water breathing to living entities in range | Prolonged extends duration |
| `root` | 30.0 | Snares entities in place with heavy slowness and jump suppression | Power level increases duration, Prolonged extends duration |
| `grapple` | 35.0 | Pulls the caster toward the spell origin in an arc | Power level increases pull strength |
| `gust` | 30.0 | Blasts entities in a cone along the caster's look direction | Power level increases push strength |

## Modifiers

| ID | Base Cost | Stackable | Description |
|---|---|---|---|
| `increased_area` | 15.0 | Yes | Increases spell range by 1 block per stack (base: 5.0 blocks) |
| `increased_power` | 20.0 | Yes | Increases power level by 1 per stack (affects damage, effect duration/amplifier, explosion strength) |
| `nullify` | 5.0 | No | Removes all entity damage from spells (effects and combos); status effects, knockback, ignition, and block changes are unaffected |
| `gentleness` | 10.0 | No | Break collects blocks intact (silk touch); Reap harvests without fortune multiplication |
| `fortunate_son` | 25.0 | Yes | Adds fortune level to Break effect (ore drops), Reap effect (crop drops), and looting level to spell damage (mob drops). Each stack provides +1 Fortune/Looting level |
| `double` | 50.0 | Yes | Increases projectile count by 1 per stack for Cast/Tracking delivery methods (projectiles spread horizontally) |
| `echo` | 40.0 | Yes | Re-casts the spell after a short delay (10 ticks per echo); each echo strikes at reduced power (×0.8 per echo). Works with every delivery method |
| `prolonged` | 18.0 | Yes | Extends duration of timed effects: Poison (+60 ticks/level), Wither (+50 ticks/level), Ignite (+40 ticks/level), Freeze (+40 ticks/level), Levitation (+40 ticks/level), Slow Fall (+40 ticks/level), Invisibility (+40 ticks/level), Speed (+40 ticks/level), Haste (+40 ticks/level), Saturation (+100 ticks/level), Charm (+40 ticks/level), Blind (+40 ticks/level), Growth |
| `delay` | 8.0 | Yes | Delays spell execution by 10 ticks (0.5s) per stack (1-500 ticks total). Works with all delivery methods: Rune adds to cast delay, Cast/Tracking delays after projectile hit, Self/Sight delays after cast |
| `chain` | 30.0 | Yes | Chains spell effects to nearby entities of the same type. Each stack allows 2 additional chains (level 1 = 2 chains, level 2 = 4 chains, etc.). Works with Damage, Heal, and Lightning effects. Searches within 5 block radius for matching entity types. Prevents hitting the same entity twice. **Chain falloff**: Each jump reduces damage by 15% (×0.85 per jump). |
| `pierce` | 20.0 | Yes | Projectiles pass through blocks and execute spell effects at each block hit. Each stack allows passing through 1 additional block. Only works with Cast and Tracking delivery methods. |
| `bounce` | 25.0 | No | Projectiles bounce off blocks instead of stopping (up to a few bounces), reflecting off the surface at full speed. Only works with Cast and Tracking delivery methods. |
| `accelerate` | 15.0 | No | Projectiles launch faster: adds a flat bonus to the projectile's starting speed (they then fly at that constant speed). Only works with Cast and Tracking delivery methods. |
| `duration` | 35.0 | Yes | Creates persistent area entity affected by gravity that repeatedly applies spell effects every 5 ticks (0.25s). Duration: 100 + level × 60 ticks (5-8+ seconds). If the spell matches a combo, the combo takes precedence over the persistent area. |
| `linger` | 25.0 | No | Settles the spell where it lands as a persistent area that re-applies its other effects in pulses (every 5 ticks). A cheaper, one-time enabler with a fixed base lifetime (100 ticks); stack Duration to extend it and Increased Area to widen it. If the spell matches a combo, the combo takes precedence |
| `split` | 45.0 | Yes | Increases projectile count by 1 per stack for Cast/Tracking delivery methods (projectiles spread horizontally). Each stack provides an additional projectile. |
| `wall` | 30.0 | No | Modifies Conjure to create a vertical wall shape at spell origin |
| `floor` | 30.0 | No | Modifies Conjure to create a horizontal platform shape at spell origin |
| `fill` | 40.0 | No | Modifies Conjure to fill in holes and gaps at spell origin |
| `leech` | 30.0 | Yes | Heals the caster for a fraction of the Damage effect's damage dealt. Each stack increases the healed fraction. Only affects the Damage effect |
| `sunder` | 25.0 | Yes | Adds bonus Damage based on the target's armor value, so heavily armored targets take more. Each stack increases the bonus. Only affects the Damage effect |
| `return` | 35.0 | No | Cast/Tracking projectiles boomerang back to the caster after reaching their target. Only works with Cast and Tracking delivery methods |

## Base Mechanics

- **Base Range**: 5.0 blocks (increased by the Increased Area modifier)
- **Mana Cost**: sum of delivery + all effects + all modifiers (modified by the Spell Power stat)
- **Effect Execution**: all effects execute sequentially at the determined origin point
- **Spell Power Stat**: obtained from enchantments; affects damage/healing amounts and projectile strength
- **Projectiles**: Cast and Tracking deliveries spawn glowing light orb projectiles with no gravity, a 200 tick lifetime, flame particles, and proper collision detection
- **Projectile Rendering**: light blue glowing orbs using vanilla billboard rendering with fullbright lighting
- **Projectile Behavior**: Tracking projectiles home on the nearest living entity within 16 blocks; Cast projectiles fly straight
- **Rune Markers**: spawn an animated ground decal at the player's feet with rotating particles, a 1-second cast delay, and a pulsing scale effect

## Spell Combos

Some component combinations do more than stack their individual effects. When the right pieces come together, the spell collapses into something greater than the sum of its parts: a combo.

Combos are meant to be discovered, not read about. Pairing movement with force, summons with raw power, or water with reach can produce results that no single effect in the tables above can explain. Watch for spells that behave in ways you did not expect, then chase the pattern.

> Combo recipes are intentionally left out of this documentation. Half the fun is figuring them out: experiment in the Spell Builder and compare notes with other casters.

## Damage Scaling

All damage in the spell system scales exclusively with **Spell Power**. The formulas are:

### Direct Damage Effects
- **Damage Effect**: `spellPower × 0.25` damage per hit
- **Heal Effect (vs Undead)**: `spellPower × 0.6` damage per hit
- **Heal Effect (Living)**: `spellPower × 0.3` health restored

### Damage Properties
- All spell damage uses `indirectMagic` damage source
- Respects armor values and Protection enchantments
- Reduced invulnerability frames: 5 ticks (0.25 seconds) instead of vanilla 20 ticks
- Nullify modifier removes **all creature damage** from a spell, including: Damage effect, Heal effect (undead damage), Explosion effect (entities only), Lightning effect (strikes become visual-only, including the Lightning Storm combo), and every other combo. Status effects (Poison, Wither, etc.), knockback, ignition, and block changes are not damage and still apply. Nullify does not protect terrain: a nullified high-power Explosion still breaks blocks

### Spell Power Sources
Spell Power and mana come from two sources:
- **Enchantments on equipment** (applied at the Arcane Altar):
  - **Mana Pool**: Increases maximum mana. Read from worn armor.
  - **Mana Regeneration**: Increases mana regeneration rate. Read from worn armor.
  - **Spell Power**: Directly increases spell damage output. Read from the item in your main hand (a weapon or a wand).
  - Each enchantment only counts from the slots listed above. An off-hand item or a second held wand never adds to the total, so only one wand's Spell Power applies at a time.
- **Holding a wand**: higher-tier wands grant bonus Spell Power while held (and a mana-cost discount). See the wand progression in-game.

## Effect Interactions

- **Nullify**: Prevents all creature damage from any spell component. For combo spells, this removes damage while preserving their movement and physics effects. Status effects are not damage: Poison, Wither, and the other debuffs still apply under Nullify, as do knockback and ignition. For Lightning, the strikes become visual-only (no damage or fire); this also covers the Lightning Storm combo. For Explosion, entity damage is prevented but knockback remains and blocks still break per the power thresholds: Nullify does not protect terrain
- **Fortunate Son**: Provides three benefits: (1) Applies Fortune enchantment to Break effect for increased ore/block drops, (2) Applies fortune to Reap effect for bonus crop drops, (3) Applies Looting to all spell damage - when mobs are killed by spells, they drop bonus items (0-1 bonus drops per level per item, same as vanilla Looting)
- **Gentleness + Fortunate Son**: Fortunate Son adds bonus drops but Gentleness prevents fortune multiplication (Reap effect only). For Break effect, Gentleness takes priority and applies silk touch behavior. Looting from mob kills is not affected by Gentleness.
- **Prolonged**: Affects Ignite, Freeze, Poison, Wither, Saturation, Levitation, Slow Fall, Invisibility, Speed, Haste, Charm, Blind, and Growth effects
- **Power Level**: Affects most effects differently - increases damage/healing, effect durations, explosion power, projectile counts, etc. For Break effect, increases max block hardness that can be broken. For Blink, increases teleport distance. For Speed/Haste, increases amplifier (max III). For Summon, increases wolf count (max 5).
- **Increased Area**: Affects all area-based effects (all except Teleport and Blink which always affect caster). Several combos also call for it.
- **Delay**: Adds execution delay to all spell types - stacks with Rune's base delay, delays projectile hits, or delays direct cast execution
- **Chain**: Only works with Damage, Heal, and Lightning effects. Chains recursively to nearby entities of the same EntityType within 5 blocks. Tracks hit entities to prevent infinite loops. Each stack adds 2 chain attempts (level 1 = 2 chains, level 2 = 4 chains).
- **Pierce**: Only works with Cast and Tracking delivery methods. Projectiles pass through solid blocks (1 block per stack) and execute spell effects at each block hit location.
- **Bounce**: Only works with Cast and Tracking delivery methods. Projectiles reflect off surfaces instead of stopping.
- **Accelerate**: Only works with Cast and Tracking delivery methods. Projectiles launch at a higher starting speed (a flat bonus added at cast), then fly at that constant speed.
- **Split**: Only works with Cast and Tracking delivery methods. Increases projectile count by 1 per stack (each stack adds an additional projectile), spreading projectiles horizontally.
- **Duration**: Creates a falling area-of-effect entity that applies all spell effects every 5 ticks to entities/blocks within range. Falls with gravity and slows down on ground. Combo detection runs first: if the spell matches a combo, the combo executes instead of the persistent area. Duration increases with modifier level (5-8+ seconds).
