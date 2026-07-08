package buildaspell.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import org.lwjgl.glfw.GLFW;

public class ModKeyBinds {
    public static final KeyMapping.Category KEY_CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("buildaspell", "spells"));

    public static final KeyMapping CAST_SPELL = new KeyMapping(
            "key.buildaspell.cast_spell", GLFW.GLFW_KEY_R, KEY_CATEGORY);
    public static final KeyMapping OPEN_SPELL_BUILDER = new KeyMapping(
            "key.buildaspell.spell_builder", GLFW.GLFW_KEY_G, KEY_CATEGORY);
    public static final KeyMapping OPEN_ABILITY_RING = new KeyMapping(
            "key.buildaspell.ability_ring", GLFW.GLFW_KEY_V, KEY_CATEGORY);

    public static final KeyMapping SPELL_SLOT_1 = new KeyMapping(
            "key.buildaspell.spell_slot_1", GLFW.GLFW_KEY_1, KEY_CATEGORY);
    public static final KeyMapping SPELL_SLOT_2 = new KeyMapping(
            "key.buildaspell.spell_slot_2", GLFW.GLFW_KEY_2, KEY_CATEGORY);
    public static final KeyMapping SPELL_SLOT_3 = new KeyMapping(
            "key.buildaspell.spell_slot_3", GLFW.GLFW_KEY_3, KEY_CATEGORY);
    public static final KeyMapping SPELL_SLOT_4 = new KeyMapping(
            "key.buildaspell.spell_slot_4", GLFW.GLFW_KEY_4, KEY_CATEGORY);
    public static final KeyMapping SPELL_SLOT_5 = new KeyMapping(
            "key.buildaspell.spell_slot_5", GLFW.GLFW_KEY_5, KEY_CATEGORY);

    public static final KeyMapping NEXT_SPELL_SLOT = new KeyMapping(
            "key.buildaspell.next_spell_slot",
            InputConstants.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_5, KEY_CATEGORY);
    public static final KeyMapping PREV_SPELL_SLOT = new KeyMapping(
            "key.buildaspell.prev_spell_slot",
            InputConstants.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_4, KEY_CATEGORY);

    public static final KeyMapping IMPORT_SPELL = new KeyMapping(
            "key.buildaspell.import_spell", GLFW.GLFW_KEY_I, KEY_CATEGORY);
    public static final KeyMapping TOGGLE_MANA_BAR = new KeyMapping(
            "key.buildaspell.toggle_mana_bar", GLFW.GLFW_KEY_M, KEY_CATEGORY);

    public static final KeyMapping[] ALL_KEYS = {
            CAST_SPELL, OPEN_SPELL_BUILDER, OPEN_ABILITY_RING,
            SPELL_SLOT_1, SPELL_SLOT_2, SPELL_SLOT_3, SPELL_SLOT_4, SPELL_SLOT_5,
            NEXT_SPELL_SLOT, PREV_SPELL_SLOT,
            IMPORT_SPELL, TOGGLE_MANA_BAR
    };
}
