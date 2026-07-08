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

For compile-time access, add the Cyber's Build A Spell jar to your `build.gradle`:

```groovy
dependencies {
    compileOnly files("libs/buildaspell-<version>.jar")
}
```

## Custom Attributes

All three attributes are registered on `EntityType.PLAYER` and live in the `buildaspell` namespace. They are standard vanilla `Attribute`s backed by `RangedAttribute`, so any system that applies `AttributeModifier`s (equipment, potions, mob effects, events) works out of the box.

| Registry ID | Default | Min | Max | Description |
|---|---|---|---|---|
| `buildaspell:mana_pool` | 100.0 | 0.0 | 10,000.0 | Maximum mana capacity |
| `buildaspell:mana_regen` | 5.0 | 0.0 | 1,000.0 | Mana regenerated per tick cycle |
| `buildaspell:spell_power` | 10.0 | 0.0 | 1,000.0 | Spell damage and effectiveness scaling |

Enchantment bonuses from Cyber's Build A Spell's own enchantments are applied **on top of** the attribute value, so attribute modifiers and enchantments stack naturally.

## Accessing Attributes at Compile Time

If you depend on Cyber's Build A Spell directly, you can reference the `DeferredHolder` fields:

```java
import buildaspell.registry.ModAttributes;

// Read current effective value (includes all modifiers)
double mana = player.getAttributeValue(ModAttributes.MANA_POOL);
double regen = player.getAttributeValue(ModAttributes.MANA_REGEN);
double power = player.getAttributeValue(ModAttributes.SPELL_POWER);
```

## Accessing Attributes by Registry ID (No Hard Dependency)

If you want to keep Cyber's Build A Spell as an optional dependency, look up the attribute holders at runtime using the registry:

```java
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;

Holder<Attribute> manaPool = BuiltInRegistries.ATTRIBUTE
        .getHolder(ResourceLocation.parse("buildaspell:mana_pool"))
        .orElse(null);

if (manaPool != null) {
    double value = player.getAttributeValue(manaPool);
}
```

## Adding Attribute Modifiers

### Transient Modifiers (Runtime Only)

Transient modifiers are not saved and are removed when the player logs out or the source is cleared. Useful for temporary buffs from events, abilities, or auras.

```java
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

ResourceLocation modifierId = ResourceLocation.parse("yourmod:mana_boost");

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
| `ADD_MULTIPLIED_BASE` | Multiplied by base, then added | +0.2 = +20% of base |
| `ADD_MULTIPLIED_TOTAL` | Multiplied by total after all other ops | +0.1 = +10% of final |

Evaluation order: `(base + ADD_VALUE) * (1 + sum(ADD_MULTIPLIED_BASE)) * (1 + sum(ADD_MULTIPLIED_TOTAL))`

## Common Integration Patterns

### Custom Item That Boosts Mana Pool

Apply the modifier when the item is equipped, remove it when unequipped. The standard way is to add it to the item's `AttributeModifiers` component via data or in code:

```java
// In your item's constructor or via data generation
ItemAttributeModifiers modifiers = ItemAttributeModifiers.builder()
        .add(ModAttributes.MANA_POOL,
                new AttributeModifier(
                        ResourceLocation.parse("yourmod:staff_mana"),
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
                ResourceLocation.parse("yourmod:spell_power_effect"),
                5.0,                                     // +5 per level
                AttributeModifier.Operation.ADD_VALUE
        );
    }
}
```

## Notes

- Modifier IDs must be unique `ResourceLocation`s scoped to your mod (e.g., `yourmod:my_modifier`). Adding a modifier with a duplicate ID to the same attribute instance is a no-op.
- Attribute values are automatically clamped to the min/max range defined by the `RangedAttribute`.
- These attributes are only registered on players. They are not present on other `LivingEntity` types.
- All attribute reads happen server-side. On the client, synced attribute data is available through the same `player.getAttributeValue()` calls.
