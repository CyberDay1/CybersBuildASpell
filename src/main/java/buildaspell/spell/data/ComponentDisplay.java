package buildaspell.spell.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.Optional;

/**
 * Optional client-facing display metadata for a datapack-defined component. Built-in
 * components leave this empty and fall back to the enum-driven display (icon/cost/name);
 * datapack-authored components supply it so the spell builder can render and price them.
 *
 * <ul>
 *   <li>{@code icon} – item id rendered as the palette sprite (falls back to paper).</li>
 *   <li>{@code color} – ARGB accent override (falls back to the per-type accent).</li>
 *   <li>{@code cost} – base mana cost (falls back to 0).</li>
 *   <li>{@code name} – translation key for the display name (falls back to the id).</li>
 * </ul>
 */
public record ComponentDisplay(Optional<Identifier> icon, Optional<Integer> color,
                               Optional<Double> cost, Optional<String> name) {

    public static final ComponentDisplay EMPTY =
            new ComponentDisplay(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());

    public static final Codec<ComponentDisplay> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Identifier.CODEC.optionalFieldOf("icon").forGetter(ComponentDisplay::icon),
            Codec.INT.optionalFieldOf("color").forGetter(ComponentDisplay::color),
            Codec.DOUBLE.optionalFieldOf("cost").forGetter(ComponentDisplay::cost),
            Codec.STRING.optionalFieldOf("name").forGetter(ComponentDisplay::name)
    ).apply(inst, ComponentDisplay::new));

    /** Network form: only display data crosses the wire (behavior stays server-side). */
    public static final StreamCodec<ByteBuf, ComponentDisplay> STREAM_CODEC = StreamCodec.of(
            (buf, d) -> {
                ByteBufCodecs.optional(Identifier.STREAM_CODEC).encode(buf, d.icon);
                ByteBufCodecs.optional(ByteBufCodecs.INT).encode(buf, d.color);
                ByteBufCodecs.optional(ByteBufCodecs.DOUBLE).encode(buf, d.cost);
                ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8).encode(buf, d.name);
            },
            buf -> new ComponentDisplay(
                    ByteBufCodecs.optional(Identifier.STREAM_CODEC).decode(buf),
                    ByteBufCodecs.optional(ByteBufCodecs.INT).decode(buf),
                    ByteBufCodecs.optional(ByteBufCodecs.DOUBLE).decode(buf),
                    ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8).decode(buf)
            )
    );
}
