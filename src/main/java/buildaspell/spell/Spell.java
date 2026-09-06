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
    /**
     * Default cap on each of the two lists a spell holds: the effect chain and the delivery's own
     * modifiers. Server owners can move it with the {@code maxSpellComponents} setting in
     * {@code general.toml}, so runtime checks should read {@link #maxComponents()} rather than this
     * constant, which is only the fallback.
     */
    public static final int MAX_COMPONENTS = 100;

    /**
     * The cap actually in force. This is a server setting and syncs to clients on login, so the
     * builder and the save path agree on it.
     */
    public static int maxComponents() {
        return ModConfig.getMaxSpellComponents();
    }

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
        if (components.size() < maxComponents()) {
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
        if (deliveryModifiers.size() >= maxComponents()) return;
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
     * Still capped, but only as a backstop: this feeds a real Fortune and Looting level, and
     * vanilla's ore-drop formula multiplies by up to (level + 1), so the number has to stay finite.
     * What actually limits a stack is the escalating repeat cost - see {@link #getCostBreakdown()}.
     */
    public int getFortuneLevel() {
        int stacks = getModifierCount(SpellModifier.FORTUNATE_SON);
        return Math.min(stacks, ModConfig.modifierInt(SpellModifier.FORTUNATE_SON, "maxLevel", 25));
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
        return getCostBreakdown().total();
    }

    /**
     * What every part of this spell charges, split out piece by piece. {@link #getManaCost()} is
     * simply the sum. The spell builder shows a component's entry in its tooltip, so what a chip
     * claims to cost is the same number the caster is charged for it - a base price would be wrong
     * for every repeat, and for anything a combo has stopped charging for.
     *
     * @param delivery          the delivery method's own cost, or 0 when none is chosen
     * @param components        one entry per {@link #getComponents()} entry, in the same order
     * @param deliveryModifiers one entry per {@link #getDeliveryModifiers()} entry, in the same order
     */
    public record CostBreakdown(float delivery, float[] components, float[] deliveryModifiers) {
        public float total() {
            float sum = delivery;
            for (float part : components) sum += part;
            for (float part : deliveryModifiers) sum += part;
            return sum;
        }
    }

    public CostBreakdown getCostBreakdown() {
        float deliveryCost = delivery != null ? ModConfig.getDeliveryCost(delivery) : 0;
        float[] componentCosts = new float[components.size()];
        // Repeating one effect escalates geometrically: the k-th copy of a given effect costs its
        // normal price times growth^(k-1). Counts are per effect identity, so Damage x3 + Explosion
        // x3 price as two independent series rather than one combined run of six. At growth = 1.0
        // every factor is exactly 1.0 (Math.pow(1.0, n) == 1.0), so the sum collapses back to the
        // old flat per-copy total and a server owner can switch the escalation off cleanly.
        double growth = ModConfig.sharedEffectDouble("repeatCostGrowth", 1.5);
        Map<String, Integer> repeats = new HashMap<>();
        for (int i = 0; i < components.size(); i++) {
            SpellComponent component = components.get(i);
            float base;
            if (component instanceof SpellComponent.Effect effect) {
                base = ModConfig.getEffectCost(effect.effect());
            } else if (component instanceof SpellComponent.DataEffect de) {
                base = dataEffectCost(de.effectId());
            } else {
                // Modifiers are priced in the second pass below. A CompatEffect is priced at
                // nothing on purpose: this mod never executes one - it is a marker another mod
                // reads back off the spell via getCompatEffects() - so there is no cost of ours
                // to charge for it, and no definition here that could carry one.
                continue;
            }
            // Type-qualified key: an enum effect's id is a bare name, a datapack effect's is a
            // namespaced id, so they can never be conflated into the same series.
            int k = repeats.merge(component.type() + "/" + component.id(), 1, Integer::sum);
            componentCosts[i] = base * (float) Math.pow(growth, k - 1);
        }
        // Modifiers escalate the same way, but per effect rather than per spell: piling one modifier
        // onto one effect is what gets expensive, while spreading the same modifier across several
        // effects prices each run on its own. A modifier binds to the effect it follows, so each
        // effect component opens a fresh group; the delivery-level bucket is one further group.
        // If this spell forms a combo that hard-caps a modifier, stacks beyond that cap do nothing
        // and so are charged nothing - and, being uncharged, they do not advance the escalation.
        buildaspell.spell.data.ComboDefinition combo = buildaspell.spell.data.ComboRegistry.detect(this);
        Map<SpellModifier, Integer> caps = combo != null ? combo.modifierCaps() : Map.of();
        double modifierGrowth = ModConfig.sharedModifierDouble("repeatCostGrowth", 1.5);
        Map<SpellModifier, Integer> chargedTotals = new EnumMap<>(SpellModifier.class);
        Map<SpellModifier, Integer> group = new EnumMap<>(SpellModifier.class);
        List<PendingMultiplier> pending = new ArrayList<>();
        for (int i = 0; i < components.size(); i++) {
            if (components.get(i) instanceof SpellComponent.Modifier mod) {
                priceModifierStack(mod.modifier(), componentCosts, i,
                        group, chargedTotals, caps, modifierGrowth, pending);
            } else {
                group.clear();
            }
        }
        float[] deliveryModifierCosts = new float[deliveryModifiers.size()];
        Map<SpellModifier, Integer> deliveryGroup = new EnumMap<>(SpellModifier.class);
        for (int i = 0; i < deliveryModifiers.size(); i++) {
            priceModifierStack(deliveryModifiers.get(i), deliveryModifierCosts, i,
                    deliveryGroup, chargedTotals, caps, modifierGrowth, pending);
        }
        settleMultipliers(deliveryCost, componentCosts, deliveryModifierCosts, pending);
        return new CostBreakdown(deliveryCost, componentCosts, deliveryModifierCosts);
    }

    /**
     * A stack of a modifier that charges a percentage of the whole spell rather than a fixed price,
     * held back until everything else has been priced. {@code costs[index]} is where its charge goes,
     * which is the same slot the flat pass wrote to, so the breakdown keeps one entry per chip.
     */
    private record PendingMultiplier(float[] costs, int index, double multiplier) {}

    /**
     * Charges the modifiers that price themselves as a share of the spell. Double and Echo each
     * re-scale everything the spell does - Double sends a second projectile, Echo re-casts the whole
     * thing - so a flat price made them nearly free on a large spell and most of the bill on a small
     * one. Each stack is charged the difference it makes to the running total, which is what makes
     * a second stack cost more than the first without the repeat escalation touching it: two 80%
     * stacks come to 1.8 x 1.8, not 1.8 + 1.8.
     */
    private static void settleMultipliers(float deliveryCost, float[] componentCosts,
                                          float[] deliveryModifierCosts,
                                          List<PendingMultiplier> pending) {
        if (pending.isEmpty()) return;
        float running = deliveryCost;
        for (float part : componentCosts) running += part;
        for (float part : deliveryModifierCosts) running += part;
        for (PendingMultiplier p : pending) {
            float increment = (float) (running * (p.multiplier() - 1.0));
            p.costs()[p.index()] += increment;
            running += increment;
        }
    }

    /**
     * Prices one stack of a modifier into {@code costs[index]} and records it. {@code group} counts
     * stacks within the effect (or the delivery bucket) currently being priced and drives the
     * escalation exponent; {@code chargedTotals} counts them across the whole spell and is what a
     * combo cap is measured against, so a capped modifier stops costing mana once the cap is met no
     * matter how the stacks are spread. At growth 1.0 every factor is exactly 1.0 and this collapses
     * to a flat per-stack price. A stack that also charges a share of the whole spell is added to
     * {@code pending} for {@link #settleMultipliers} to finish once the flat total is known.
     */
    private static void priceModifierStack(SpellModifier modifier, float[] costs, int index,
                                           Map<SpellModifier, Integer> group,
                                           Map<SpellModifier, Integer> chargedTotals,
                                           Map<SpellModifier, Integer> caps,
                                           double growth,
                                           List<PendingMultiplier> pending) {
        Integer cap = caps.get(modifier);
        int alreadyCharged = chargedTotals.getOrDefault(modifier, 0);
        if (cap != null && alreadyCharged >= cap) {
            costs[index] = 0f;
            return;
        }
        chargedTotals.put(modifier, alreadyCharged + 1);
        int k = group.merge(modifier, 1, Integer::sum);
        costs[index] = ModConfig.getModifierCost(modifier, 1) * (float) Math.pow(growth, k - 1);
        // A modifier that also takes a share of the whole spell cannot be priced yet - the rest of
        // the spell is still being totalled. A capped stack never reaches here, so a combo that caps
        // one of these stops it multiplying just as it stops it charging.
        double multiplier = ModConfig.modifierDouble(
                modifier, "totalCostMultiplier", modifier.getTotalCostMultiplier());
        if (multiplier > 1.0) {
            pending.add(new PendingMultiplier(costs, index, multiplier));
        }
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

    /**
     * Base mana cost for a datapack effect (0 if the effect is unknown here).
     *
     * <p>The definitions themselves are a server-side datapack load, so a player connected to a
     * remote server has none of them and would price every datapack effect at zero while building a
     * spell. What that player does have is the display metadata the server synced for the builder's
     * palette, which carries the cost, so fall back to it. On a server the definitions are present
     * and authoritative, and the synced cache there is empty, so this fallback never fires.
     */
    private static float dataEffectCost(Identifier id) {
        EffectDefinition def = EffectRegistry.get(id);
        if (def != null) {
            return def.display().cost().orElse(0.0).floatValue();
        }
        for (var entry : buildaspell.client.ClientComponentRegistry.effects()) {
            if (entry.id().equals(id)) {
                return entry.display().cost().orElse(0.0).floatValue();
            }
        }
        return 0f;
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
