package buildaspell.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public record OpenPortalNamingScreenPacket(UUID portalUUID,
                                            String currentName) implements CustomPacketPayload {
    public static final Type<OpenPortalNamingScreenPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath("buildaspell", "open_portal_naming"));

    public static final StreamCodec<ByteBuf, OpenPortalNamingScreenPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            packet -> packet.portalUUID.toString(),
            ByteBufCodecs.STRING_UTF8,
            OpenPortalNamingScreenPacket::currentName,
            (uuidStr, name) -> new OpenPortalNamingScreenPacket(UUID.fromString(uuidStr), name)
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenPortalNamingScreenPacket packet, IPayloadContext context) {
        context.enqueueWork(() ->
                buildaspell.client.ClientScreenOpener.openPortalNaming(packet));
    }
}
