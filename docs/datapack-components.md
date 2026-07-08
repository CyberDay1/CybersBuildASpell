---
title: Datapack Components
parent: Guides
nav_order: 3
---

# Custom Components via Datapacks

Build a Spell loads its spell **effects, modifiers, delivery methods, and combos**
from datapacks. Every built-in component ships as bundled JSON in exactly the
format described here, so a datapack can **retune a built-in** or **author a
brand-new component** with the same files the mod uses itself.

Component data is **server-authoritative**: the server loads the definitions and
syncs the display metadata (icon, colour, cost, name) of any datapack-authored
effects down to each client so they appear in the Spell Builder. Actual behaviour
always runs on the server.

{: .note }
Editing a component's behaviour requires a server reload (`/reload`) or a restart.
Removing a datapack that defined a component you've already used in a saved spell
is safe: the unknown id is skipped (and logged) rather than crashing the save.

## File layout

Place definitions under your namespace in one of the four component folders:

```
data/<namespace>/spell_effects/<id>.json
data/<namespace>/spell_modifiers/<id>.json
data/<namespace>/spell_deliveries/<id>.json
data/<namespace>/spell_combos/<id>.json
```

The file name (minus `.json`) is the component's id. To **override a built-in**,
reuse its namespace and id (for example `data/buildaspell/spell_effects/damage.json`).
To **add a new component**, use your own namespace
(for example `data/mypack/spell_effects/frost_nova.json`).

## Behaviour primitives

A component's behaviour is an ordered list of **primitives**: small, typed units
the mod knows how to execute. Each entry is dispatched on its `type` field:

```json
{ "type": "buildaspell:builtin_effect", "effect": "damage" }
```

Two primitive types ship today:

| Type | Param | Does |
|---|---|---|
| `buildaspell:builtin_effect` | `effect` — a built-in effect id | Runs that built-in effect's behaviour unchanged |
| `buildaspell:builtin_combo` | `combo` — a built-in combo id | Runs that built-in combo's behaviour unchanged |

These bridges are how every shipped component routes through the datapack system
while its Java behaviour stays identical. You author a **new** effect today by
**composing existing built-in behaviours**: the list runs top to bottom.

{: .note }
Fully decomposed primitives (standalone `area_damage`, `apply_mob_effect`,
`knockback`, `summon`, `particles`, `sound`, …) are planned, which will let new
components be built from scratch rather than only by reusing built-in behaviours.

## Spell effects

`spell_effects/<id>.json`: a `behavior` list (required) and an optional `display`
block.

A built-in effect simply references itself:

```json
{
  "behavior": [
    { "type": "buildaspell:builtin_effect", "effect": "damage" }
  ]
}
```

A new effect composes built-in behaviours and supplies display metadata so the
builder can render and price it:

```json
{
  "behavior": [
    { "type": "buildaspell:builtin_effect", "effect": "damage" },
    { "type": "buildaspell:builtin_effect", "effect": "ignite" }
  ],
  "display": {
    "icon": "minecraft:blaze_powder",
    "color": -3407872,
    "cost": 25.0,
    "name": "effect.mypack.smite"
  }
}
```

Datapack-authored effects (those whose id is not a built-in) are **always
available** in the Spell Builder: they bypass the rune-unlock progression
that gates the built-in effects.

## Modifiers and delivery methods

`spell_modifiers/<id>.json` and `spell_deliveries/<id>.json` share the same shape:
an optional `params` map of numeric scalars and an optional `display` block.

```json
{
  "params": { "range_per_stack": 1.0 },
  "display": { "icon": "minecraft:firework_star", "cost": 15.0 }
}
```

`params` carries the numbers a modifier or delivery feeds into spell stats: for
example `range_per_stack` for an area modifier. An empty file (`{}`) is valid and
falls back to the mod's configured values. Most built-ins ship as `{}` and read
their tuning from the server config.

{: .note }
Behaviour for brand-new modifiers and deliveries is not yet data-authored: only
their stats (`params`) and display are. Built-in modifier/delivery behaviour can
be retuned via `params`/config, but creating a wholly new modifier behaviour still
requires the planned primitive set.

## Combos

`spell_combos/<id>.json` defines a combo's match requirements and behaviour:

| Field | Required | Meaning |
|---|---|---|
| `required_effects` | yes | The exact set of effect ids the spell must contain |
| `min_components` | yes | Minimum total component count to qualify |
| `behavior` | yes | Primitives to run when the combo matches |
| `required_modifiers` | no | Minimum count of each named modifier (default none) |
| `modifier_caps` | no | Per-modifier hard caps used for mana refunds (default none) |

```json
{
  "required_effects": ["<effect_id>", "<effect_id>"],
  "min_components": 3,
  "required_modifiers": { "<modifier_id>": 1 },
  "modifier_caps": { "<modifier_id>": 2 },
  "behavior": [
    { "type": "buildaspell:builtin_combo", "combo": "<combo_id>" }
  ]
}
```

Combos are matched against the spell's exact effect set, so order doesn't matter
but the set must match precisely. When several combos could match, they are
evaluated in id order and the first match wins.

{: .note }
The recipes for the **built-in** combos are intentionally left undocumented:
discovering them is part of the game. The format above is everything you need to
author your own; fill in your own effect, modifier, and combo ids.

## The display block

Every `display` field is optional; omit any you don't need.

| Field | Type | Falls back to |
|---|---|---|
| `icon` | item id | `minecraft:paper` |
| `color` | ARGB integer | the per-type accent colour |
| `cost` | number (base mana) | `0` |
| `name` | translation key | the component id |

## Applying a datapack

Drop the pack into a world's `datapacks/` folder (or use a global datapack loader
such as OpenLoader to apply it to every world), then run `/reload`. On reload the
server reparses the definitions and resyncs datapack effects to connected clients;
the new or changed components appear in the Spell Builder immediately.
