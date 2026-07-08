package buildaspell.network;

import buildaspell.registry.ModAttachments;
import buildaspell.spell.DeliveryMethod;
import buildaspell.spell.PlayerSpellData;
import buildaspell.spell.SpellEffect;
import buildaspell.spell.SpellModifier;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public record SyncPlayerSpellDataPacket(List<String> deliveries, List<String> effects,
                                         List<String> modifiers) implements CustomPacketPayload {
    public static final Type<SyncPlayerSpellDataPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("buildaspell", "sync_player_spell_data"));

    public static final StreamCodec<ByteBuf, SyncPlayerSpellDataPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
            SyncPlayerSpellDataPacket::deliveries,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
            SyncPlayerSpellDataPacket::effects,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
            SyncPlayerSpellDataPacket::modifiers,
            SyncPlayerSpellDataPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static SyncPlayerSpellDataPacket fromPlayerData(PlayerSpellData data) {
        List<String> deliveryIds = new ArrayList<>();
        for (DeliveryMethod method : data.getUnlockedDeliveryMethods()) {
            deliveryIds.add(method.getSerializedName());
        }

        List<String> effectIds = new ArrayList<>();
        for (SpellEffect effect : data.getUnlockedEffects()) {
            effectIds.add(effect.getSerializedName());
        }

        List<String> modifierIds = new ArrayList<>();
        for (SpellModifier modifier : data.getUnlockedModifiers()) {
            modifierIds.add(modifier.getSerializedName());
        }

        return new SyncPlayerSpellDataPacket(deliveryIds, effectIds, modifierIds);
    }

    public static void handle(SyncPlayerSpellDataPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            PlayerSpellData spellData = context.player().getData(ModAttachments.PLAYER_SPELL_DATA.get());

            spellData.clearAll();

            for (String deliveryId : packet.deliveries) {
                DeliveryMethod method = DeliveryMethod.fromId(deliveryId);
                if (method != null) {
                    spellData.unlockDelivery(method);
                }
            }

            for (String effectId : packet.effects) {
                SpellEffect effect = SpellEffect.fromId(effectId);
                if (effect != null) {
                    spellData.unlockEffect(effect);
                }
            }

            for (String modifierId : packet.modifiers) {
                SpellModifier modifier = SpellModifier.fromId(modifierId);
                if (modifier != null) {
                    spellData.unlockModifier(modifier);
                }
            }
        });
    }
}
