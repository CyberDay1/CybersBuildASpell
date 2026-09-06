---
title: Home
layout: home
nav_order: 1
permalink: /
---

# Build a Spell
{: .fs-9 }

A component-based spell creation system for NeoForge: design your own spells from delivery methods, effects, and modifiers, unlock them through a rune-and-essence progression, and discover powerful multi-component combos.
{: .fs-5 .fw-300 }

[Get Started](getting-started){: .btn .btn-primary .fs-5 .mb-4 .mb-md-0 .mr-2 }
[Spell Components](SPELL_COMPONENTS){: .btn .fs-5 .mb-4 .mb-md-0 .mr-2 }
[View on GitHub](https://github.com/CyberDay1/CybersBuildASpell){: .btn .fs-5 .mb-4 .mb-md-0 }

---

## Start here

- **New players**: read [Getting Started](getting-started) to install the mod, open the Spell Builder, and cast your first spell.
- **Spell crafters**: browse the [Spell Components Reference](SPELL_COMPONENTS) for every delivery, effect, modifier, and combo.
- **Server admins**: see [Configuration](configuration) for the per-category config files and what you can tune or disable.
- **Mod developers**: the [Cross-Mod API](CROSS_MOD_API) shows how to read and modify mana, spell power, and casting from another mod.

## Reference

| | |
|---|---|
| [Spell Components](SPELL_COMPONENTS) | Every delivery method, effect, modifier, and combo: costs, formulas, interactions. |
| [Cross-Mod API](CROSS_MOD_API) | Read/modify the attribute system and cast spells from another mod. |
| [Configuration](configuration) | Per-category config files; tuning and disabling components. |

## How spells work

Spells are assembled from three kinds of components:

- **Delivery method**: how the spell is cast (self, line of sight, projectile, homing projectile, touch, a timed ground rune, or a proximity trap).
- **Effects**: what the spell does (damage, status effects, terrain manipulation, summons, utility).
- **Modifiers**: how the effects behave (more power, larger area, longer duration, chaining, piercing, and shape modifiers).

Mana cost is the sum of every component, discounted a little by a held wand, with repeats priced higher each time: a second copy of an effect, or a second stack of a modifier on the same effect, costs more than the first. Double and Echo are the exception to the sum: each is charged a share of what the rest of the spell costs, because each one re-scales everything the spell does rather than adding a fixed amount to it. Certain combinations form **combos** that override their individual behavior to produce a single, more powerful spell: black holes, tornadoes, Void Rifts (dialable, linkable portals), summon swarms, and more.

## Project

- Source: [github.com/CyberDay1/CybersBuildASpell](https://github.com/CyberDay1/CybersBuildASpell)
- Releases & changelogs: [GitHub Releases](https://github.com/CyberDay1/CybersBuildASpell/releases)
- Bugs & requests: [GitHub Issues](https://github.com/CyberDay1/CybersBuildASpell/issues)
