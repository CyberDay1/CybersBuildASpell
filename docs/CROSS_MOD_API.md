---
title: Cross-Mod API
parent: Reference
nav_order: 2
---

# Build a Spell — Cross-Mod Integration Guide

This document describes how other NeoForge mods can interact with Build a Spell's player attribute system to modify mana, spell power, and casting behavior.

## Dependency Setup

Add `buildaspell` as a dependency in your `neoforge.mods.toml`:

```toml
[[dependencies.yourmodid]]
modId = "buildaspell"
type = "required"          # or "optional" if you check at runtime
versionRange = "[1.0,)"
ordering = "AFTER"
side = "BOTH"
```

For compile-time access, add the Build a Spell jar to your `build.gradle`:

```groovy
dependencies {
    compileOnly files("libs/buildaspell-<version>.jar")
}
```

## Casting Spells

`buildaspell.api.BuildASpellAPI` is the stable entry point for building and casting spells from another mod. Its package, class name and method signatures are versioned API and will not be renamed; everything else in the mod (`SpellExecutor`, the attachments, the component enums) is internal and may change without notice. Every method is server-side, and bad input returns `false` or `null` with a log line rather than throwing across the mod boundary.

```java
import buildaspell.api.BuildASpellAPI;
import buildaspell.spell.Spell;

// One ordered list of component ids: a modifier binds to the effect before it.
Spell spell = BuildASpellAPI.createSpell("cast", List.of("damage"));

if (spell != null) {
    // consumeMana = false skips Build a Spell's mana check, so your mod can
    // charge its own resource instead. Config enable/disable gates still apply.
    BuildASpellAPI.cast(serverPlayer, spell, false);

    // Or run the spell's effects at an explicit position, bypassing the delivery's targeting:
    BuildASpellAPI.castAtLocation(serverPlayer, spell, new Vec3(x, y, z), false);
}
```

- `createSpell(deliveryId, componentIds)` takes a delivery id and one interleaved, **ordered** list of component ids: an effect-bound modifier attaches to the effect that precedes it. Delivery-level modifiers (`double`, `split`, `accelerate`, `pierce`, `bounce`, `return`, `chain`, `delay`, `echo`, `linger`, `duration`) attach to the delivery as a whole, so their position in the list does not matter. Each id resolves as a modifier first, then an effect; an id starting with `compat:` is passed through verbatim; anything else fails the whole build with `null` and a log line. The list is capped at the server's `maxSpellComponents` and a non-stackable duplicate modifier is dropped, so an API-built spell can never exceed what a player could assemble by hand.
- `cast(caster, spell, consumeMana)` dispatches the spell through its own delivery method. With `consumeMana` true the cast checks and spends the Build a Spell mana pool; with it false only that check (and its refund-on-failure path) is skipped, and the caller charges whatever resource it likes.
- `castAtLocation(caster, spell, pos, consumeMana)` runs the spell's effects at an explicit world position instead of the delivery's own targeting.
- `deliveryIds()`, `effectIds()` and `modifierIds()` return the live id lists to build from.

### Current mana is deliberately not exposed

The attributes below govern the pool's maximum, its regeneration rate and spell power: the current contents of the pool are not part of the API. A mod that wants to run spells on its own resource system casts with `consumeMana = false` and charges that resource itself.

## Custom Attributes

All three attributes are registered on `EntityType.PLAYER` and live in the `buildaspell` namespace. They are standard vanilla `Attribute`s backed by `RangedAttribute`, so any system that applies `AttributeModifier`s (equipment, potions, mob effects, events) works out of the box.

| Registry ID | Default | Min | Max | Description |
|---|---|---|---|---|
| `buildaspell:mana_pool` | 100.0 | 0.0 | 10,000.0 | Maximum mana capacity |
| `buildaspell:mana_regen` | 5.0 | 0.0 | 1,000.0 | Mana regenerated per second |
| `buildaspell:spell_power` | 10.0 | 0.0 | 1,000.0 | Spell damage and effectiveness scaling |

Enchantment bonuses from Build a Spell's own enchantments are applied **on top of** the attribute value, so attribute modifiers and enchantments stack naturally.

## Accessing Attributes at Compile Time

If you depend on Build a Spell directly, you can reference the `DeferredHolder` fields:

```java
import buildaspell.registry.ModAttributes;

// Read current effective value (includes all modifiers)
double mana = player.getAttributeValue(ModAttributes.MANA_POOL);
double regen = player.getAttributeValue(ModAttributes.MANA_REGEN);
double power = player.getAttributeValue(ModAttributes.SPELL_POWER);
```

## Accessing Attributes by Registry ID (No Hard Dependency)

If you want to keep Build a Spell as an optional dependency, look up the attribute holders at runtime using the registry:

```java
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;

Holder<Attribute> manaPool = BuiltInRegistries.ATTRIBUTE
        .get(Identifier.parse("buildaspell:mana_pool"))
        .orElse(null);

if (manaPool != null) {
    double value = player.getAttributeValue(manaPool);
}
```

## Adding Attribute Modifiers

### Transient Modifiers (Runtime Only)

Transient modifiers are not saved and are removed when the player logs out or the source is cleared. Useful for temporary buffs from events, abilities, or auras.

```java
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

Identifier modifierId = Identifier.parse("yourmod:mana_boost");

AttributeInstance instance = player.getAttribute(ModAttributes.MANA_POOL);
if (instance != null && instance.getModifier(modifierId) == null) {
    instance.addTransientModifier(new AttributeModifier(
            modifierId,
            50.0,                                    // +50 mana
            AttributeModifier.Operation.ADD_VALUE     // flat addition
    ));
}
```

### Permanent Modifiers (Saved with Player)

Permanent modifiers persist across sessions. Useful for progression systems or permanent unlocks.

```java
AttributeInstance instance = player.getAttribute(ModAttributes.MANA_POOL);
if (instance != null && instance.getModifier(modifierId) == null) {
    instance.addPermanentModifier(new AttributeModifier(
            modifierId,
            50.0,
            AttributeModifier.Operation.ADD_VALUE
    ));
}
```

### Removing Modifiers

```java
AttributeInstance instance = player.getAttribute(ModAttributes.MANA_POOL);
if (instance != null) {
    instance.removeModifier(modifierId);
}
```

## Modifier Operations

All three vanilla `AttributeModifier.Operation` types work:

| Operation | Effect | Example |
|---|---|---|
| `ADD_VALUE` | Flat addition to base | +50 mana pool |
| `ADD_MULTIPLIED_BASE` | Multiplied by base after flat additions, then added | +0.2 = +20% of base |
| `ADD_MULTIPLIED_TOTAL` | Multiplies the running total | +0.1 = ×1.1 on the final value |

Evaluation order: `(base + ADD_VALUE) * (1 + sum(ADD_MULTIPLIED_BASE))`, then each `ADD_MULTIPLIED_TOTAL` modifier multiplies the running total by `(1 + amount)` in turn: two +0.1 modifiers give ×1.21, not ×1.2.

## Common Integration Patterns

### Custom Item That Boosts Mana Pool

Apply the modifier when the item is equipped, remove it when unequipped. The standard way is to add it to the item's `AttributeModifiers` component via data or in code:

```java
// In your item's constructor or via data generation
ItemAttributeModifiers modifiers = ItemAttributeModifiers.builder()
        .add(ModAttributes.MANA_POOL,
                new AttributeModifier(
                        Identifier.parse("yourmod:staff_mana"),
                        25.0,
                        AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.MAINHAND)
        .build();
```

### Mob Effect That Increases Spell Power

```java
public class SpellPowerEffect extends MobEffect {
    public SpellPowerEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x7B68EE);
        addAttributeModifier(
                ModAttributes.SPELL_POWER,
                Identifier.parse("yourmod:spell_power_effect"),
                5.0,                                     // +5 per level
                AttributeModifier.Operation.ADD_VALUE
        );
    }
}
```

## Notes

- Modifier IDs must be unique `Identifier`s scoped to your mod (e.g., `yourmod:my_modifier`). Adding a modifier whose ID is already applied to the same attribute instance throws an `IllegalArgumentException`: check `getModifier(id) == null` first, as the examples above do.
- Attribute values are automatically clamped to the min/max range defined by the `RangedAttribute`.
- These attributes are only registered on players. They are not present on other `LivingEntity` types.
- All attribute reads happen server-side. On the client, synced attribute data is available through the same `player.getAttributeValue()` calls.
