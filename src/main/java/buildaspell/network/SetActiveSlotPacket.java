package buildaspell.network;

import buildaspell.registry.ModAttachments;
import buildaspell.spell.PlayerSpellSlots;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SetActiveSlotPacket(int slotIndex) implements CustomPacketPayload {
    public static final Type<SetActiveSlotPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("buildaspell", "set_active_slot"));

    public static final StreamCodec<ByteBuf, SetActiveSlotPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            SetActiveSlotPacket::slotIndex,
            SetActiveSlotPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SetActiveSlotPacket packet, IPayloadContext context) {
        if (packet.slotIndex() < 0 || packet.slotIndex() >= 10) return;
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                PlayerSpellSlots spellSlots = serverPlayer.getData(ModAttachments.PLAYER_SPELL_SLOTS.get());
                spellSlots.setActiveSlot(packet.slotIndex());
            }
        });
    }
}
