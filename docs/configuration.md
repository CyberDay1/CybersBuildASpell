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
| `general.toml` | Global settings: base range, the global mana-cost multiplier, and shared tuning. |
| `deliveries.toml` | Per-delivery enable flags, base mana cost, and cost multipliers. |
| `effects.toml` | Per-effect enable flags, base mana cost, cost and damage multipliers, and effect tuning. |
| `modifiers.toml` | Per-modifier enable flags, base mana cost, and cost multipliers. |
| `wands.toml` | Per-tier wand tuning: the mana-cost discount and bonus Spell Power each wand tier grants while held. |

Client-only options live in `client.toml`.

Server config is per-world (with `defaultconfigs/` support), and every server value is pushed to clients on login through NeoForge's config sync: there is no custom packet to manage.

## Enabling and disabling components

Each delivery, effect, and modifier has an `enabled` flag. Disabling a component makes it **unobtainable everywhere**: it disappears from the Spell Builder palette, can't be unlocked by Spell Runes, won't load from saved spells, and won't cast. This lets server admins curate exactly which components are available.

## Costs

Mana cost can be tuned at several levels:

- **`baseManaCost`**: an absolute base cost per component (defaults to the component's built-in cost).
- **`costMultiplier`**: a per-component multiplier.
- **`damageMultiplier`**: a per-effect damage scalar.
- **`globalManaCostMultiplier`**: a single multiplier applied to every spell's total.

## Gameplay tuning

Over 200 individual gameplay numbers are exposed: durations, ranges, damage values, projectile behavior, combo parameters, entity behavior (tornado, black hole, blizzard, lingering areas), and more. Each is keyed by `<id>.<name>` in the relevant category file — combo values by `combo.<combo>.<name>`, and the handful shared across components by `shared.<name>` — with the original in-game value as its default.

A few purely cosmetic details (sound pitches, exact particle types) stay hardcoded, but most gameplay-affecting particle counts and densities are exposed alongside the numbers above.

## See also

- [Spell Components Reference](SPELL_COMPONENTS): the component IDs you'll see as config keys.
