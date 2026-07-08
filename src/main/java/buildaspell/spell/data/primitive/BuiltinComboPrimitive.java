package buildaspell.spell.data.primitive;

import buildaspell.BuildASpell;
import buildaspell.spell.SpellCombo;
import buildaspell.spell.data.SpellContext;
import buildaspell.spell.data.SpellPrimitive;
import buildaspell.spell.execution.SpellExecutor;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

/**
 * Bridges a built-in {@link SpellCombo} behavior into the primitive system unchanged.
 * Shipped combos reference one of these (param {@code combo} = the combo id), so all 14
 * route through the datapack registry with their bespoke Java bodies intact. Datapack
 * combos that need wholly new behavior compose decomposed primitives instead.
 */
public record BuiltinComboPrimitive(String comboId) implements SpellPrimitive {

    public static final ResourceLocation TYPE =
            ResourceLocation.fromNamespaceAndPath(BuildASpell.MOD_ID, "builtin_combo");

    public static final MapCodec<BuiltinComboPrimitive> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            com.mojang.serialization.Codec.STRING.fieldOf("combo").forGetter(BuiltinComboPrimitive::comboId)
    ).apply(inst, BuiltinComboPrimitive::new));

    @Override
    public ResourceLocation type() {
        return TYPE;
    }

    @Override
    public void execute(SpellContext ctx) {
        SpellCombo combo = SpellCombo.fromId(comboId);
        if (combo == null) {
            BuildASpell.LOGGER.warn("builtin_combo references unknown combo id '{}'", comboId);
            return;
        }
        SpellExecutor.runBuiltinComboBehavior(ctx.caster(), ctx.level(), ctx.origin(), combo, ctx.spell(), ctx.spellPower());
    }
}
