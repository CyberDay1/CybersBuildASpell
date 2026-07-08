package buildaspell.spell.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/**
 * Datapack definition of a spell effect: an ordered list of {@link SpellPrimitive}
 * behaviors. Built-in effects ship as bundled JSON in this same format, so a datapack
 * can override a shipped effect or author a brand-new one identically.
 */
public record EffectDefinition(List<SpellPrimitive> behavior, ComponentDisplay display) {

    public static final Codec<EffectDefinition> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            SpellPrimitive.CODEC.listOf().fieldOf("behavior").forGetter(EffectDefinition::behavior),
            ComponentDisplay.CODEC.optionalFieldOf("display", ComponentDisplay.EMPTY).forGetter(EffectDefinition::display)
    ).apply(inst, EffectDefinition::new));

    public void run(SpellContext ctx) {
        for (SpellPrimitive primitive : behavior) {
            primitive.execute(ctx);
        }
    }
}
