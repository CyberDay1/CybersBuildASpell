package buildaspell.mana;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public class PlayerManaData {
    public static final Codec<PlayerManaData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.FLOAT.fieldOf("current_mana").forGetter(PlayerManaData::getCurrentMana)
    ).apply(inst, PlayerManaData::new));

    /**
     * Network form of {@link #CODEC}, used by the attachment type's {@code sync(...)} handler. The
     * per-second tick sync only fires when mana changed or is below max, so a full-mana player who
     * respawns or changes dimension would otherwise sit on the client-side default forever.
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerManaData> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);

    private float currentMana;

    public PlayerManaData() {
        this.currentMana = ManaConstants.DEFAULT_MAX_MANA;
    }

    public PlayerManaData(float currentMana) {
        this.currentMana = Math.max(0, currentMana);
    }

    public float getCurrentMana() { return currentMana; }

    public void setCurrentMana(float mana) {
        this.currentMana = Math.max(0, mana);
    }

    public void addMana(float amount) {
        this.currentMana += amount;
        if (this.currentMana < 0) this.currentMana = 0;
    }

    public boolean consumeMana(float amount) {
        if (currentMana >= amount) {
            currentMana -= amount;
            return true;
        }
        return false;
    }
}
