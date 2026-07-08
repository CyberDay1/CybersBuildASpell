package buildaspell.spell.data.primitive;

import buildaspell.BuildASpell;
import buildaspell.spell.SpellEffect;
import buildaspell.spell.data.SpellContext;
import buildaspell.spell.data.SpellPrimitive;
import buildaspell.spell.execution.SpellExecutor;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

/**
 * Bridges a built-in {@link SpellEffect} behavior into the primitive system unchanged.
 * This is the low-risk migration path: every shipped effect's JSON references one of
 * these, so all 37 effects route through the datapack registry while their Java bodies
 * stay byte-for-byte identical. Decomposed, fully data-authored primitives are layered
 * on top later without disturbing this bridge.
 */
public record BuiltinEffectPrimitive(SpellEffect effect) implements SpellPrimitive {

    public static final Identifier TYPE =
            Identifier.fromNamespaceAndPath(BuildASpell.MOD_ID, "builtin_effect");

    public static final MapCodec<BuiltinEffectPrimitive> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            SpellEffect.CODEC.fieldOf("effect").forGetter(BuiltinEffectPrimitive::effect)
    ).apply(inst, BuiltinEffectPrimitive::new));

    @Override
    public Identifier type() {
        return TYPE;
    }

    @Override
    public void execute(SpellContext ctx) {
        SpellExecutor.runBuiltinEffectBehavior(ctx.caster(), ctx.level(), ctx.origin(), effect, ctx.spell(), ctx.spellPower(), ctx.stacks());
    }
}
