package buildaspell.client.gui;

import buildaspell.mana.ManaHelper;
import buildaspell.mana.PlayerManaData;
import buildaspell.registry.ModAttachments;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.gui.GuiLayer;

public class ManaBarOverlay implements GuiLayer {
    private static final int BAR_WIDTH = 82;
    private static final int BAR_HEIGHT = 7;
    private static final int BAR_OFFSET_X = 91;
    private static final int BAR_OFFSET_Y = 49;

    // Palette: teal fill with purple accents.
    private static final int BORDER_COLOR = 0xFF2E1F55;      // deep purple border
    private static final int TRACK_TOP = 0xFF19112E;         // empty-track gradient (purple-dark)
    private static final int TRACK_BOTTOM = 0xFF0B0718;
    private static final int MANA_COLOR_START = 0xFF1FA890;  // deep teal (low fill)
    private static final int MANA_COLOR_END = 0xFF3DE8C6;    // bright teal (full fill)
    private static final int MANA_LOW_COLOR = 0xFFB060FF;    // purple low-mana warning
    private static final int SHIMMER_TINT = 0x00E8DAFF;      // lavender sweep (RGB only)

    // Animation: a highlight that sweeps across the filled portion, and a
    // low-mana warning pulse. Kept subtle on purpose.
    private static final int SHIMMER_HALF = 2;        // half-width of the sweep band (px)
    private static final double SHIMMER_PERIOD_MS = 1800.0;
    private static final double LOW_PULSE_PERIOD_MS = 500.0;

    private static float displayedMana = 0;
    private static final float SMOOTHING_SPEED = 0.15f;

    private static boolean visible = true;

    public static boolean isVisible() {
        return visible;
    }

    public static void toggleVisible() {
        visible = !visible;
    }

    public static void reset() {
        displayedMana = 0;
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (!visible) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player == null || player.isSpectator() || mc.gui.hud.isHidden()) {
            return;
        }

        PlayerManaData manaData = player.getData(ModAttachments.PLAYER_MANA.get());
        float currentMana = manaData.getCurrentMana();
        float maxMana = ManaHelper.getMaxMana(player);

        displayedMana = Mth.lerp(SMOOTHING_SPEED, displayedMana, currentMana);

        float manaPercent = Mth.clamp(displayedMana / maxMana, 0, 1);

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int barX = screenWidth / 2 + BAR_OFFSET_X;
        int barY = screenHeight - BAR_OFFSET_Y;

        int radius = BAR_HEIGHT / 2;

        // Purple border capsule.
        roundedRect(graphics, barX - 1, barY - 1, barX + BAR_WIDTH + 1, barY + BAR_HEIGHT + 1, radius + 1, BORDER_COLOR);
        // Empty track: subtle vertical inner-shadow.
        roundedGradient(graphics, barX, barY, barX + BAR_WIDTH, barY + BAR_HEIGHT, radius, TRACK_TOP, TRACK_BOTTOM);

        // Filled portion.
        int filledWidth = (int) (BAR_WIDTH * manaPercent);
        if (filledWidth > 0) {
            double time = System.currentTimeMillis();

            int manaColor;
            if (manaPercent < 0.2f) {
                // Pulse the low-mana warning between dim and bright purple.
                float pulse = 0.4f + 0.6f * (float) (0.5 + 0.5 * Math.sin(time / LOW_PULSE_PERIOD_MS * Math.PI));
                manaColor = scaleBrightness(MANA_LOW_COLOR, pulse);
            } else {
                manaColor = lerpColor(MANA_COLOR_START, MANA_COLOR_END, manaPercent);
            }

            // Glassy vertical gradient: bright mint top, deep teal bottom.
            int topColor = scaleBrightness(manaColor, 1.35f);
            int botColor = scaleBrightness(manaColor, 0.6f);
            roundedGradient(graphics, barX, barY, barX + filledWidth, barY + BAR_HEIGHT, radius, topColor, botColor);

            // Top sheen line.
            int sheen = (scaleBrightness(manaColor, 1.6f) & 0x00FFFFFF) | 0x70000000;
            roundedRect(graphics, barX, barY, barX + filledWidth, barY + 1, radius, sheen);

            // Crisp leading edge (lavender-tinted).
            if (manaPercent < 1.0f && filledWidth >= 2) {
                graphics.fill(barX + filledWidth - 1, barY, barX + filledWidth, barY + BAR_HEIGHT, 0x90000000 | SHIMMER_TINT);
            }

            // Animated shimmer: a soft lavender band that sweeps across the fill.
            float sweep = (float) ((time / SHIMMER_PERIOD_MS) % 1.0);
            int fillRight = barX + filledWidth;
            int sweepCenter = barX + (int) (sweep * filledWidth);
            for (int dx = -SHIMMER_HALF; dx <= SHIMMER_HALF; dx++) {
                int sx = sweepCenter + dx;
                if (sx < barX || sx >= fillRight) continue;
                float edge = 1f - (Math.abs(dx) / (float) (SHIMMER_HALF + 1));
                int shimmerAlpha = (int) (0x66 * edge) << 24;
                graphics.fill(sx, barY, sx + 1, barY + BAR_HEIGHT, shimmerAlpha | SHIMMER_TINT);
            }
        }

        // Mana text
        String manaText = String.format("%.0f/%.0f", displayedMana, maxMana);
        int textWidth = mc.font.width(manaText);
        int textX = barX + (BAR_WIDTH - textWidth) / 2;
        int textY = barY - 9;

        // Shadow
        graphics.text(mc.font, manaText, textX + 1, textY + 1, 0x40000000, false);
        // Text
        graphics.text(mc.font, manaText, textX, textY, 0xFFCCDDFF, false);
    }

    private int lerpColor(int colorA, int colorB, float t) {
        return GuiTheme.lerpColor(colorA, colorB, t);
    }

    private int scaleBrightness(int color, float factor) {
        return GuiTheme.scaleBrightness(color, factor);
    }

    private void roundedRect(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, int radius, int color) {
        GuiTheme.roundedRect(graphics, x0, y0, x1, y1, radius, color);
    }

    private void roundedGradient(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, int radius, int topColor, int bottomColor) {
        GuiTheme.roundedGradient(graphics, x0, y0, x1, y1, radius, topColor, bottomColor);
    }
}
