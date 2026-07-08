package buildaspell.network;

import buildaspell.registry.ModAttachments;
import buildaspell.spell.PlayerSpellSlots;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncPlayerSpellSlotsPacket(
        net.minecraft.nbt.CompoundTag data
) implements CustomPacketPayload {
    public static final Type<SyncPlayerSpellSlotsPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("buildaspell", "sync_player_spell_slots"));

    public static final StreamCodec<ByteBuf, SyncPlayerSpellSlotsPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.COMPOUND_TAG,
            SyncPlayerSpellSlotsPacket::data,
            SyncPlayerSpellSlotsPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static SyncPlayerSpellSlotsPacket fromPlayerSlots(PlayerSpellSlots slots) {
        // Serialize via Codec to CompoundTag for network transport
        var result = PlayerSpellSlots.CODEC.encodeStart(
                net.minecraft.nbt.NbtOps.INSTANCE, slots
        );
        net.minecraft.nbt.CompoundTag tag = (net.minecraft.nbt.CompoundTag) result.getOrThrow();
        return new SyncPlayerSpellSlotsPacket(tag);
    }

    public static void handle(SyncPlayerSpellSlotsPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            // Decode via Codec from CompoundTag
            var result = PlayerSpellSlots.CODEC.parse(
                    net.minecraft.nbt.NbtOps.INSTANCE, packet.data
            );
            result.ifSuccess(slots -> {
                PlayerSpellSlots current = context.player().getData(ModAttachments.PLAYER_SPELL_SLOTS.get());
                // Copy data from decoded slots
                for (int i = 0; i < PlayerSpellSlots.MAX_SLOTS; i++) {
                    current.setSlot(i, slots.getSlot(i));
                }
                current.setActiveSlot(slots.getActiveSlot());
            });
        });
    }
}
