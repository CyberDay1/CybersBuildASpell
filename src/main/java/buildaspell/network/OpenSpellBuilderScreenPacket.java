package buildaspell.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenSpellBuilderScreenPacket() implements CustomPacketPayload {
    public static final Type<OpenSpellBuilderScreenPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("buildaspell", "open_spell_builder"));

    public static final StreamCodec<ByteBuf, OpenSpellBuilderScreenPacket> STREAM_CODEC = StreamCodec.unit(new OpenSpellBuilderScreenPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenSpellBuilderScreenPacket packet, IPayloadContext context) {
        context.enqueueWork(() ->
                buildaspell.client.ClientScreenOpener.openSpellBuilder(packet));
    }
}
