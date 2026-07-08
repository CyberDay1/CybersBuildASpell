package buildaspell.spell;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PlayerSpellSlots implements ValueIOSerializable {
    public static final int MAX_SLOTS = 10;

    public static final Codec<PlayerSpellSlots> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            SpellSlot.CODEC.listOf().fieldOf("slots").forGetter(PlayerSpellSlots::getSlotList),
            Codec.INT.fieldOf("active_slot").forGetter(PlayerSpellSlots::getActiveSlot)
    ).apply(inst, PlayerSpellSlots::new));

    private final SpellSlot[] slots;
    private int activeSlot;

    public PlayerSpellSlots() {
        this.slots = new SpellSlot[MAX_SLOTS];
        for (int i = 0; i < MAX_SLOTS; i++) {
            slots[i] = new SpellSlot();
        }
        this.activeSlot = 0;
    }

    private PlayerSpellSlots(List<SpellSlot> slotList, int activeSlot) {
        this.slots = new SpellSlot[MAX_SLOTS];
        for (int i = 0; i < MAX_SLOTS; i++) {
            slots[i] = i < slotList.size() ? slotList.get(i) : new SpellSlot();
        }
        this.activeSlot = Math.min(Math.max(activeSlot, 0), MAX_SLOTS - 1);
    }

    public SpellSlot getSlot(int index) {
        if (index < 0 || index >= MAX_SLOTS) return new SpellSlot();
        return slots[index];
    }

    public void setSlot(int index, SpellSlot slot) {
        if (index >= 0 && index < MAX_SLOTS) {
            slots[index] = slot;
        }
    }

    public int getActiveSlot() { return activeSlot; }

    public void setActiveSlot(int slot) {
        if (slot >= 0 && slot < MAX_SLOTS) {
            this.activeSlot = slot;
        }
    }

    @Nullable
    public SpellSlot getActiveSpellSlot() {
        return slots[activeSlot];
    }

    @Nullable
    public Spell getActiveSpell() {
        SpellSlot slot = getActiveSpellSlot();
        return slot != null && slot.hasSpell() ? slot.getSpell() : null;
    }

    public int getMaxSlots() { return MAX_SLOTS; }

    private List<SpellSlot> getSlotList() {
        List<SpellSlot> list = new ArrayList<>(MAX_SLOTS);
        for (SpellSlot slot : slots) {
            list.add(slot);
        }
        return list;
    }

    @Override
    public void serialize(ValueOutput output) {
        output.store("slots", SpellSlot.CODEC.listOf(), getSlotList());
        output.putInt("active_slot", activeSlot);
    }

    @Override
    public void deserialize(ValueInput input) {
        List<SpellSlot> slotList = input.read("slots", SpellSlot.CODEC.listOf()).orElse(List.of());
        for (int i = 0; i < MAX_SLOTS; i++) {
            slots[i] = i < slotList.size() ? slotList.get(i) : new SpellSlot();
        }
        this.activeSlot = Math.min(Math.max(input.getIntOr("active_slot", 0), 0), MAX_SLOTS - 1);
    }
}
