package buildaspell.spell.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Map;

/**
 * Datapack definition of a spell modifier. For now it carries the numeric scalars a
 * modifier contributes to spell stats (e.g. {@code range_per_stack} for Increased Area).
 * Built-in modifiers ship as bundled JSON so a pack can retune these or add new modifiers.
 */
public record ModifierDefinition(Map<String, Double> params, ComponentDisplay display) {

    public static final Codec<ModifierDefinition> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).optionalFieldOf("params", Map.of())
                    .forGetter(ModifierDefinition::params),
            ComponentDisplay.CODEC.optionalFieldOf("display", ComponentDisplay.EMPTY).forGetter(ModifierDefinition::display)
    ).apply(inst, ModifierDefinition::new));

    public double param(String name, double fallback) {
        return params.getOrDefault(name, fallback);
    }
}
