package buildaspell.network;

import buildaspell.entity.PortalEntity;
import buildaspell.portal.PortalInfo;
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

import javax.annotation.Nullable;
import java.util.UUID;

public record DialPortalPacket(UUID sourcePortalUUID,
                                @Nullable UUID targetPortalUUID) implements CustomPacketPayload {
    public static final Type<DialPortalPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath("buildaspell", "dial_portal"));

    public static final StreamCodec<ByteBuf, DialPortalPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            packet -> packet.sourcePortalUUID.toString(),
            ByteBufCodecs.STRING_UTF8,
            packet -> packet.targetPortalUUID != null ? packet.targetPortalUUID.toString() : "",
            (sourceStr, targetStr) -> new DialPortalPacket(
                    UUID.fromString(sourceStr),
                    targetStr.isEmpty() ? null : UUID.fromString(targetStr)
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DialPortalPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer
                    && serverPlayer.level() instanceof ServerLevel serverLevel) {
                Entity entity = serverLevel.getEntity(packet.sourcePortalUUID);
                if (entity instanceof PortalEntity portal) {
                    // Only the portal's owner (or an OP) may dial it. Mirrors PortalEntity.interact()
                    // so a hand-crafted packet can't dial someone else's portal.
                    UUID owner = portal.getCasterUUID();
                    if (owner != null && !owner.equals(serverPlayer.getUUID())
                            && !net.minecraft.commands.Commands.LEVEL_GAMEMASTERS.check(serverPlayer.permissions())) {
                        return;
                    }
                    if (packet.targetPortalUUID == null) {
                        portal.undial();
                        serverPlayer.sendSystemMessage(Component.literal("Portal connection cleared").withStyle(ChatFormatting.GRAY));
                    } else {
                        PortalInfo targetInfo = PortalManager.getPortalInfo(packet.targetPortalUUID);
                        if (targetInfo != null) {
                            portal.dialTo(packet.targetPortalUUID, targetInfo.getDimension(), targetInfo.getPosition());
                            serverPlayer.sendSystemMessage(Component.literal("Portal dialed successfully").withStyle(ChatFormatting.GREEN));
                        }
                    }
                }
            }
        });
    }
}
