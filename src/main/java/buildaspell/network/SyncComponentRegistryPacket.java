package buildaspell.network;

import buildaspell.BuildASpell;
import buildaspell.client.ClientComponentRegistry;
import buildaspell.spell.SpellEffect;
import buildaspell.spell.data.ComponentDisplay;
import buildaspell.spell.data.EffectDefinition;
import buildaspell.spell.data.EffectRegistry;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Syncs datapack-authored spell components (currently effects) to the client so the spell
 * builder can render and price them. Only components without a backing enum constant are
 * sent — built-in effects are already known to the client via the enum. Display metadata
 * only crosses the wire; behavior stays server-side.
 *
 * <p>Sent on player join and on {@code /reload} via {@code OnDatapackSyncEvent}.
 */
public record SyncComponentRegistryPacket(List<Entry> effects) implements CustomPacketPayload {

    public record Entry(ResourceLocation id, ComponentDisplay display) {
        public static final StreamCodec<ByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, Entry::id,
                ComponentDisplay.STREAM_CODEC, Entry::display,
                Entry::new
        );
    }

    public static final Type<SyncComponentRegistryPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("buildaspell", "sync_component_registry"));

    public static final StreamCodec<ByteBuf, SyncComponentRegistryPacket> STREAM_CODEC = StreamCodec.composite(
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list()), SyncComponentRegistryPacket::effects,
            SyncComponentRegistryPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Snapshot the datapack-only effects (those with no enum constant) from the server registry. */
    public static SyncComponentRegistryPacket build() {
        List<Entry> effects = new ArrayList<>();
        for (Map.Entry<ResourceLocation, EffectDefinition> entry : EffectRegistry.all().entrySet()) {
            ResourceLocation id = entry.getKey();
            if (isEnumEffect(id)) {
                continue; // built-in, client already knows it via the enum
            }
            effects.add(new Entry(id, entry.getValue().display()));
        }
        return new SyncComponentRegistryPacket(effects);
    }

    private static boolean isEnumEffect(ResourceLocation id) {
        return id.getNamespace().equals(BuildASpell.MOD_ID) && SpellEffect.fromId(id.getPath()) != null;
    }

    public static void handle(SyncComponentRegistryPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ClientComponentRegistry.setEffects(packet.effects()));
    }
}
