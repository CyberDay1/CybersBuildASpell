package buildaspell.network;

import buildaspell.entity.PortalEntity;
import buildaspell.portal.PortalManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public record NamePortalPacket(UUID portalUUID, String name) implements CustomPacketPayload {
    public static final Type<NamePortalPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath("buildaspell", "name_portal"));

    public static final StreamCodec<ByteBuf, NamePortalPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            packet -> packet.portalUUID.toString(),
            ByteBufCodecs.STRING_UTF8,
            NamePortalPacket::name,
            (uuidStr, name) -> new NamePortalPacket(UUID.fromString(uuidStr), name)
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(NamePortalPacket packet, IPayloadContext context) {
        if (packet.name.length() > 64) return;
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer
                    && serverPlayer.level() instanceof ServerLevel serverLevel) {
                Entity entity = serverLevel.getEntity(packet.portalUUID);
                if (entity instanceof PortalEntity portal) {
                    double distance = serverPlayer.distanceToSqr(portal);
                    if (distance <= 256) {
                        portal.setPortalName(packet.name);
                        PortalManager.updatePortalName(packet.portalUUID, packet.name);
                        serverPlayer.sendSystemMessage(Component.literal("Portal renamed to: " + packet.name).withStyle(ChatFormatting.GREEN));
                    }
                }
            }
        });
    }
}
