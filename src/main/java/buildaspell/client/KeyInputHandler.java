package buildaspell.client;

import buildaspell.client.gui.AbilityRingScreen;
import buildaspell.client.gui.ManaBarOverlay;
import buildaspell.client.gui.SpellBuilderScreen;
import buildaspell.client.gui.SpellImportScreen;
import buildaspell.network.CastSpellPacket;
import buildaspell.network.SetActiveSlotPacket;
import buildaspell.registry.ModAttachments;
import buildaspell.spell.PlayerSpellData;
import buildaspell.spell.PlayerSpellSlots;
import buildaspell.spell.Spell;
import buildaspell.spell.SpellExporter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public class KeyInputHandler {

    public static void handleKeyInput() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        // Cast spell
        if (ModKeyBinds.CAST_SPELL.consumeClick()) {
            ClientPacketDistributor.sendToServer(new CastSpellPacket());
        }

        // Open spell builder GUI
        if (ModKeyBinds.OPEN_SPELL_BUILDER.consumeClick()) {
            PlayerSpellData spellData = player.getData(ModAttachments.PLAYER_SPELL_DATA.get());
            PlayerSpellSlots spellSlots = player.getData(ModAttachments.PLAYER_SPELL_SLOTS.get());
            mc.setScreenAndShow(new SpellBuilderScreen(spellData, spellSlots));
        }

        // Open ability ring
        if (ModKeyBinds.OPEN_ABILITY_RING.consumeClick()) {
            mc.setScreenAndShow(new AbilityRingScreen());
        }

        // Import spell
        if (ModKeyBinds.IMPORT_SPELL.consumeClick()) {
            mc.setScreenAndShow(new SpellImportScreen(null, code -> {
                Spell spell = SpellExporter.decode(code);
                if (spell != null) {
                    SpellBuilderScreen.loadImportedSpell(spell);
                    PlayerSpellSlots slots = player.getData(ModAttachments.PLAYER_SPELL_SLOTS.get());
                    PlayerSpellData data = player.getData(ModAttachments.PLAYER_SPELL_DATA.get());
                    mc.setScreenAndShow(new SpellBuilderScreen(data, slots));
                }
            }));
        }

        // Toggle mana bar
        if (ModKeyBinds.TOGGLE_MANA_BAR.consumeClick()) {
            ManaBarOverlay.toggleVisible();
        }

        // Spell slot selection (1-5)
        handleSlotKey(ModKeyBinds.SPELL_SLOT_1, 0);
        handleSlotKey(ModKeyBinds.SPELL_SLOT_2, 1);
        handleSlotKey(ModKeyBinds.SPELL_SLOT_3, 2);
        handleSlotKey(ModKeyBinds.SPELL_SLOT_4, 3);
        handleSlotKey(ModKeyBinds.SPELL_SLOT_5, 4);

        // Next/prev slot cycling
        if (ModKeyBinds.NEXT_SPELL_SLOT.consumeClick()) {
            PlayerSpellSlots slots = player.getData(ModAttachments.PLAYER_SPELL_SLOTS.get());
            int next = (slots.getActiveSlot() + 1) % PlayerSpellSlots.MAX_SLOTS;
            ClientPacketDistributor.sendToServer(new SetActiveSlotPacket(next));
        }
        if (ModKeyBinds.PREV_SPELL_SLOT.consumeClick()) {
            PlayerSpellSlots slots = player.getData(ModAttachments.PLAYER_SPELL_SLOTS.get());
            int prev = (slots.getActiveSlot() - 1 + PlayerSpellSlots.MAX_SLOTS) % PlayerSpellSlots.MAX_SLOTS;
            ClientPacketDistributor.sendToServer(new SetActiveSlotPacket(prev));
        }
    }

    private static void handleSlotKey(net.minecraft.client.KeyMapping key, int slotIndex) {
        if (key.consumeClick()) {
            ClientPacketDistributor.sendToServer(new SetActiveSlotPacket(slotIndex));
        }
    }
}
