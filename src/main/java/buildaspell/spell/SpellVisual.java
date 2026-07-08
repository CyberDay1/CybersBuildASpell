package buildaspell.spell;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Per-spell projectile appearance chosen by the player in the Spell Builder.
 *
 * @param color  packed 0xRRGGBB, or {@link #COLOR_DEFAULT} to fall back to the
 *               effect-derived color used historically by the renderer.
 * @param shape  procedural projectile shape.
 * @param trail  curated trail-particle id (see SpellProjectileEntity for the mapping).
 */
public record SpellVisual(int color, ProjectileShape shape, String trail) {

    /** Sentinel: "no explicit color, use the per-effect default in the renderer". */
    public static final int COLOR_DEFAULT = -1;

    public static final String DEFAULT_TRAIL = "witch";

    public static final SpellVisual DEFAULT =
            new SpellVisual(COLOR_DEFAULT, ProjectileShape.CROSS, DEFAULT_TRAIL);

    public static final Codec<SpellVisual> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.INT.optionalFieldOf("color", COLOR_DEFAULT).forGetter(SpellVisual::color),
            ProjectileShape.CODEC.optionalFieldOf("shape", ProjectileShape.CROSS).forGetter(SpellVisual::shape),
            Codec.STRING.optionalFieldOf("trail", DEFAULT_TRAIL).forGetter(SpellVisual::trail)
    ).apply(inst, SpellVisual::new));

    public static final StreamCodec<ByteBuf, SpellVisual> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, SpellVisual::color,
            ByteBufCodecs.STRING_UTF8, v -> v.shape().getSerializedName(),
            ByteBufCodecs.STRING_UTF8, SpellVisual::trail,
            (color, shapeId, trail) -> new SpellVisual(color, ProjectileShape.fromId(shapeId), trail)
    );

    public boolean hasExplicitColor() {
        return color != COLOR_DEFAULT;
    }
}
