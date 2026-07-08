package buildaspell.network;

import buildaspell.BuildASpell;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModPackets {

    @SubscribeEvent
    public static void registerPackets(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        // Server-bound packets
        registrar.playToServer(
                CastSpellPacket.TYPE,
                CastSpellPacket.STREAM_CODEC,
                CastSpellPacket::handle
        );

        registrar.playToServer(
                SetActiveSlotPacket.TYPE,
                SetActiveSlotPacket.STREAM_CODEC,
                SetActiveSlotPacket::handle
        );

        registrar.playToServer(
                SaveSpellPacket.TYPE,
                SaveSpellPacket.STREAM_CODEC,
                SaveSpellPacket::handle
        );

        registrar.playToServer(
                NamePortalPacket.TYPE,
                NamePortalPacket.STREAM_CODEC,
                NamePortalPacket::handle
        );

        registrar.playToServer(
                DialPortalPacket.TYPE,
                DialPortalPacket.STREAM_CODEC,
                DialPortalPacket::handle
        );

        registrar.playToServer(
                ArcaneAltarEnchantPacket.TYPE,
                ArcaneAltarEnchantPacket.STREAM_CODEC,
                ArcaneAltarEnchantPacket::handle
        );

        registrar.playToServer(
                ResizePortalPacket.TYPE,
                ResizePortalPacket.STREAM_CODEC,
                ResizePortalPacket::handle
        );

        // Client-bound packets
        registrar.playToClient(
                SyncPlayerSpellDataPacket.TYPE,
                SyncPlayerSpellDataPacket.STREAM_CODEC,
                SyncPlayerSpellDataPacket::handle
        );

        registrar.playToClient(
                SyncPlayerSpellSlotsPacket.TYPE,
                SyncPlayerSpellSlotsPacket.STREAM_CODEC,
                SyncPlayerSpellSlotsPacket::handle
        );

        registrar.playToClient(
                OpenPortalNamingScreenPacket.TYPE,
                OpenPortalNamingScreenPacket.STREAM_CODEC,
                OpenPortalNamingScreenPacket::handle
        );

        registrar.playToClient(
                OpenPortalDialScreenPacket.TYPE,
                OpenPortalDialScreenPacket.STREAM_CODEC,
                OpenPortalDialScreenPacket::handle
        );

        registrar.playToClient(
                OpenSpellBuilderScreenPacket.TYPE,
                OpenSpellBuilderScreenPacket.STREAM_CODEC,
                OpenSpellBuilderScreenPacket::handle
        );

        registrar.playToClient(
                SyncPlayerManaPacket.TYPE,
                SyncPlayerManaPacket.STREAM_CODEC,
                SyncPlayerManaPacket::handle
        );

        registrar.playToClient(
                SyncComponentRegistryPacket.TYPE,
                SyncComponentRegistryPacket.STREAM_CODEC,
                SyncComponentRegistryPacket::handle
        );

        BuildASpell.LOGGER.debug("Registered network packets");
    }
}
