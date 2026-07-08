package buildaspell.spell;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

/**
 * Single source of truth for whether a {@link SpellModifier} actually does anything when attached
 * to a given {@link SpellEffect} under a given {@link DeliveryMethod}. Used by the Spell Builder to
 * stop players from spending mana on modifiers that have no effect on the component they're attached
 * to (e.g. Chain on a Light effect, or Pierce on a non-projectile delivery).
 *
 * <p>The mapping mirrors what each effect executor actually reads at runtime (see EffectExecutors,
 * SpellExecutor, and SpellProjectileEntity). When in doubt this errs toward "useful" so a real
 * interaction is never blocked.
 */
public final class ModifierApplicability {

    private ModifierApplicability() {}

    /**
     * Modifiers that only matter when a projectile is actually spawned (Cast / Tracking delivery).
     * CHAIN is now a projectile-level mechanic (block-hop chaining in SpellProjectileEntity), so it
     * lives here rather than in the per-effect table.
     */
    private static final EnumSet<SpellModifier> PROJECTILE_ONLY = EnumSet.of(
            SpellModifier.DOUBLE, SpellModifier.SPLIT, SpellModifier.ACCELERATE,
            SpellModifier.PIERCE, SpellModifier.BOUNCE, SpellModifier.RETURN, SpellModifier.CHAIN);

    /** Orchestration modifiers that wrap, delay, or repeat any effect group regardless of effect. */
    private static final EnumSet<SpellModifier> UNIVERSAL = EnumSet.of(
            SpellModifier.DURATION, SpellModifier.DELAY, SpellModifier.ECHO, SpellModifier.LINGER);

    /**
     * The delivery-level roster: modifiers that act on the DELIVERY / PROJECTILE as a whole rather
     * than binding to a preceding effect. These are stored in {@link Spell#getDeliveryModifiers()}
     * and read globally via {@code spell.hasModifier(...)}; their position in the component list is
     * meaningless. The remaining modifiers stay effect-bound (see the per-effect table below).
     *
     * <p>This is the single source of truth used by the Spell Builder to route a modifier to the
     * delivery zone instead of the selected effect, and by the load-time migration in {@link Spell}
     * to relocate any roster modifier that a saved spell left interleaved in its components.
     */
    private static final EnumSet<SpellModifier> DELIVERY_LEVEL = EnumSet.of(
            SpellModifier.DOUBLE, SpellModifier.SPLIT, SpellModifier.ACCELERATE,
            SpellModifier.PIERCE, SpellModifier.BOUNCE, SpellModifier.RETURN,
            SpellModifier.CHAIN, SpellModifier.DELAY, SpellModifier.ECHO,
            SpellModifier.LINGER, SpellModifier.DURATION);

    /** @return true if {@code modifier} attaches to the delivery/projectile as a whole (roster). */
    public static boolean isDeliveryLevel(SpellModifier modifier) {
        return DELIVERY_LEVEL.contains(modifier);
    }

    /**
     * @return true if a delivery-level (roster) modifier actually does something under {@code
     *         delivery}. Projectile-only roster modifiers (double/split/accelerate/pierce/bounce/
     *         return/chain) require a projectile delivery; the orchestration ones (delay/echo/linger/
     *         duration) always apply. A null delivery is "not yet chosen" and never blocks.
     */
    public static boolean isDeliveryModifierUseful(SpellModifier modifier, DeliveryMethod delivery) {
        if (PROJECTILE_ONLY.contains(modifier)) {
            return delivery == null || isProjectileDelivery(delivery);
        }
        return true;
    }

    /** Per-effect set of effect-specific modifiers each effect actually consumes. */
    private static final Map<SpellEffect, EnumSet<SpellModifier>> EFFECT_MODIFIERS = build();

    private static final EnumSet<SpellModifier> AREA = EnumSet.of(SpellModifier.INCREASED_AREA);

    public static boolean isProjectileDelivery(DeliveryMethod delivery) {
        return delivery == DeliveryMethod.CAST || delivery == DeliveryMethod.TRACKING;
    }

    /**
     * @return true if {@code modifier} has a gameplay effect when attached to {@code effect} cast via
     *         {@code delivery}. A null delivery is treated as "not yet chosen" and never blocks a
     *         delivery-dependent modifier.
     */
    public static boolean isUseful(SpellModifier modifier, SpellEffect effect, DeliveryMethod delivery) {
        if (UNIVERSAL.contains(modifier)) return true;
        if (PROJECTILE_ONLY.contains(modifier)) {
            return delivery == null || isProjectileDelivery(delivery);
        }
        return EFFECT_MODIFIERS.getOrDefault(effect, AREA).contains(modifier);
    }

    private static EnumSet<SpellModifier> set(SpellModifier... mods) {
        EnumSet<SpellModifier> s = EnumSet.noneOf(SpellModifier.class);
        for (SpellModifier m : mods) s.add(m);
        return s;
    }

    private static Map<SpellEffect, EnumSet<SpellModifier>> build() {
        SpellModifier POWER = SpellModifier.INCREASED_POWER;
        SpellModifier AREA = SpellModifier.INCREASED_AREA;
        SpellModifier NULLIFY = SpellModifier.NULLIFY;
        SpellModifier PROLONGED = SpellModifier.PROLONGED;
        SpellModifier FORTUNE = SpellModifier.FORTUNATE_SON;
        SpellModifier GENTLE = SpellModifier.GENTLENESS;
        SpellModifier WALL = SpellModifier.WALL;
        SpellModifier FLOOR = SpellModifier.FLOOR;
        SpellModifier FILL = SpellModifier.FILL;
        SpellModifier LEECH = SpellModifier.LEECH;
        SpellModifier SUNDER = SpellModifier.SUNDER;

        Map<SpellEffect, EnumSet<SpellModifier>> m = new EnumMap<>(SpellEffect.class);
        m.put(SpellEffect.DAMAGE, set(POWER, AREA, NULLIFY, FORTUNE, LEECH, SUNDER));
        m.put(SpellEffect.IGNITE, set(POWER, AREA, PROLONGED));
        m.put(SpellEffect.FREEZE, set(POWER, AREA, PROLONGED));
        m.put(SpellEffect.TELEPORT, set(AREA)); // Area extends spell range = self-teleport distance
        m.put(SpellEffect.PULL, set(POWER, AREA));
        m.put(SpellEffect.PUSH, set(POWER, AREA));
        m.put(SpellEffect.YEET, set(POWER, AREA));
        m.put(SpellEffect.REAP, set(AREA, FORTUNE, GENTLE));
        m.put(SpellEffect.EXPLOSION, set(POWER, AREA, NULLIFY));
        m.put(SpellEffect.HEAL, set(POWER, AREA, NULLIFY, FORTUNE));
        m.put(SpellEffect.LIGHTNING, set(POWER, AREA, NULLIFY));
        m.put(SpellEffect.POISON, set(POWER, AREA, PROLONGED));
        m.put(SpellEffect.WITHER, set(POWER, AREA, PROLONGED));
        m.put(SpellEffect.SATURATION, set(POWER, AREA, PROLONGED));
        m.put(SpellEffect.LAUNCH, set(POWER, AREA));
        m.put(SpellEffect.LIGHT, set(POWER, AREA));
        m.put(SpellEffect.SLAM, set(POWER, AREA));
        m.put(SpellEffect.LEVITATION, set(POWER, AREA, PROLONGED));
        m.put(SpellEffect.SLOW_FALL, set(POWER, AREA, PROLONGED));
        m.put(SpellEffect.BREAK, set(POWER, AREA, GENTLE, FORTUNE, WALL, FLOOR, FILL));
        m.put(SpellEffect.INVISIBILITY, set(POWER, AREA, PROLONGED));
        m.put(SpellEffect.SPEED, set(POWER, AREA, PROLONGED));
        m.put(SpellEffect.HASTE, set(POWER, AREA, PROLONGED));
        m.put(SpellEffect.BLINK, set(POWER));
        m.put(SpellEffect.SWAP, set(AREA));
        m.put(SpellEffect.SUMMON, set(POWER));
        m.put(SpellEffect.CREATE_WATER, set(AREA, WALL, FLOOR, FILL));
        m.put(SpellEffect.EVAPORATE_WATER, set(AREA, WALL, FLOOR, FILL));
        m.put(SpellEffect.MARK, set());
        m.put(SpellEffect.RECALL, set());
        m.put(SpellEffect.PICKUP, set(AREA));
        m.put(SpellEffect.SHIELD, set(AREA));
        m.put(SpellEffect.CONJURE, set(AREA, WALL, FLOOR, FILL));
        m.put(SpellEffect.GROWTH, set(POWER, AREA, PROLONGED));
        m.put(SpellEffect.CLEANSE, set(AREA));
        m.put(SpellEffect.CHARM, set(POWER, AREA, PROLONGED));
        m.put(SpellEffect.BLIND, set(POWER, AREA, PROLONGED));
        m.put(SpellEffect.SLOW, set(POWER, AREA, PROLONGED));
        m.put(SpellEffect.WEAKEN, set(POWER, AREA, PROLONGED));
        m.put(SpellEffect.STRENGTHEN, set(POWER, AREA, PROLONGED));
        m.put(SpellEffect.REGENERATE, set(POWER, AREA, PROLONGED));
        m.put(SpellEffect.RESIST, set(POWER, AREA, PROLONGED));
        m.put(SpellEffect.NIGHT_VISION, set(AREA, PROLONGED));
        m.put(SpellEffect.WATER_BREATHING, set(AREA, PROLONGED));
        m.put(SpellEffect.ROOT, set(POWER, AREA, PROLONGED));
        m.put(SpellEffect.GRAPPLE, set(POWER));
        m.put(SpellEffect.GUST, set(POWER, AREA));
        return m;
    }
}
