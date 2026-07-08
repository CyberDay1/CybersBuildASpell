package buildaspell.spell.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * A single unit of spell behavior. Built-in effects, modifiers, deliveries and combos
 * are all expressed as ordered lists of primitives in datapack JSON, so a pack can
 * compose brand-new components from the same building blocks the mod ships with.
 *
 * <p>Each implementation registers a {@link MapCodec} under a {@link Identifier}
 * type id via {@link #register}; the dispatch {@link #CODEC} then (de)serializes any
 * {@code {"type": "<id>", ...params}} object.
 */
public interface SpellPrimitive {

    /** Run this primitive against the given cast context (server side). */
    void execute(SpellContext ctx);

    /** The registered type id used to look up this primitive's codec. */
    Identifier type();

    /** type id -> param codec. Populated at mod init by {@link SpellPrimitives#bootstrap()}. */
    Map<Identifier, MapCodec<? extends SpellPrimitive>> TYPES = new HashMap<>();

    static void register(Identifier id, MapCodec<? extends SpellPrimitive> codec) {
        TYPES.put(id, codec);
    }

    Codec<SpellPrimitive> CODEC = Identifier.CODEC.dispatch(
            "type",
            SpellPrimitive::type,
            TYPES::get);
}
