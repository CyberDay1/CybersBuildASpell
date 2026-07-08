package buildaspell.network;

import buildaspell.mana.PlayerManaData;
import buildaspell.registry.ModAttachments;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncPlayerManaPacket(float currentMana) implements CustomPacketPayload {
    public static final Type<SyncPlayerManaPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("buildaspell", "sync_player_mana"));

    public static final StreamCodec<ByteBuf, SyncPlayerManaPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT,
            SyncPlayerManaPacket::currentMana,
            SyncPlayerManaPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncPlayerManaPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            PlayerManaData manaData = context.player().getData(ModAttachments.PLAYER_MANA.get());
            manaData.setCurrentMana(packet.currentMana);
        });
    }
}
