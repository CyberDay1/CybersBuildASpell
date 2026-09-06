package buildaspell.mana;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;

public class PlayerManaData implements ValueIOSerializable {
    public static final Codec<PlayerManaData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.FLOAT.fieldOf("current_mana").forGetter(PlayerManaData::getCurrentMana)
    ).apply(inst, PlayerManaData::new));

    private float currentMana;

    /**
     * The figure this player's client was last sent. Server-side only, and deliberately absent from
     * {@link #CODEC} and from the saved form: it describes the connection rather than the player, and
     * starting at NaN means the first comparison always disagrees, so a freshly loaded player is
     * always sent one packet.
     */
    private transient float lastSyncedMana = Float.NaN;

    public PlayerManaData() {
        this.currentMana = ManaConstants.DEFAULT_MAX_MANA;
    }

    public PlayerManaData(float currentMana) {
        this.currentMana = Math.max(0, currentMana);
    }

    @Override
    public void serialize(ValueOutput output) {
        output.putFloat("current_mana", currentMana);
    }

    @Override
    public void deserialize(ValueInput input) {
        this.currentMana = input.getFloatOr("current_mana", ManaConstants.DEFAULT_MAX_MANA);
    }

    public float getCurrentMana() { return currentMana; }

    public float getLastSyncedMana() { return lastSyncedMana; }

    public void setLastSyncedMana(float mana) {
        this.lastSyncedMana = mana;
    }

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
