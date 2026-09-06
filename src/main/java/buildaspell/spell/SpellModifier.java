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
    // Double and Echo charge no flat price: each stack raises the whole spell's cost by a
    // percentage instead (their "totalCostMultiplier"), because each one re-scales the whole
    // spell's output rather than adding a fixed amount of work to it.
    DOUBLE("double", 0.0f, true, 1.8),
    ECHO("echo", 0.0f, true, 1.8),
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
    private final double totalCostMultiplier;

    SpellModifier(String id, float baseCost, boolean stackable) {
        this(id, baseCost, stackable, 1.0);
    }

    SpellModifier(String id, float baseCost, boolean stackable, double totalCostMultiplier) {
        this.id = id;
        this.baseCost = baseCost;
        this.stackable = stackable;
        this.totalCostMultiplier = totalCostMultiplier;
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

    /**
     * What one stack of this multiplies the spell's whole mana cost by, before the server config has
     * its say. 1.0 - every modifier but Double and Echo - means it charges its flat price and nothing
     * more. This is the shipped default the config setting of the same name is seeded from, and the
     * number used when there is no server config to read, so the two can never disagree.
     */
    public double getTotalCostMultiplier() {
        return totalCostMultiplier;
    }

    @Nullable
    public static SpellModifier fromId(String id) {
        return BY_ID.get(id);
    }
}
