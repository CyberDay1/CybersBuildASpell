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
            SpellVisual.CODEC.optionalFieldOf("visual", SpellVisual.DEFAULT).forGetter(Spell::getVisual)
    ).apply(inst, (delivery, components, visual) -> new Spell(delivery.orElse(null), components, visual)));

    public static final StreamCodec<ByteBuf, Spell> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(DeliveryMethod.STREAM_CODEC), s -> Optional.ofNullable(s.delivery),
            SpellComponent.STREAM_CODEC.apply(ByteBufCodecs.list()), Spell::getComponents,
            SpellVisual.STREAM_CODEC, Spell::getVisual,
            (delivery, components, visual) -> new Spell(delivery.orElse(null), components, visual)
    );

    @Nullable
    private DeliveryMethod delivery;
    private final List<SpellComponent> components;
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
        this.delivery = delivery;
        this.components = new ArrayList<>(components);
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

    public void setComponents(List<SpellComponent> newComponents) {
        components.clear();
        components.addAll(newComponents);
        invalidateCache();
    }

    public void clear() {
        delivery = null;
        components.clear();
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

    public List<SpellModifier> getModifiers() {
        return components.stream()
                .filter(c -> c instanceof SpellComponent.Modifier)
                .map(c -> ((SpellComponent.Modifier) c).modifier())
                .toList();
    }

    public List<String> getCompatEffects() {
        return components.stream()
                .filter(c -> c instanceof SpellComponent.CompatEffect)
                .map(c -> ((SpellComponent.CompatEffect) c).effectId())
                .toList();
    }

    public Map<SpellModifier, Integer> getModifierCounts() {
        if (modifierCountCache == null) {
            modifierCountCache = new EnumMap<>(SpellModifier.class);
            for (SpellComponent component : components) {
                if (component instanceof SpellComponent.Modifier mod) {
                    modifierCountCache.merge(mod.modifier(), 1, Integer::sum);
                }
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
    public int getFortuneLevel() { return getModifierCount(SpellModifier.FORTUNATE_SON); }
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
        for (SpellComponent component : components) {
            if (component instanceof SpellComponent.Effect effect) {
                cost += ModConfig.getEffectCost(effect.effect());
            } else if (component instanceof SpellComponent.DataEffect de) {
                cost += dataEffectCost(de.effectId());
            }
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
        return new Spell(delivery, new ArrayList<>(components), visual);
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
        sb.append("]}");
        return sb.toString();
    }
}
