package buildaspell.spell.data;

import buildaspell.spell.data.primitive.BuiltinComboPrimitive;
import buildaspell.spell.data.primitive.BuiltinEffectPrimitive;

/**
 * Registers all code-side {@link SpellPrimitive} types. Call {@link #bootstrap()} once
 * during mod construction, before any datapack JSON is parsed.
 */
public final class SpellPrimitives {
    private SpellPrimitives() {}

    public static void bootstrap() {
        SpellPrimitive.register(BuiltinEffectPrimitive.TYPE, BuiltinEffectPrimitive.CODEC);
        SpellPrimitive.register(BuiltinComboPrimitive.TYPE, BuiltinComboPrimitive.CODEC);
    }
}
