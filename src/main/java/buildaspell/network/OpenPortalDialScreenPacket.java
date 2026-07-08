package buildaspell.network;

import buildaspell.portal.PortalInfo;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record OpenPortalDialScreenPacket(UUID sourcePortalUUID,
                                          List<PortalInfo> discoveredPortals,
                                          float currentWidth, float currentHeight,
                                          float minSize, float maxSize) implements CustomPacketPayload {
    public static final Type<OpenPortalDialScreenPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("buildaspell", "open_portal_dial"));

    public static final StreamCodec<ByteBuf, OpenPortalDialScreenPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            packet -> packet.sourcePortalUUID.toString(),
            ByteBufCodecs.COMPOUND_TAG,
            OpenPortalDialScreenPacket::serializeData,
            (uuidStr, tag) -> deserializeData(uuidStr, tag)
    );

    private static CompoundTag serializeData(OpenPortalDialScreenPacket packet) {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (PortalInfo portal : packet.discoveredPortals) {
            PortalInfo.CODEC.encodeStart(NbtOps.INSTANCE, portal)
                    .ifSuccess(list::add);
        }
        tag.put("Portals", list);
        tag.putFloat("Width", packet.currentWidth);
        tag.putFloat("Height", packet.currentHeight);
        tag.putFloat("MinSize", packet.minSize);
        tag.putFloat("MaxSize", packet.maxSize);
        return tag;
    }

    private static OpenPortalDialScreenPacket deserializeData(String uuidStr, CompoundTag tag) {
        List<PortalInfo> portals = new ArrayList<>();
        ListTag list = tag.getList("Portals", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            PortalInfo.CODEC.parse(NbtOps.INSTANCE, list.getCompound(i))
                    .ifSuccess(portals::add);
        }
        float width = tag.getFloat("Width");
        float height = tag.getFloat("Height");
        float minSize = tag.getFloat("MinSize");
        float maxSize = tag.getFloat("MaxSize");
        return new OpenPortalDialScreenPacket(UUID.fromString(uuidStr), portals, width, height, minSize, maxSize);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenPortalDialScreenPacket packet, IPayloadContext context) {
        context.enqueueWork(() ->
                buildaspell.client.ClientScreenOpener.openPortalDial(packet));
    }
}
