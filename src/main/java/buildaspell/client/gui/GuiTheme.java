package buildaspell.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

/**
 * Shared visual language for Build a Spell's screens: the Spell Builder, the
 * Arcane Altar, the radial spell-select (V) menu, and the mana bar HUD.
 * <p>
 * One canonical teal-with-purple-accents palette plus the hand-drawn draw
 * helpers (rounded cards, gradient slots, amethyst corner brackets, arcane
 * motes) so every surface reads as the same UI. Everything here is static and
 * side-effect free — call it from any screen's render pass.
 */
public final class GuiTheme {
    private GuiTheme() {}

    // ── Canonical palette — teal obsidian with amethyst-purple accents ──
    /** Fully-opaque scrim: hides the vanilla menu blur regardless of the video setting. */
    public static final int SCRIM        = 0xFF132028;
    /** Panel body (top of the card gradient). */
    public static final int PANEL_TOP    = 0xF2153038;
    /** Panel body (bottom of the card gradient). */
    public static final int PANEL_BOT    = 0xF20A1518;
    /** Flat panel fill for surfaces that don't want a gradient. */
    public static final int PANEL_BG     = 0xF20E2026;
    /** Dark outer edge of the double frame. */
    public static final int BORDER       = 0xFF26464E;
    /** Inset highlight of the double frame (brighter teal). */
    public static final int BORDER_HI    = 0xFF3C6A74;
    /** Cyan-mint header text. */
    public static final int HEADER       = 0xFFB8ECF0;
    /** Signature amethyst-purple accent (corner brackets, active states). */
    public static final int AMETHYST     = 0xFFAE74E8;
    /** Deeper amethyst for slot frames. */
    public static final int AMETHYST_DIM = 0xFF7E54B8;
    /** Body text. */
    public static final int TEXT         = 0xFFDCECEC;
    /** De-emphasised text. */
    public static final int TEXT_DIM     = 0xFF7C9498;
    /** Recessed slot background. */
    public static final int SLOT_BG      = 0xFF06100F;
    /** Slot frame. */
    public static final int SLOT_BORDER  = 0xFF7E54B8;
    /** Translucent amethyst hover wash. */
    public static final int HOVER        = 0x50AE74E8;

    // ── Colour maths ────────────────────────────────────────────────────
    public static int lerpColor(int a, int b, float t) {
        int aA = (a >>> 24) & 0xFF, aR = (a >> 16) & 0xFF, aG = (a >> 8) & 0xFF, aB = a & 0xFF;
        int bA = (b >>> 24) & 0xFF, bR = (b >> 16) & 0xFF, bG = (b >> 8) & 0xFF, bB = b & 0xFF;
        int oA = (int) (aA + (bA - aA) * t);
        int oR = (int) (aR + (bR - aR) * t);
        int oG = (int) (aG + (bG - aG) * t);
        int oB = (int) (aB + (bB - aB) * t);
        return (oA << 24) | (oR << 16) | (oG << 8) | oB;
    }

    /** Scale RGB by {@code factor}, clamping each channel; alpha is preserved. */
    public static int scaleBrightness(int color, float factor) {
        int a = (color >>> 24) & 0xFF;
        int r = (int) Mth.clamp(((color >> 16) & 0xFF) * factor, 0, 255);
        int g = (int) Mth.clamp(((color >> 8) & 0xFF) * factor, 0, 255);
        int b = (int) Mth.clamp((color & 0xFF) * factor, 0, 255);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int lightenColor(int color, float factor) { return scaleBrightness(color, factor); }
    public static int darkenColor(int color, float factor)  { return scaleBrightness(color, factor); }

    // ── Panels & frames ─────────────────────────────────────────────────
    /** Vertical gradient via 2px fill bands (no native gradient in the screen). */
    public static void fillVGradient(GuiGraphicsExtractor g, int x, int y, int w, int h, int top, int bot) {
        for (int i = 0; i < h; i += 2) {
            float t = h <= 1 ? 0f : (float) i / (h - 1);
            g.fill(x, y + i, x + w, Math.min(y + h, y + i + 2), lerpColor(top, bot, t));
        }
    }

    /** Amethyst L-brackets in each corner — the arcane "frame" accent. */
    public static void drawCorners(GuiGraphicsExtractor g, int x, int y, int w, int h, int color, int len) {
        g.fill(x, y, x + len, y + 1, color);                 g.fill(x, y, x + 1, y + len, color);
        g.fill(x + w - len, y, x + w, y + 1, color);         g.fill(x + w - 1, y, x + w, y + len, color);
        g.fill(x, y + h - 1, x + len, y + h, color);         g.fill(x, y + h - len, x + 1, y + h, color);
        g.fill(x + w - len, y + h - 1, x + w, y + h, color); g.fill(x + w - 1, y + h - len, x + w, y + h, color);
    }

    /**
     * The signature "card": soft offset drop-shadow, teal vertical gradient body,
     * double frame (dark outer + teal inset highlight), amethyst corner brackets.
     */
    public static void drawPanel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        // elevation: layered offset drop-shadow
        g.fill(x + 5, y + 5, x + w + 5, y + h + 5, 0x22000000);
        g.fill(x + 3, y + 3, x + w + 4, y + h + 4, 0x33000000);
        // gradient body
        fillVGradient(g, x, y, w, h, PANEL_TOP, PANEL_BOT);
        // double frame
        g.fill(x, y, x + w, y + 1, BORDER);
        g.fill(x, y + h - 1, x + w, y + h, BORDER);
        g.fill(x, y, x + 1, y + h, BORDER);
        g.fill(x + w - 1, y, x + w, y + h, BORDER);
        g.fill(x + 2, y + 2, x + w - 2, y + 3, BORDER_HI);
        g.fill(x + 2, y + h - 3, x + w - 2, y + h - 2, BORDER_HI);
        g.fill(x + 2, y + 2, x + 3, y + h - 2, BORDER_HI);
        g.fill(x + w - 3, y + 2, x + w - 2, y + h - 2, BORDER_HI);
        // amethyst corner brackets
        drawCorners(g, x, y, w, h, 0xC0AE74E8, 6);
    }

    // ── Slots ───────────────────────────────────────────────────────────
    /** A recessed square slot with an accent top edge; washes amethyst on hover. */
    public static void drawSlot(GuiGraphicsExtractor g, int x, int y, int size, int accent, boolean hovered) {
        g.fill(x, y, x + size, y + size, SLOT_BG);
        g.fill(x, y, x + size, y + 1, SLOT_BORDER);
        g.fill(x, y + size - 1, x + size, y + size, SLOT_BORDER);
        g.fill(x, y, x + 1, y + size, SLOT_BORDER);
        g.fill(x + size - 1, y, x + size, y + size, SLOT_BORDER);
        g.fill(x + 1, y + 1, x + size - 1, y + 2, accent);
        if (hovered) g.fill(x, y, x + size, y + size, HOVER);
    }

    // ── Rounded shapes (for the mana bar capsule & radial cells) ─────────
    public static void roundedRect(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int radius, int color) {
        roundedGradient(g, x0, y0, x1, y1, radius, color, color);
    }

    /** A rounded-corner rectangle filled with a top→bottom gradient. */
    public static void roundedGradient(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int radius, int topColor, int bottomColor) {
        int h = y1 - y0;
        if (h <= 0 || x1 <= x0) return;
        int r = Math.min(radius, Math.min(h / 2, (x1 - x0) / 2));
        for (int y = y0; y < y1; y++) {
            float t = h <= 1 ? 0f : (float) (y - y0) / (h - 1);
            int color = lerpColor(topColor, bottomColor, t);
            int d = Math.min(y - y0, (y1 - 1) - y);
            int inset = 0;
            if (d < r) {
                int k = r - d;
                inset = r - (int) Math.round(Math.sqrt((double) r * r - (double) k * k));
            }
            g.fill(x0 + inset, y, x1 - inset, y + 1, color);
        }
    }

    // ── Arcane motes ────────────────────────────────────────────────────
    /** A few accent motes that rise and fade inside a filled slot — subtle arcane shimmer. */
    public static void drawSlotParticles(GuiGraphicsExtractor g, int x, int y, int size, int accent, int seed) {
        long t = System.currentTimeMillis();
        int motes = 5;
        int rgb = accent & 0x00FFFFFF;
        for (int i = 0; i < motes; i++) {
            double period = 1200.0 + ((seed * 31 + i * 97) & 0x1FF);
            double phase  = ((seed * 53 + i * 131) & 0x3FF) / 1024.0;
            float p = (float) ((((t / period) + phase) % 1.0 + 1.0) % 1.0);
            int px = x + 2 + ((seed * 17 + i * 53) % (size - 4));
            px += (int) Math.round(Math.sin(t / 260.0 + i * 2.1) * 2.0);
            int py = y + size - 1 - Math.round(p * (size + 2));
            float fade = p < 0.18f ? p / 0.18f : (p > 0.72f ? (1f - p) / 0.28f : 1f);
            int alpha = Math.min(255, (int) (fade * 235f));
            if (alpha <= 6) continue;
            g.fill(px, py, px + 2, py + 2, (alpha << 24) | rgb);
            int core = Math.min(255, (int) (fade * 255f));
            g.fill(px, py, px + 1, py + 1, (core << 24) | 0xFFFFFF);
        }
    }
}
