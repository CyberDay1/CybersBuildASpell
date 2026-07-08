package buildaspell.network;

import buildaspell.config.ModConfig;
import buildaspell.registry.ModAttachments;
import buildaspell.spell.*;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public record SaveSpellPacket(int slotIndex, String name, String deliveryId,
                               List<String> orderedComponentIds,
                               List<String> componentTypes,
                               int visualColor, String visualShape, String visualTrail) implements CustomPacketPayload {
    public static final Type<SaveSpellPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("buildaspell", "save_spell"));

    // Written by hand: StreamCodec.composite caps at 6 field pairs and this payload has 8.
    public static final StreamCodec<ByteBuf, SaveSpellPacket> STREAM_CODEC = new StreamCodec<>() {
        private static final StreamCodec<ByteBuf, List<String>> STRING_LIST =
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list());

        @Override
        public SaveSpellPacket decode(ByteBuf buf) {
            int slot = ByteBufCodecs.INT.decode(buf);
            String name = ByteBufCodecs.STRING_UTF8.decode(buf);
            String deliveryId = ByteBufCodecs.STRING_UTF8.decode(buf);
            List<String> ids = STRING_LIST.decode(buf);
            List<String> types = STRING_LIST.decode(buf);
            int color = ByteBufCodecs.INT.decode(buf);
            String shape = ByteBufCodecs.STRING_UTF8.decode(buf);
            String trail = ByteBufCodecs.STRING_UTF8.decode(buf);
            return new SaveSpellPacket(slot, name, deliveryId, ids, types, color, shape, trail);
        }

        @Override
        public void encode(ByteBuf buf, SaveSpellPacket packet) {
            ByteBufCodecs.INT.encode(buf, packet.slotIndex());
            ByteBufCodecs.STRING_UTF8.encode(buf, packet.name());
            ByteBufCodecs.STRING_UTF8.encode(buf, packet.deliveryId());
            STRING_LIST.encode(buf, packet.orderedComponentIds());
            STRING_LIST.encode(buf, packet.componentTypes());
            ByteBufCodecs.INT.encode(buf, packet.visualColor());
            ByteBufCodecs.STRING_UTF8.encode(buf, packet.visualShape());
            ByteBufCodecs.STRING_UTF8.encode(buf, packet.visualTrail());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SaveSpellPacket packet, IPayloadContext context) {
        if (packet.slotIndex() < 0 || packet.slotIndex() >= 10) return;
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                PlayerSpellData spellData = serverPlayer.getData(ModAttachments.PLAYER_SPELL_DATA.get());

                DeliveryMethod delivery = DeliveryMethod.fromId(packet.deliveryId());
                if (delivery == null || !spellData.isDeliveryUnlocked(delivery)
                        || !ModConfig.isDeliveryEnabled(delivery)) {
                    return;
                }

                List<SpellComponent> components = new ArrayList<>();
                for (int i = 0; i < packet.orderedComponentIds().size() && i < packet.componentTypes().size(); i++) {
                    if (components.size() >= Spell.MAX_COMPONENTS) break;

                    String type = packet.componentTypes().get(i);
                    String id = packet.orderedComponentIds().get(i);

                    if ("effect".equals(type)) {
                        SpellEffect effect = SpellEffect.fromId(id);
                        if (effect != null && spellData.isEffectUnlocked(effect)
                                && ModConfig.isEffectEnabled(effect)) {
                            components.add(new SpellComponent.Effect(effect));
                        }
                    } else if ("modifier".equals(type)) {
                        SpellModifier modifier = SpellModifier.fromId(id);
                        if (modifier != null && spellData.isModifierUnlocked(modifier)
                                && ModConfig.isModifierEnabled(modifier)) {
                            components.add(new SpellComponent.Modifier(modifier));
                        }
                    } else if ("data_effect".equals(type)) {
                        // Datapack-authored effects bypass the unlock progression — always available.
                        // Validate against the loaded registry so stale ids aren't persisted.
                        ResourceLocation effectId = ResourceLocation.tryParse(id);
                        if (effectId != null && buildaspell.spell.data.EffectRegistry.get(effectId) != null) {
                            components.add(new SpellComponent.DataEffect(effectId));
                        }
                    } else if ("compat_effect".equals(type)) {
                        components.add(new SpellComponent.CompatEffect(id));
                    }
                }

                SpellVisual visual = new SpellVisual(
                        packet.visualColor(),
                        ProjectileShape.fromId(packet.visualShape()),
                        packet.visualTrail() == null || packet.visualTrail().isEmpty()
                                ? SpellVisual.DEFAULT_TRAIL : packet.visualTrail());
                Spell spell = new Spell(delivery, components, visual);
                SpellSlot slot = new SpellSlot(packet.name(), spell);

                PlayerSpellSlots spellSlots = serverPlayer.getData(ModAttachments.PLAYER_SPELL_SLOTS.get());
                spellSlots.setSlot(packet.slotIndex(), slot);

                SyncPlayerSpellSlotsPacket syncPacket = SyncPlayerSpellSlotsPacket.fromPlayerSlots(spellSlots);
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(serverPlayer, syncPacket);
            }
        });
    }
}
