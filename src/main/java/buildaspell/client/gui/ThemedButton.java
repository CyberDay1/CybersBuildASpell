package buildaspell.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * A {@link Button} rendered in the shared {@link GuiTheme} — a rounded teal-obsidian
 * body with an amethyst frame that brightens on hover, matching the Spell Builder /
 * Arcane Altar surfaces instead of the flat vanilla button texture.
 */
public class ThemedButton extends Button {

    protected ThemedButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    /** Convenience factory mirroring {@code Button.builder(...).bounds(...).build()}. */
    public static ThemedButton of(Component message, int x, int y, int width, int height, OnPress onPress) {
        return new ThemedButton(x, y, width, height, message, onPress);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        boolean hovered = isHoveredOrFocused();

        int top, bot, border;
        if (!active) {
            top = 0xFF14242A; bot = 0xFF0A171C;
            border = 0xFF2A4A52;
        } else if (hovered) {
            top = 0xFF1E4650; bot = 0xFF12333B;
            border = GuiTheme.AMETHYST;
        } else {
            top = 0xFF16333B; bot = 0xFF0C1E24;
            border = GuiTheme.AMETHYST_DIM;
        }

        // Rounded amethyst/teal border with an inset gradient body.
        GuiTheme.roundedRect(g, x, y, x + w, y + h, 3, border);
        GuiTheme.roundedGradient(g, x + 1, y + 1, x + w - 1, y + h - 1, 2, top, bot);
        // Soft top sheen.
        g.fill(x + 3, y + 1, x + w - 3, y + 2, 0x22FFFFFF);
    }
}
