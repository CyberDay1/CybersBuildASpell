package buildaspell.client;

import buildaspell.client.gui.PortalDialScreen;
import buildaspell.client.gui.PortalNamingScreen;
import buildaspell.client.gui.SpellBuilderScreen;
import buildaspell.network.OpenPortalDialScreenPacket;
import buildaspell.network.OpenPortalNamingScreenPacket;
import buildaspell.network.OpenSpellBuilderScreenPacket;
import buildaspell.registry.ModAttachments;
import buildaspell.spell.PlayerSpellData;
import buildaspell.spell.PlayerSpellSlots;
import net.minecraft.client.Minecraft;

/**
 * Client-only sink for screen-opening payload handlers. Keeping the actual
 * {@code net.minecraft.client} references out of the common packet classes
 * means a dedicated server never has to verify (and therefore load) those
 * client classes when the packet types are registered.
 */
public final class ClientScreenOpener {
    private ClientScreenOpener() {}

    public static void openPortalNaming(OpenPortalNamingScreenPacket packet) {
        Minecraft.getInstance().setScreenAndShow(
                new PortalNamingScreen(packet.portalUUID(), packet.currentName()));
    }

    public static void openPortalDial(OpenPortalDialScreenPacket packet) {
        Minecraft.getInstance().setScreenAndShow(
                new PortalDialScreen(packet.sourcePortalUUID(), packet.discoveredPortals(),
                        packet.currentWidth(), packet.currentHeight(),
                        packet.minSize(), packet.maxSize()));
    }

    public static void openSpellBuilder(OpenSpellBuilderScreenPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        PlayerSpellData spellData = mc.player.getData(ModAttachments.PLAYER_SPELL_DATA.get());
        PlayerSpellSlots spellSlots = mc.player.getData(ModAttachments.PLAYER_SPELL_SLOTS.get());
        mc.setScreenAndShow(new SpellBuilderScreen(spellData, spellSlots));
    }
}
