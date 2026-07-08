package buildaspell.spell;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum SpellModifier implements StringRepresentable {
    INCREASED_AREA("increased_area", 15.0f, true),
    INCREASED_POWER("increased_power", 20.0f, true),
    NULLIFY("nullify", 5.0f, false),
    GENTLENESS("gentleness", 10.0f, false),
    FORTUNATE_SON("fortunate_son", 25.0f, true),
    DOUBLE("double", 50.0f, true),
    ECHO("echo", 40.0f, true),
    PROLONGED("prolonged", 18.0f, true),
    DELAY("delay", 8.0f, true),
    CHAIN("chain", 30.0f, true),
    PIERCE("pierce", 20.0f, true),
    BOUNCE("bounce", 25.0f, false),
    ACCELERATE("accelerate", 15.0f, false),
    DURATION("duration", 35.0f, true),
    LINGER("linger", 25.0f, false),
    SPLIT("split", 45.0f, true),
    WALL("wall", 30.0f, false),
    FLOOR("floor", 30.0f, false),
    FILL("fill", 40.0f, false),
    LEECH("leech", 30.0f, true),
    SUNDER("sunder", 25.0f, true),
    RETURN("return", 35.0f, false);

    public static final Codec<SpellModifier> CODEC = StringRepresentable.fromEnum(SpellModifier::values);
    public static final StreamCodec<ByteBuf, SpellModifier> STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8.map(SpellModifier::fromId, SpellModifier::getSerializedName);

    private static final Map<String, SpellModifier> BY_ID = Arrays.stream(values())
            .collect(Collectors.toMap(SpellModifier::getSerializedName, Function.identity()));

    private final String id;
    private final float baseCost;
    private final boolean stackable;

    SpellModifier(String id, float baseCost, boolean stackable) {
        this.id = id;
        this.baseCost = baseCost;
        this.stackable = stackable;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    public float getBaseCost() {
        return baseCost;
    }

    public boolean isStackable() {
        return stackable;
    }

    @Nullable
    public static SpellModifier fromId(String id) {
        return BY_ID.get(id);
    }
}
