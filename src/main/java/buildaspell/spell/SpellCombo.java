package buildaspell.spell;

import org.jetbrains.annotations.Nullable;

import java.util.*;

public enum SpellCombo {
    BLACK_HOLE("black_hole",
            Set.of(SpellEffect.PULL, SpellEffect.TELEPORT),
            Map.of(SpellModifier.INCREASED_AREA, 2),
            4),
    TORNADO("tornado",
            Set.of(SpellEffect.PULL, SpellEffect.LAUNCH),
            Map.of(SpellModifier.INCREASED_AREA, 1),
            4),
    CREATIVE_FLIGHT("creative_flight",
            Set.of(SpellEffect.LAUNCH, SpellEffect.LEVITATION, SpellEffect.SLOW_FALL),
            Map.of(),
            3),
    IRON_GOLEM("iron_golem",
            Set.of(SpellEffect.SUMMON, SpellEffect.HEAL),
            Map.of(SpellModifier.INCREASED_POWER, 1),
            3),
    VEXES("vexes",
            Set.of(SpellEffect.SUMMON, SpellEffect.TELEPORT, SpellEffect.DAMAGE),
            Map.of(),
            3),
    SKELETONS("skeletons",
            Set.of(SpellEffect.SUMMON, SpellEffect.LIGHTNING),
            Map.of(SpellModifier.INCREASED_POWER, 1),
            3),
    VINDICATORS("vindicators",
            Set.of(SpellEffect.SUMMON, SpellEffect.DAMAGE),
            Map.of(SpellModifier.INCREASED_POWER, 2, SpellModifier.INCREASED_AREA, 1),
            4),
    VOID_RIFT("void_rift",
            Set.of(SpellEffect.TELEPORT),
            Map.of(SpellModifier.DURATION, 1, SpellModifier.INCREASED_AREA, 1),
            3),
    FORTRESS("fortress",
            Set.of(SpellEffect.CONJURE),
            Map.of(SpellModifier.INCREASED_AREA, 1, SpellModifier.DURATION, 1),
            2),
    FLOOD("flood",
            Set.of(SpellEffect.CREATE_WATER),
            Map.of(SpellModifier.INCREASED_AREA, 1, SpellModifier.CHAIN, 1),
            4),
    FLOOD_LAVA("flood_lava",
            Set.of(SpellEffect.CREATE_WATER, SpellEffect.IGNITE),
            Map.of(SpellModifier.INCREASED_AREA, 1, SpellModifier.CHAIN, 1),
            6),
    EMERGENCY_ESCAPE("emergency_escape",
            Set.of(SpellEffect.BLINK, SpellEffect.RECALL, SpellEffect.TELEPORT),
            Map.of(),
            3),
    METEOR_STRIKE("meteor_strike",
            Set.of(SpellEffect.LAUNCH, SpellEffect.EXPLOSION, SpellEffect.IGNITE),
            Map.of(SpellModifier.INCREASED_POWER, 2),
            5,
            Map.of(SpellModifier.INCREASED_POWER, 2)),
    BLIZZARD("blizzard",
            Set.of(SpellEffect.FREEZE, SpellEffect.PULL),
            Map.of(SpellModifier.INCREASED_AREA, 2, SpellModifier.DURATION, 1),
            5),
    LIGHTNING_STORM("lightning_storm",
            Set.of(SpellEffect.LIGHTNING),
            Map.of(SpellModifier.INCREASED_AREA, 1, SpellModifier.DURATION, 1),
            3),
    EARTHQUAKE("earthquake",
            Set.of(SpellEffect.SLAM, SpellEffect.EXPLOSION),
            Map.of(SpellModifier.INCREASED_AREA, 1),
            3),
    SANCTUARY("sanctuary",
            Set.of(SpellEffect.HEAL),
            Map.of(SpellModifier.INCREASED_AREA, 1, SpellModifier.DURATION, 1),
            3),
    FIRESTORM("firestorm",
            Set.of(SpellEffect.IGNITE, SpellEffect.EXPLOSION),
            Map.of(SpellModifier.INCREASED_AREA, 1),
            3),
    GEYSER("geyser",
            Set.of(SpellEffect.CREATE_WATER, SpellEffect.LAUNCH),
            Map.of(SpellModifier.INCREASED_AREA, 1),
            3);

    private final String id;
    private final Set<SpellEffect> requiredEffects;
    private final Map<SpellModifier, Integer> requiredModifiers;
    private final int minComponents;
    // Per-modifier hard caps: stacks beyond these provide no extra effect, so they are not
    // charged mana (see Spell.getManaCost). Empty when a combo has no diminishing modifiers.
    private final Map<SpellModifier, Integer> modifierCaps;

    SpellCombo(String id, Set<SpellEffect> requiredEffects, Map<SpellModifier, Integer> requiredModifiers, int minComponents) {
        this(id, requiredEffects, requiredModifiers, minComponents, Map.of());
    }

    SpellCombo(String id, Set<SpellEffect> requiredEffects, Map<SpellModifier, Integer> requiredModifiers, int minComponents, Map<SpellModifier, Integer> modifierCaps) {
        this.id = id;
        this.requiredEffects = requiredEffects;
        this.requiredModifiers = requiredModifiers;
        this.minComponents = minComponents;
        this.modifierCaps = modifierCaps;
    }

    public String getId() { return id; }
    public Set<SpellEffect> getRequiredEffects() { return requiredEffects; }
    public Map<SpellModifier, Integer> getRequiredModifiers() { return requiredModifiers; }
    public int getMinComponents() { return minComponents; }
    public Map<SpellModifier, Integer> getModifierCaps() { return modifierCaps; }

    public boolean matches(Spell spell) {
        List<SpellEffect> effects = spell.getEffects();
        Map<SpellModifier, Integer> modCounts = spell.getModifierCounts();
        int totalComponents = spell.getComponents().size();

        if (totalComponents < minComponents) return false;

        // Effects must exactly match — no extra effects allowed to prevent accidental triggers
        Set<SpellEffect> effectSet = new HashSet<>(effects);
        if (!effectSet.equals(requiredEffects)) return false;

        for (Map.Entry<SpellModifier, Integer> entry : requiredModifiers.entrySet()) {
            if (modCounts.getOrDefault(entry.getKey(), 0) < entry.getValue()) return false;
        }

        return true;
    }

    @Nullable
    public static SpellCombo detect(Spell spell) {
        for (SpellCombo combo : values()) {
            if (combo.matches(spell)) return combo;
        }
        return null;
    }

    @Nullable
    public static SpellCombo fromId(String id) {
        for (SpellCombo combo : values()) {
            if (combo.id.equals(id)) return combo;
        }
        return null;
    }
}
