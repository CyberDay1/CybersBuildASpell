package buildaspell.spell.data;

import buildaspell.spell.Spell;
import buildaspell.spell.SpellCombo;
import buildaspell.spell.SpellEffect;
import buildaspell.spell.SpellModifier;
import buildaspell.spell.data.primitive.BuiltinComboPrimitive;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Datapack definition of a spell combo: the match requirements (exact effect set, minimum
 * components, required modifier counts), the per-modifier hard caps used for mana refunds,
 * and the behavior to run. Built-in combos ship as bundled JSON in this format, so a pack
 * can retune a combo's requirements/caps or author a brand-new combo identically.
 */
public record ComboDefinition(Set<SpellEffect> requiredEffects,
                              Map<SpellModifier, Integer> requiredModifiers,
                              int minComponents,
                              Map<SpellModifier, Integer> modifierCaps,
                              List<SpellPrimitive> behavior) {

    public static final Codec<ComboDefinition> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            SpellEffect.CODEC.listOf().fieldOf("required_effects")
                    .forGetter(d -> List.copyOf(d.requiredEffects)),
            Codec.unboundedMap(SpellModifier.CODEC, Codec.INT).optionalFieldOf("required_modifiers", Map.of())
                    .forGetter(ComboDefinition::requiredModifiers),
            Codec.INT.fieldOf("min_components").forGetter(ComboDefinition::minComponents),
            Codec.unboundedMap(SpellModifier.CODEC, Codec.INT).optionalFieldOf("modifier_caps", Map.of())
                    .forGetter(ComboDefinition::modifierCaps),
            SpellPrimitive.CODEC.listOf().fieldOf("behavior").forGetter(ComboDefinition::behavior)
    ).apply(inst, (effects, mods, min, caps, behavior) ->
            new ComboDefinition(new HashSet<>(effects), mods, min, caps, behavior)));

    /** Same matching contract as the legacy {@link SpellCombo#matches}: exact effect set, min size, required modifier counts. */
    public boolean matches(Spell spell) {
        if (spell.getComponents().size() < minComponents) return false;
        if (!new HashSet<>(spell.getEffects()).equals(requiredEffects)) return false;
        Map<SpellModifier, Integer> counts = spell.getModifierCounts();
        for (Map.Entry<SpellModifier, Integer> req : requiredModifiers.entrySet()) {
            if (counts.getOrDefault(req.getKey(), 0) < req.getValue()) return false;
        }
        return true;
    }

    public void run(SpellContext ctx) {
        for (SpellPrimitive primitive : behavior) {
            primitive.execute(ctx);
        }
    }

    /** Wraps a built-in enum combo as a definition (used as a client-side fallback before registry sync). */
    public static ComboDefinition fromBuiltin(SpellCombo combo) {
        return new ComboDefinition(combo.getRequiredEffects(), combo.getRequiredModifiers(),
                combo.getMinComponents(), combo.getModifierCaps(),
                List.of(new BuiltinComboPrimitive(combo.getId())));
    }
}
