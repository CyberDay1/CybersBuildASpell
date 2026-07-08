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

public enum DeliveryMethod implements StringRepresentable {
    RUNE("rune", 20.0f),
    SIGHT("sight", 15.0f),
    SELF("self", 5.0f),
    CAST("cast", 25.0f),
    TRACKING("tracking", 35.0f),
    TOUCH("touch", 15.0f),
    TRAP("trap", 25.0f);

    public static final Codec<DeliveryMethod> CODEC = StringRepresentable.fromEnum(DeliveryMethod::values);
    public static final StreamCodec<ByteBuf, DeliveryMethod> STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8.map(DeliveryMethod::fromId, DeliveryMethod::getSerializedName);

    private static final Map<String, DeliveryMethod> BY_ID = Arrays.stream(values())
            .collect(Collectors.toMap(DeliveryMethod::getSerializedName, Function.identity()));

    private final String id;
    private final float baseCost;

    DeliveryMethod(String id, float baseCost) {
        this.id = id;
        this.baseCost = baseCost;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    public float getBaseCost() {
        return baseCost;
    }

    @Nullable
    public static DeliveryMethod fromId(String id) {
        return BY_ID.get(id);
    }
}
