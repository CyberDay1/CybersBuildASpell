---
title: Configuration
parent: Guides
nav_order: 2
---

# Configuration

Build a Spell is extensively configurable. Server settings are split by category and sync to clients automatically on login, so players always use the server's values.

## Config files

Server config lives under `config/buildaspell/`, split by category:

| File | Controls |
|---|---|
| `general.toml` | Global settings: base range, the global mana-cost multiplier, how large a spell may get, and shared tuning. |
| `deliveries.toml` | Per-delivery enable flags, base mana cost, and cost multipliers. |
| `effects.toml` | Per-effect enable flags, base mana cost, cost and damage multipliers, and effect tuning. |
| `modifiers.toml` | Per-modifier enable flags, base mana cost, and cost multipliers. |
| `wands.toml` | Per-tier wand tuning: the mana-cost discount and bonus Spell Power each wand tier grants while held. |

Client-only options live in `client.toml`.

Server config is per-world (with `defaultconfigs/` support), and every server value is pushed to clients on login through NeoForge's config sync: there is no custom packet to manage.

The `configVersion` at the top of `effects.toml`, `modifiers.toml` and `wands.toml` is bookkeeping rather than a setting: it records which balance pass the file was last brought up to, and is best left alone.

## Enabling and disabling components

Each delivery, effect, and modifier has an `enabled` flag. Disabling a component makes it **unobtainable everywhere**: it disappears from the Spell Builder palette, can't be unlocked by Spell Runes, won't load from saved spells, and won't cast. This lets server admins curate exactly which components are available.

## Costs

Mana cost can be tuned at several levels:

- **`baseManaCost`**: an absolute base cost per component (defaults to the component's built-in cost).
- **`costMultiplier`**: a per-component multiplier.
- **`damageMultiplier`**: a per-effect damage scalar.
- **`globalManaCostMultiplier`**: a single multiplier applied to every spell's total.
- **`shared.repeatCostGrowth`**: how much more a repeat costs than the copy before it. Defaults to 1.5. In `effects.toml` it prices repeats of the same effect within a spell; in `modifiers.toml` it prices further stacks of the same modifier on a single effect. 1.0 restores flat pricing.
- **`totalCostMultiplier`**: only on `double` and `echo` in `modifiers.toml`. Each stack multiplies the whole spell's cost rather than adding a flat price, since each one re-scales everything the spell does: 1.8 charges 80% more, and stacks compound. 1.0 charges nothing for the modifier, so pair it with that modifier's `baseManaCost` to put it back on a fixed price.

## Spell size

`maxSpellComponents` in `general.toml` caps how large a single spell may get. It applies to each of a spell's two lists separately: the effect chain, which counts one entry per effect plus one per modifier attached to an effect, and the modifiers hung on the delivery itself. Both default to 100.

Mana is the practical limiter long before this is, so the setting mostly decides how elaborate a spell a well-equipped player can build. What to watch when raising it is the work a cast asks of the server: a Duration area re-runs every effect on every pulse, so a very long chain on a long-lived spell is the expensive shape. Lowering it leaves already-saved spells intact; the new limit applies the next time one is saved.

## Range

- **`spellBaseRange`**: the base range and area radius a spell starts from, before any Increased Area modifier. Default 5 blocks, in `general.toml`.

## Enchantment scaling

The Arcane Altar's three enchantments are tuned by these `general.toml` values:

- **`spellPowerPerLevel`**: Spell Power gained per level of the Spell Power enchantment. Default 5.
- **`manaPoolMaxLevel`**, **`manaRegenMaxLevel`**, **`spellPowerMaxLevel`**: per-item caps on the level that still scales the bonus. A level above the cap can still be bought and still displays; it just stops adding anything. These do not limit what the altar will sell, which always runs to 255. Default 255, meaning no cap.

## Portals

- **`maxPortalsPerPlayer`**: how many portals one player may keep at once. Default 0, meaning unlimited.
- **`portalMinSize`** and **`portalMaxSize`**: the width and height a portal may be sized between, in blocks. Defaults 1 and 10.

## Progression

How Blank Runes gather essence into Spell Runes, and what every player starts with. All in `general.toml`:

- **`essenceRequired`**: essence a Blank Rune must gather before it becomes a Spell Rune. Default 200.
- **`killEssence`**: essence granted per regular hostile mob killed. Default 1.
- **`bossEssence`**: essence granted per boss killed. Default 50.
- **`castEssenceRatio`**: essence granted per point of mana actually spent casting. Default 0.25, and 0 turns cast progression off.
- **`starterDeliveries`** and **`starterEffects`**: the component ids every player begins with unlocked. Defaults `["cast"]` and `["damage", "break"]`.
- **`giveStarterGuidebook`**: give new players the Arcane Codex guidebook on first join. Default true, and it requires Modonomicon to be installed.

## Blocks and visuals

- **`lightningTransmutesBlocks`**: whether spell-cast lightning runs Build a Spell's own block transmutations where a bolt lands, such as sand fusing into glass. Vanilla bolt side effects always apply regardless, and a Nullified strike never transmutes. Default true.
- **`particleDensity`**: density of the big combo-spell particle effects (tornado, blizzard, black hole). 1.0 is the full default look, lower values thin the particles out, and 0.0 turns those particles off entirely. In `general.toml`, so the server sets the look for everyone.
- **`abilityRingTransparency`**: the ability ring's background transparency, in `client.toml`. Default 0.7.

What Conjure may build with is not a config setting: it is the block tag `#buildaspell:conjurable`, which a datapack can extend.

## Iron's Spells 'n Spellbooks

`deferManaToIronsSpellbooks` in `general.toml` decides whether Build a Spell keeps a mana pool of its own when Iron's Spells 'n Spellbooks is installed alongside it. It defaults to `false`, and it does nothing at all when Iron's is absent. On the 1.21.1 build only: Iron's has no release for the newer Minecraft versions.

Left off, the two mods run separate pools. That means two bars, two numbers and two unrelated refill rates for the same caster, and gear that grants `irons_spellbooks:max_mana` moves Iron's casting while leaving Build a Spell's where it was.

Turned on, Build a Spell reads and writes Iron's mana instead of its own: one pool, one bar, and gear that raises it raises both mods together. Iron's then owns both the size of the pool and the rate it refills, so Build a Spell's Mana Pool and Mana Regeneration enchantments have nothing left to act on and the Arcane Altar stops offering them. Spell Power is unaffected, since damage was never handed over. Turning the setting back off restores Build a Spell's own pool and puts both enchantments back on sale.

## Gameplay tuning

Over 200 individual gameplay numbers are exposed: durations, ranges, damage values, projectile behavior, combo parameters, entity behavior (tornado, black hole, blizzard, lingering areas), and more. Each is keyed by `<id>.<name>` in the relevant category file — combo values by `combo.<combo>.<name>`, and the handful shared across components by `shared.<name>` — with the original in-game value as its default.

A few purely cosmetic details (sound pitches, exact particle types) stay hardcoded, but most gameplay-affecting particle counts and densities are exposed alongside the numbers above.

## See also

- [Spell Components Reference](SPELL_COMPONENTS): the component IDs you'll see as config keys.
