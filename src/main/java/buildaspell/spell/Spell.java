package buildaspell.spell;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import buildaspell.BuildASpell;
import buildaspell.config.ModConfig;
import buildaspell.spell.data.EffectDefinition;
import buildaspell.spell.data.EffectRegistry;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class Spell {
    public static final int MAX_COMPONENTS = 30;

    public static final Codec<Spell> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            DeliveryMethod.CODEC.optionalFieldOf("delivery").forGetter(s -> Optional.ofNullable(s.delivery)),
            SpellComponent.CODEC.listOf().fieldOf("components").forGetter(Spell::getComponents),
            SpellModifier.CODEC.listOf().optionalFieldOf("delivery_modifiers", List.of()).forGetter(Spell::getDeliveryModifiers),
            SpellVisual.CODEC.optionalFieldOf("visual", SpellVisual.DEFAULT).forGetter(Spell::getVisual)
    ).apply(inst, (delivery, components, deliveryModifiers, visual) ->
            new Spell(delivery.orElse(null), components, deliveryModifiers, visual)));

    public static final StreamCodec<ByteBuf, Spell> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(DeliveryMethod.STREAM_CODEC), s -> Optional.ofNullable(s.delivery),
            SpellComponent.STREAM_CODEC.apply(ByteBufCodecs.list()), Spell::getComponents,
            SpellModifier.STREAM_CODEC.apply(ByteBufCodecs.list()), Spell::getDeliveryModifiers,
            SpellVisual.STREAM_CODEC, Spell::getVisual,
            (delivery, components, deliveryModifiers, visual) ->
                    new Spell(delivery.orElse(null), components, deliveryModifiers, visual)
    );

    @Nullable
    private DeliveryMethod delivery;
    private final List<SpellComponent> components;
    /**
     * Delivery-level (roster) modifiers that attach to the delivery/projectile as a whole rather
     * than to a preceding effect (see {@link ModifierApplicability#isDeliveryLevel}). Repeated
     * entries encode a stack count, mirroring how {@link #components} counts effect-bound modifiers.
     */
    private final List<SpellModifier> deliveryModifiers;
    private SpellVisual visual;

    // Cached modifier counts - recomputed on demand
    private transient Map<SpellModifier, Integer> modifierCountCache;

    public Spell() {
        this(null, new ArrayList<>());
    }

    public Spell(@Nullable DeliveryMethod delivery, List<SpellComponent> components) {
        this(delivery, components, SpellVisual.DEFAULT);
    }

    public Spell(@Nullable DeliveryMethod delivery, List<SpellComponent> components, SpellVisual visual) {
        this(delivery, components, List.of(), visual);
    }

    /**
     * Canonical constructor. Runs the load-time migration: any roster (delivery-level) modifier left
     * interleaved in {@code components} by an older save is moved into {@code deliveryModifiers} and
     * removed from the component list, so already-saved spells keep their exact behavior under the
     * new split model. All deserialization entry points (Codec, stream codec, NBT parse, packet
     * rebuild) funnel through here, so migration is applied uniformly.
     */
    public Spell(@Nullable DeliveryMethod delivery, List<SpellComponent> components,
                 List<SpellModifier> deliveryModifiers, SpellVisual visual) {
        this.delivery = delivery;
        this.components = new ArrayList<>();
        this.deliveryModifiers = new ArrayList<>(deliveryModifiers);
        for (SpellComponent c : components) {
            if (c instanceof SpellComponent.Modifier mod && ModifierApplicability.isDeliveryLevel(mod.modifier())) {
                this.deliveryModifiers.add(mod.modifier());
            } else {
                this.components.add(c);
            }
        }
        this.visual = visual != null ? visual : SpellVisual.DEFAULT;
        invalidateCache();
    }

    public SpellVisual getVisual() {
        return visual;
    }

    public void setVisual(SpellVisual visual) {
        this.visual = visual != null ? visual : SpellVisual.DEFAULT;
    }

    @Nullable
    public DeliveryMethod getDelivery() {
        return delivery;
    }

    public void setDelivery(@Nullable DeliveryMethod delivery) {
        this.delivery = delivery;
    }

    public List<SpellComponent> getComponents() {
        return Collections.unmodifiableList(components);
    }

    public void addComponent(SpellComponent component) {
        // Roster (delivery-level) modifiers attach to the delivery as a whole — route them to
        // deliveryModifiers so callers (in-game builder, BuildASpellAPI, importers) stay honest
        // without needing to know the split themselves.
        if (component instanceof SpellComponent.Modifier mod
                && ModifierApplicability.isDeliveryLevel(mod.modifier())) {
            addDeliveryModifier(mod.modifier());
            return;
        }
        if (components.size() < MAX_COMPONENTS) {
            // Enforce non-stackable modifier limit: only allow one instance
            if (component instanceof SpellComponent.Modifier mod && !mod.modifier().isStackable()) {
                if (hasModifier(mod.modifier())) {
                    return;
                }
            }
            components.add(component);
            invalidateCache();
        }
    }

    /** Adds a roster modifier to the delivery, honoring the same non-stackable single-instance rule. */
    public void addDeliveryModifier(SpellModifier modifier) {
        if (deliveryModifiers.size() >= MAX_COMPONENTS) return;
        if (!modifier.isStackable() && deliveryModifiers.contains(modifier)) return;
        deliveryModifiers.add(modifier);
        invalidateCache();
    }

    public List<SpellModifier> getDeliveryModifiers() {
        return Collections.unmodifiableList(deliveryModifiers);
    }

    public void setDeliveryModifiers(List<SpellModifier> newModifiers) {
        deliveryModifiers.clear();
        deliveryModifiers.addAll(newModifiers);
        invalidateCache();
    }

    public void setComponents(List<SpellComponent> newComponents) {
        components.clear();
        // Preserve the split: a caller passing a flat interleaved list still lands roster modifiers
        // in the delivery bucket rather than back in components.
        for (SpellComponent c : newComponents) {
            if (c instanceof SpellComponent.Modifier mod && ModifierApplicability.isDeliveryLevel(mod.modifier())) {
                deliveryModifiers.add(mod.modifier());
            } else {
                components.add(c);
            }
        }
        invalidateCache();
    }

    public void clear() {
        delivery = null;
        components.clear();
        deliveryModifiers.clear();
        visual = SpellVisual.DEFAULT;
        invalidateCache();
    }

    public boolean hasSpell() {
        return delivery != null && hasAnyEffect();
    }

    /** True if the spell has at least one effect of any kind (enum-backed or datapack). */
    public boolean hasAnyEffect() {
        for (SpellComponent c : components) {
            if (c instanceof SpellComponent.Effect || c instanceof SpellComponent.DataEffect) {
                return true;
            }
        }
        return false;
    }

    /** Effect ids in order: enum effects as {@code buildaspell:<name>}, datapack effects verbatim. */
    public List<Identifier> getEffectIds() {
        List<Identifier> ids = new ArrayList<>();
        for (SpellComponent c : components) {
            if (c instanceof SpellComponent.Effect e) {
                ids.add(Identifier.fromNamespaceAndPath(BuildASpell.MOD_ID, e.effect().getSerializedName()));
            } else if (c instanceof SpellComponent.DataEffect de) {
                ids.add(de.effectId());
            }
        }
        return ids;
    }

    public List<SpellEffect> getEffects() {
        return components.stream()
                .filter(c -> c instanceof SpellComponent.Effect)
                .map(c -> ((SpellComponent.Effect) c).effect())
                .toList();
    }

    /**
     * Every modifier on the spell (effect-bound from {@link #components} PLUS delivery-level from
     * {@link #deliveryModifiers}), so total-aggregation callers (mana cost, combo detection) see the
     * full set regardless of which bucket a modifier lives in.
     */
    public List<SpellModifier> getModifiers() {
        List<SpellModifier> all = new ArrayList<>();
        for (SpellComponent c : components) {
            if (c instanceof SpellComponent.Modifier m) all.add(m.modifier());
        }
        all.addAll(deliveryModifiers);
        return all;
    }

    public List<String> getCompatEffects() {
        return components.stream()
                .filter(c -> c instanceof SpellComponent.CompatEffect)
                .map(c -> ((SpellComponent.CompatEffect) c).effectId())
                .toList();
    }

    /**
     * Total stack count per modifier across BOTH buckets: effect-bound modifiers in
     * {@link #components} and delivery-level (roster) modifiers in {@link #deliveryModifiers}.
     * Roster-modifier accessors ({@link #getChainLevel()}, {@link #getDoubleCount()}, etc.) read
     * from here, so they transparently pull from the delivery bucket, and mana-cost aggregation
     * still counts everything.
     */
    public Map<SpellModifier, Integer> getModifierCounts() {
        if (modifierCountCache == null) {
            modifierCountCache = new EnumMap<>(SpellModifier.class);
            for (SpellComponent component : components) {
                if (component instanceof SpellComponent.Modifier mod) {
                    modifierCountCache.merge(mod.modifier(), 1, Integer::sum);
                }
            }
            for (SpellModifier mod : deliveryModifiers) {
                modifierCountCache.merge(mod, 1, Integer::sum);
            }
        }
        return Collections.unmodifiableMap(modifierCountCache);
    }

    public int getModifierCount(SpellModifier modifier) {
        return getModifierCounts().getOrDefault(modifier, 0);
    }

    public boolean hasModifier(SpellModifier modifier) {
        return getModifierCount(modifier) > 0;
    }

    public float getRange() {
        float baseRange = (float) ModConfig.getSpellBaseRange();
        double rangePerStack = buildaspell.spell.data.ModifierRegistry.param(SpellModifier.INCREASED_AREA,
                "range_per_stack", ModConfig.modifierDouble(SpellModifier.INCREASED_AREA, "rangePerStack", 1.0));
        return baseRange + getModifierCount(SpellModifier.INCREASED_AREA) * (float) rangePerStack;
    }

    // Convenience methods for modifier queries used by spell execution
    public int getPowerLevel() { return getModifierCount(SpellModifier.INCREASED_POWER); }
    public int getProlongedLevel() { return getModifierCount(SpellModifier.PROLONGED); }
    public int getChainLevel() { return getModifierCount(SpellModifier.CHAIN); }
    public boolean hasNullify() { return hasModifier(SpellModifier.NULLIFY); }
    public int getDoubleCount() { return getModifierCount(SpellModifier.DOUBLE); }
    public int getSplitLevel() { return getModifierCount(SpellModifier.SPLIT); }
    public int getDelayLevel() { return getModifierCount(SpellModifier.DELAY); }
    public int getDurationLevel() { return getModifierCount(SpellModifier.DURATION); }
    /**
     * Capped on purpose: this feeds a real Fortune enchantment level and a Looting level, and
     * vanilla's ore-drop formula multiplies by up to (level + 1), so an uncapped stack multiplied
     * drops without limit.
     */
    public int getFortuneLevel() {
        int stacks = getModifierCount(SpellModifier.FORTUNATE_SON);
        return Math.min(stacks, ModConfig.modifierInt(SpellModifier.FORTUNATE_SON, "maxLevel", 3));
    }
    public boolean hasGentleness() { return hasModifier(SpellModifier.GENTLENESS); }
    public boolean hasWall() { return hasModifier(SpellModifier.WALL); }
    public boolean hasFloor() { return hasModifier(SpellModifier.FLOOR); }
    public int getFillLevel() { return getModifierCount(SpellModifier.FILL); }
    public int getAccelerateLevel() { return getModifierCount(SpellModifier.ACCELERATE); }
    public int getLeechLevel() { return getModifierCount(SpellModifier.LEECH); }
    public int getSunderLevel() { return getModifierCount(SpellModifier.SUNDER); }
    public boolean hasReturn() { return hasModifier(SpellModifier.RETURN); }

    public float getManaCost() {
        float cost = delivery != null ? ModConfig.getDeliveryCost(delivery) : 0;
        // Repeating one effect escalates geometrically: the k-th copy of a given effect costs its
        // normal price times growth^(k-1). Counts are per effect identity, so Damage x3 + Explosion
        // x3 price as two independent series rather than one combined run of six. At growth = 1.0
        // every factor is exactly 1.0 (Math.pow(1.0, n) == 1.0), so the sum collapses back to the
        // old flat per-copy total and a server owner can switch the escalation off cleanly.
        double growth = ModConfig.sharedEffectDouble("repeatCostGrowth", 1.5);
        Map<String, Integer> repeats = new HashMap<>();
        for (SpellComponent component : components) {
            float base;
            if (component instanceof SpellComponent.Effect effect) {
                base = ModConfig.getEffectCost(effect.effect());
            } else if (component instanceof SpellComponent.DataEffect de) {
                base = dataEffectCost(de.effectId());
            } else {
                continue;
            }
            // Type-qualified key: an enum effect's id is a bare name, a datapack effect's is a
            // namespaced id, so they can never be conflated into the same series.
            int k = repeats.merge(component.type() + "/" + component.id(), 1, Integer::sum);
            cost += base * (float) Math.pow(growth, k - 1);
        }
        // Modifiers are charged per stack, but if this spell forms a combo that hard-caps a
        // modifier, stacks beyond that cap do nothing and so are not charged any mana.
        buildaspell.spell.data.ComboDefinition combo = buildaspell.spell.data.ComboRegistry.detect(this);
        Map<SpellModifier, Integer> caps = combo != null ? combo.modifierCaps() : Map.of();
        for (Map.Entry<SpellModifier, Integer> entry : getModifierCounts().entrySet()) {
            SpellModifier modifier = entry.getKey();
            int count = entry.getValue();
            Integer cap = caps.get(modifier);
            int charged = cap != null ? Math.min(count, cap) : count;
            cost += ModConfig.getModifierCost(modifier, charged);
        }
        return cost;
    }

    public float getBaseCost() {
        float cost = delivery != null ? delivery.getBaseCost() : 0;
        for (SpellComponent component : components) {
            if (component instanceof SpellComponent.Effect effect) {
                cost += effect.effect().getBaseCost();
            } else if (component instanceof SpellComponent.DataEffect de) {
                cost += dataEffectCost(de.effectId());
            } else if (component instanceof SpellComponent.Modifier mod) {
                cost += mod.modifier().getBaseCost();
            }
        }
        // Delivery-level modifiers live in their own bucket; count them too so the base cost is
        // unchanged from before the split.
        for (SpellModifier mod : deliveryModifiers) {
            cost += mod.getBaseCost();
        }
        return cost;
    }

    /** Base mana cost for a datapack effect, taken from its synced display metadata (0 if absent). */
    private static float dataEffectCost(Identifier id) {
        EffectDefinition def = EffectRegistry.get(id);
        return def != null ? def.display().cost().orElse(0.0).floatValue() : 0f;
    }

    private void invalidateCache() {
        modifierCountCache = null;
    }

    public Spell copy() {
        return new Spell(delivery, new ArrayList<>(components), new ArrayList<>(deliveryModifiers), visual);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Spell{delivery=").append(delivery != null ? delivery.getSerializedName() : "none");
        sb.append(", components=[");
        for (int i = 0; i < components.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(components.get(i).id());
        }
        sb.append("], deliveryModifiers=[");
        for (int i = 0; i < deliveryModifiers.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(deliveryModifiers.get(i).getSerializedName());
        }
        sb.append("]}");
        return sb.toString();
    }
}
