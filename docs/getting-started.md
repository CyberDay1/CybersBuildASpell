---
title: Getting Started
parent: Guides
nav_order: 1
---

# Getting Started

This guide walks through installing Build a Spell, opening the Spell Builder, and casting your first spell.

## Install

1. Install [NeoForge](https://neoforged.net/) for your Minecraft version (1.21.1, 26.1.x, or 26.2).
2. Download the jar matching your Minecraft version: `buildaspell-1.0.0-mc<version>.jar`.
3. Place it in your `mods/` folder and launch the game.

Optional integrations (NeoPortals, NeoOrigins) are detected automatically when present.

## Open the Spell Builder

The Spell Builder is the GUI where you assemble spells. Open it with the **Spell Builder** keybind, which you can view and change under **Options → Controls** in the *Build a Spell* category.

The builder has three palette sections:

- **Delivery Methods**: choose exactly one. This decides how the spell is delivered (on yourself, where you look, as a projectile, as a homing projectile, imbued into your next touch, or as a timed ground rune or proximity trap).
- **Effects**: choose one or more. These are what the spell actually does.
- **Modifiers**: optional. These change how the effects behave (more power, larger area, longer duration, and so on).

Drag components into the spell, and the builder shows the running **mana cost**. A spell can hold up to **30 components**.

## Unlock components

You start with a small kit of components (by default the Cast delivery plus the Damage and Break effects; new players also receive the in-game guidebook). Everything else is unlocked through **Essence** and **runes**:

1. Craft a **Blank Rune** and keep it in your inventory.
2. The rune gathers **Essence** as you play: every kill deposits some (bosses give far more), and casting spells contributes Essence based on the mana you spend.
3. When a Blank Rune fills, it converts into a **Spell Rune**.
4. Use (right-click) a Spell Rune to unlock a random locked delivery, effect, or modifier.

The **Arcane Altar** is the mod's enchanting station: it applies the Mana Pool, Mana Regeneration, and Spell Power enchantments that grow your casting stats. Its input slot only accepts gear those enchantments can actually use — a weapon, a wand, or a piece of armor — so you can't sink an enchant into an item that would never read it. Spell Runes are also crafting ingredients for higher-tier wands. See the [Spell Components Reference](SPELL_COMPONENTS) for the full catalog.

## Mana and Spell Power

- **Mana** is the resource spells consume. Your maximum mana and regeneration rate come from the `mana_pool` and `mana_regen` attributes.
- **Spell Power** scales damage and effectiveness: most damage and healing in the mod scales directly off it.
- Both grow through Build a Spell's own enchantments, which stack on top of the base attribute values with no hard cap by default (server admins can cap each enchantment in the config).
- The enchantments only count from the gear that can use them: **Spell Power** is read from the item in your main hand (a weapon or a wand), while **Mana Pool** and **Mana Regeneration** are read from your worn armor. An off-hand item or a second held wand never adds to the total, so only one wand's Spell Power applies at a time.
- Holding a wand also grants bonus Spell Power and a mana-cost discount: higher-tier wands give more of each.

## Cast your first spell

A simple, cheap spell to start with:

1. Delivery: **Self** or **Sight**.
2. Effect: **Damage** (or a status effect like **Ignite**).
3. Save the spell, then cast it with your cast keybind.

From there, add a **modifier** — try **Increased Power** for stronger effects or **Increased Area** for a wider radius — and experiment. Some component combinations form **combos** (see the reference) that transform the spell entirely.

## Next steps

- [Spell Components Reference](SPELL_COMPONENTS): every component, with costs and formulas.
- [Configuration](configuration): for server admins tuning or disabling components.
