package buildaspell.network;

import buildaspell.compat.NeoPortalsCompat;
import buildaspell.config.ModConfig;
import buildaspell.entity.PortalEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public record ResizePortalPacket(UUID portalUUID, float width, float height) implements CustomPacketPayload {
    public static final Type<ResizePortalPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath("buildaspell", "resize_portal"));

    public static final StreamCodec<ByteBuf, ResizePortalPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            packet -> packet.portalUUID.toString(),
            ByteBufCodecs.FLOAT,
            ResizePortalPacket::width,
            ByteBufCodecs.FLOAT,
            ResizePortalPacket::height,
            (uuidStr, w, h) -> new ResizePortalPacket(UUID.fromString(uuidStr), w, h)
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ResizePortalPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer
                    && serverPlayer.level() instanceof ServerLevel serverLevel) {
                Entity entity = serverLevel.getEntity(packet.portalUUID);
                if (entity instanceof PortalEntity portal) {
                    // Validate: must be caster or OP, within 256 blocks (16^2)
                    UUID caster = portal.getCasterUUID();
                    if (caster != null && !caster.equals(serverPlayer.getUUID())
                            && !net.minecraft.commands.Commands.LEVEL_GAMEMASTERS.check(serverPlayer.permissions())) {
                        return;
                    }
                    if (serverPlayer.distanceToSqr(portal) > 256) return;

                    portal.setPortalWidth(packet.width);
                    portal.setPortalHeight(packet.height);

                    // If NeoPortals portals exist, recreate at new size
                    if (portal.hasNeoPortals() && NeoPortalsCompat.isLoaded()) {
                        UUID destUUID = portal.getDialedDestinationUUID();
                        net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> destDim = portal.getDestinationDimension();
                        net.minecraft.world.phys.Vec3 destPos = portal.getDestinationPos();
                        portal.undial();
                        if (destUUID != null && destDim != null && destPos != null) {
                            portal.dialTo(destUUID, destDim, destPos);
                        }
                    }
                }
            }
        });
    }
}
