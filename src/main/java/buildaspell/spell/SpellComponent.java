package buildaspell.spell;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/**
 * A typed spell component - either an effect or a modifier.
 * Replaces the old List<Object> pattern with proper type safety.
 *
 * <p>{@link DataEffect} carries a raw {@link Identifier} for a datapack-authored
 * effect that has no backing enum constant. Because it is stored as a plain id, a saved
 * spell referencing a datapack effect that was later removed still loads cleanly — the id
 * simply resolves to no behavior at cast time (skipped) rather than failing the parse.
 */
public sealed interface SpellComponent permits SpellComponent.Effect, SpellComponent.Modifier, SpellComponent.CompatEffect, SpellComponent.DataEffect {

    String type();
    String id();

    Codec<SpellComponent> CODEC = Codec.STRING.dispatch("type",
            SpellComponent::type,
            type -> switch (type) {
                case "effect" -> Effect.CODEC;
                case "modifier" -> Modifier.CODEC;
                case "compat_effect" -> CompatEffect.CODEC;
                case "data_effect" -> DataEffect.CODEC;
                default -> throw new IllegalArgumentException("Unknown spell component type: " + type);
            }
    );

    StreamCodec<ByteBuf, SpellComponent> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SpellComponent::type,
            ByteBufCodecs.STRING_UTF8, SpellComponent::id,
            (type, id) -> switch (type) {
                case "effect" -> {
                    SpellEffect effect = SpellEffect.fromId(id);
                    yield effect != null ? new Effect(effect) : new Effect(SpellEffect.DAMAGE);
                }
                case "modifier" -> {
                    SpellModifier modifier = SpellModifier.fromId(id);
                    yield modifier != null ? new Modifier(modifier) : new Modifier(SpellModifier.INCREASED_POWER);
                }
                case "compat_effect" -> new CompatEffect(id);
                case "data_effect" -> {
                    Identifier rl = Identifier.tryParse(id);
                    yield rl != null ? new DataEffect(rl) : new CompatEffect(id);
                }
                default -> throw new IllegalArgumentException("Unknown spell component type: " + type);
            }
    );

    record Effect(SpellEffect effect) implements SpellComponent {
        public static final MapCodec<SpellComponent> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                SpellEffect.CODEC.fieldOf("id").forGetter(c -> ((Effect) c).effect)
        ).apply(inst, Effect::new));

        @Override
        public String type() { return "effect"; }

        @Override
        public String id() { return effect.getSerializedName(); }
    }

    record Modifier(SpellModifier modifier) implements SpellComponent {
        public static final MapCodec<SpellComponent> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                SpellModifier.CODEC.fieldOf("id").forGetter(c -> ((Modifier) c).modifier)
        ).apply(inst, Modifier::new));

        @Override
        public String type() { return "modifier"; }

        @Override
        public String id() { return modifier.getSerializedName(); }
    }

    record CompatEffect(String effectId) implements SpellComponent {
        public static final MapCodec<SpellComponent> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                Codec.STRING.fieldOf("id").forGetter(c -> ((CompatEffect) c).effectId)
        ).apply(inst, CompatEffect::new));

        @Override
        public String type() { return "compat_effect"; }

        @Override
        public String id() { return effectId; }
    }

    record DataEffect(Identifier effectId) implements SpellComponent {
        public static final MapCodec<SpellComponent> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                Identifier.CODEC.fieldOf("id").forGetter(c -> ((DataEffect) c).effectId)
        ).apply(inst, DataEffect::new));

        @Override
        public String type() { return "data_effect"; }

        @Override
        public String id() { return effectId.toString(); }
    }
}
