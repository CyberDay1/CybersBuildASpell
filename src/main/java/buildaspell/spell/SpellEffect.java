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

public enum SpellEffect implements StringRepresentable {
    DAMAGE("damage", 5.0f),
    IGNITE("ignite", 15.0f),
    FREEZE("freeze", 20.0f),
    TELEPORT("teleport", 50.0f),
    PULL("pull", 25.0f),
    PUSH("push", 25.0f),
    YEET("yeet", 30.0f),
    REAP("reap", 40.0f),
    EXPLOSION("explosion", 60.0f),
    HEAL("heal", 35.0f, true),
    LIGHTNING("lightning", 55.0f),
    POISON("poison", 30.0f),
    WITHER("wither", 40.0f),
    SATURATION("saturation", 20.0f, true),
    LAUNCH("launch", 35.0f),
    LIGHT("light", 12.0f),
    SLAM("slam", 30.0f),
    LEVITATION("levitation", 25.0f),
    SLOW_FALL("slow_fall", 15.0f, true),
    BREAK("break", 40.0f),
    INVISIBILITY("invisibility", 30.0f, true),
    SPEED("speed", 25.0f, true),
    HASTE("haste", 25.0f, true),
    BLINK("blink", 35.0f),
    SWAP("swap", 45.0f),
    SUMMON("summon", 50.0f),
    CREATE_WATER("create_water", 20.0f),
    EVAPORATE_WATER("evaporate_water", 25.0f),
    MARK("mark", 30.0f),
    RECALL("recall", 40.0f),
    PICKUP("pickup", 15.0f),
    SHIELD("shield", 45.0f, true),
    CONJURE("conjure", 25.0f),
    GROWTH("growth", 30.0f),
    CLEANSE("cleanse", 20.0f, true),
    CHARM("charm", 35.0f),
    BLIND("blind", 25.0f),
    SLOW("slow", 20.0f),
    WEAKEN("weaken", 25.0f),
    STRENGTHEN("strengthen", 30.0f, true),
    REGENERATE("regenerate", 35.0f, true),
    RESIST("resist", 35.0f, true),
    NIGHT_VISION("night_vision", 15.0f, true),
    WATER_BREATHING("water_breathing", 15.0f, true),
    ROOT("root", 30.0f),
    GRAPPLE("grapple", 35.0f),
    GUST("gust", 30.0f),
    // Create compat effects removed - using string-based compat system instead
    ;

    public static final Codec<SpellEffect> CODEC = StringRepresentable.fromEnum(SpellEffect::values);
    public static final StreamCodec<ByteBuf, SpellEffect> STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8.map(SpellEffect::fromId, SpellEffect::getSerializedName);

    private static final Map<String, SpellEffect> BY_ID = Arrays.stream(values())
            .collect(Collectors.toMap(SpellEffect::getSerializedName, Function.identity()));

    private final String id;
    private final float baseCost;
    private final boolean beneficial;

    SpellEffect(String id, float baseCost) {
        this(id, baseCost, false);
    }

    SpellEffect(String id, float baseCost, boolean beneficial) {
        this.id = id;
        this.baseCost = baseCost;
        this.beneficial = beneficial;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    public float getBaseCost() {
        return baseCost;
    }

    /**
     * Beneficial effects treat the caster as a valid target (Self-cast Heal heals you; the buff
     * family buffs you). Harmful effects never touch the caster: you cannot hurt, debuff, freeze,
     * or knock around yourself with your own spell. This is the single source of truth for that
     * caster-affect policy — executors consult it instead of hardcoding their own caster filters.
     */
    public boolean isBeneficial() {
        return beneficial;
    }

    @Nullable
    public static SpellEffect fromId(String id) {
        return BY_ID.get(id);
    }

    public static boolean isCompatEffect(String effectId) {
        return effectId.startsWith("compat:");
    }
}
