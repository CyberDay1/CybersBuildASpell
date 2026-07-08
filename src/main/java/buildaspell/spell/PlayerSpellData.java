package buildaspell.spell;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;

import java.util.*;

public class PlayerSpellData implements ValueIOSerializable {

    public static final Codec<PlayerSpellData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            DeliveryMethod.CODEC.listOf().fieldOf("unlocked_deliveries")
                    .forGetter(d -> new ArrayList<>(d.unlockedDeliveryMethods)),
            SpellEffect.CODEC.listOf().fieldOf("unlocked_effects")
                    .forGetter(d -> new ArrayList<>(d.unlockedEffects)),
            SpellModifier.CODEC.listOf().fieldOf("unlocked_modifiers")
                    .forGetter(d -> new ArrayList<>(d.unlockedModifiers))
    ).apply(inst, PlayerSpellData::new));

    private final Set<DeliveryMethod> unlockedDeliveryMethods;
    private final Set<SpellEffect> unlockedEffects;
    private final Set<SpellModifier> unlockedModifiers;
    private boolean starterKitGranted = false;
    private boolean guidebookGranted = false;

    public PlayerSpellData() {
        this.unlockedDeliveryMethods = EnumSet.noneOf(DeliveryMethod.class);
        this.unlockedEffects = EnumSet.noneOf(SpellEffect.class);
        this.unlockedModifiers = EnumSet.noneOf(SpellModifier.class);
    }

    private PlayerSpellData(List<DeliveryMethod> deliveries, List<SpellEffect> effects, List<SpellModifier> modifiers) {
        this.unlockedDeliveryMethods = deliveries.isEmpty()
                ? EnumSet.noneOf(DeliveryMethod.class) : EnumSet.copyOf(deliveries);
        this.unlockedEffects = effects.isEmpty()
                ? EnumSet.noneOf(SpellEffect.class) : EnumSet.copyOf(effects);
        this.unlockedModifiers = modifiers.isEmpty()
                ? EnumSet.noneOf(SpellModifier.class) : EnumSet.copyOf(modifiers);
    }

    @Override
    public void serialize(ValueOutput output) {
        output.store("unlocked_deliveries", DeliveryMethod.CODEC.listOf(), new ArrayList<>(unlockedDeliveryMethods));
        output.store("unlocked_effects", SpellEffect.CODEC.listOf(), new ArrayList<>(unlockedEffects));
        output.store("unlocked_modifiers", SpellModifier.CODEC.listOf(), new ArrayList<>(unlockedModifiers));
        output.putBoolean("starter_kit_granted", starterKitGranted);
        output.putBoolean("guidebook_granted", guidebookGranted);
    }

    @Override
    public void deserialize(ValueInput input) {
        unlockedDeliveryMethods.clear();
        unlockedEffects.clear();
        unlockedModifiers.clear();
        input.read("unlocked_deliveries", DeliveryMethod.CODEC.listOf())
                .ifPresent(list -> { if (!list.isEmpty()) unlockedDeliveryMethods.addAll(list); });
        input.read("unlocked_effects", SpellEffect.CODEC.listOf())
                .ifPresent(list -> { if (!list.isEmpty()) unlockedEffects.addAll(list); });
        input.read("unlocked_modifiers", SpellModifier.CODEC.listOf())
                .ifPresent(list -> { if (!list.isEmpty()) unlockedModifiers.addAll(list); });
        this.starterKitGranted = input.getBooleanOr("starter_kit_granted", false);
        this.guidebookGranted = input.getBooleanOr("guidebook_granted", false);
    }

    public boolean isDeliveryUnlocked(DeliveryMethod method) { return unlockedDeliveryMethods.contains(method); }
    public boolean isEffectUnlocked(SpellEffect effect) { return unlockedEffects.contains(effect); }
    public boolean isModifierUnlocked(SpellModifier modifier) { return unlockedModifiers.contains(modifier); }

    public void unlockDelivery(DeliveryMethod method) { unlockedDeliveryMethods.add(method); }
    public void unlockEffect(SpellEffect effect) { unlockedEffects.add(effect); }
    public void unlockModifier(SpellModifier modifier) { unlockedModifiers.add(modifier); }

    public boolean isStarterKitGranted() { return starterKitGranted; }
    public void markStarterKitGranted() { this.starterKitGranted = true; }

    public boolean isGuidebookGranted() { return guidebookGranted; }
    public void markGuidebookGranted() { this.guidebookGranted = true; }

    public void unlockAll() {
        unlockedDeliveryMethods.addAll(EnumSet.allOf(DeliveryMethod.class));
        unlockedEffects.addAll(EnumSet.allOf(SpellEffect.class));
        unlockedModifiers.addAll(EnumSet.allOf(SpellModifier.class));
    }

    public Set<DeliveryMethod> getUnlockedDeliveryMethods() { return Collections.unmodifiableSet(unlockedDeliveryMethods); }
    public Set<SpellEffect> getUnlockedEffects() { return Collections.unmodifiableSet(unlockedEffects); }
    public Set<SpellModifier> getUnlockedModifiers() { return Collections.unmodifiableSet(unlockedModifiers); }

    public void clearAll() {
        unlockedDeliveryMethods.clear();
        unlockedEffects.clear();
        unlockedModifiers.clear();
    }
}
