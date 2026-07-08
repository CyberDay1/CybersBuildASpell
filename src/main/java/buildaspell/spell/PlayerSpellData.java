package buildaspell.spell;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.*;

public class PlayerSpellData {

    public static final Codec<PlayerSpellData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            DeliveryMethod.CODEC.listOf().fieldOf("unlocked_deliveries")
                    .forGetter(d -> new ArrayList<>(d.unlockedDeliveryMethods)),
            SpellEffect.CODEC.listOf().fieldOf("unlocked_effects")
                    .forGetter(d -> new ArrayList<>(d.unlockedEffects)),
            SpellModifier.CODEC.listOf().fieldOf("unlocked_modifiers")
                    .forGetter(d -> new ArrayList<>(d.unlockedModifiers)),
            Codec.BOOL.optionalFieldOf("starter_kit_granted", false)
                    .forGetter(d -> d.starterKitGranted),
            Codec.BOOL.optionalFieldOf("guidebook_granted", false)
                    .forGetter(d -> d.guidebookGranted)
    ).apply(inst, PlayerSpellData::new));

    /**
     * Network form of {@link #CODEC}, used by the attachment type's {@code sync(...)} handler so the
     * client is re-seeded with unlocks on login, respawn and dimension change (a fresh LocalPlayer is
     * built each time and carries over no attachments). Derived from the same codec as disk
     * serialization so the two can never drift apart.
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerSpellData> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);

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

    private PlayerSpellData(List<DeliveryMethod> deliveries, List<SpellEffect> effects, List<SpellModifier> modifiers, boolean starterKitGranted, boolean guidebookGranted) {
        this.unlockedDeliveryMethods = deliveries.isEmpty()
                ? EnumSet.noneOf(DeliveryMethod.class) : EnumSet.copyOf(deliveries);
        this.unlockedEffects = effects.isEmpty()
                ? EnumSet.noneOf(SpellEffect.class) : EnumSet.copyOf(effects);
        this.unlockedModifiers = modifiers.isEmpty()
                ? EnumSet.noneOf(SpellModifier.class) : EnumSet.copyOf(modifiers);
        this.starterKitGranted = starterKitGranted;
        this.guidebookGranted = guidebookGranted;
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
