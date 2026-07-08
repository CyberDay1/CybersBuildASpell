package buildaspell.client.gui;

import buildaspell.spell.ProjectileShape;
import buildaspell.spell.SpellVisual;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * Sub-screen of the Spell Builder for choosing a spell's projectile appearance:
 * color (or "use effect default"), procedural shape, and trail particle.
 * Only affects CAST / TRACKING projectile deliveries.
 */
public class SpellVisualScreen extends Screen {

    // Curated trail ids — must match SpellProjectileEntity#trailParticle.
    private static final String[] TRAILS = {
            "witch", "flame", "soul_fire_flame", "end_rod", "crit", "enchanted_hit",
            "electric_spark", "dragon_breath", "glow", "cherry", "smoke", "snowflake", "happy_villager"
    };

    // theme (mirrors SpellBuilderScreen)
    private static final int PANEL_BG     = 0xF20E2026;
    private static final int PANEL_BORDER = 0xFF3C6A74;
    private static final int HEADER       = 0xFFB8ECF0;
    private static final int TEXT         = 0xFFDCECEC;
    private static final int TEXT_DIM     = 0xFF7C9498;

    private final Screen parent;
    private final Consumer<SpellVisual> onDone;

    private int r, g, b;
    private boolean useDefault;
    private ProjectileShape shape;
    private int trailIndex;

    private RgbSlider rSlider, gSlider, bSlider;

    public SpellVisualScreen(Screen parent, SpellVisual initial, Consumer<SpellVisual> onDone) {
        super(Component.translatable("gui.buildaspell.spell_builder.visuals"));
        this.parent = parent;
        this.onDone = onDone;
        this.useDefault = !initial.hasExplicitColor();
        int c = initial.hasExplicitColor() ? initial.color() : 0x96C8FF; // 150,200,255
        this.r = (c >> 16) & 0xFF;
        this.g = (c >> 8) & 0xFF;
        this.b = c & 0xFF;
        this.shape = initial.shape();
        this.trailIndex = Math.max(0, indexOf(initial.trail()));
    }

    private static int indexOf(String trail) {
        for (int i = 0; i < TRAILS.length; i++) if (TRAILS[i].equals(trail)) return i;
        return 0;
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.width / 2;
        int top = this.height / 2 - 70;
        int fieldW = 200;
        int left = cx - fieldW / 2;

        rSlider = addRenderableWidget(new RgbSlider(left, top, fieldW, "R", r, v -> r = v));
        gSlider = addRenderableWidget(new RgbSlider(left, top + 22, fieldW, "G", g, v -> g = v));
        bSlider = addRenderableWidget(new RgbSlider(left, top + 44, fieldW, "B", b, v -> b = v));
        updateSliderState();

        addRenderableWidget(Button.builder(defaultToggleLabel(), btn -> {
            useDefault = !useDefault;
            btn.setMessage(defaultToggleLabel());
            updateSliderState();
        }).bounds(left, top + 70, fieldW, 18).build());

        addRenderableWidget(Button.builder(shapeLabel(), btn -> {
            ProjectileShape[] all = ProjectileShape.values();
            shape = all[(shape.ordinal() + 1) % all.length];
            btn.setMessage(shapeLabel());
        }).bounds(left, top + 92, fieldW, 18).build());

        addRenderableWidget(Button.builder(trailLabel(), btn -> {
            trailIndex = (trailIndex + 1) % TRAILS.length;
            btn.setMessage(trailLabel());
        }).bounds(left, top + 114, fieldW, 18).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.buildaspell.spell_builder.visuals_done"),
                btn -> { commit(); close(); }).bounds(cx - 102, top + 142, 100, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.buildaspell.spell_builder.visuals_reset"),
                btn -> { resetToDefault(); rebuildWidgets(); }).bounds(cx + 2, top + 142, 100, 18).build());
    }

    private void resetToDefault() {
        useDefault = true;
        r = 150; g = 200; b = 255;
        shape = ProjectileShape.CROSS;
        trailIndex = 0;
    }

    private void commit() {
        int color = useDefault ? SpellVisual.COLOR_DEFAULT : ((r << 16) | (g << 8) | b);
        onDone.accept(new SpellVisual(color, shape, TRAILS[trailIndex]));
    }

    private void close() {
        if (minecraft != null) minecraft.setScreenAndShow(parent);
    }

    private void updateSliderState() {
        rSlider.active = !useDefault;
        gSlider.active = !useDefault;
        bSlider.active = !useDefault;
    }

    private Component defaultToggleLabel() {
        Component state = useDefault
                ? Component.translatable("gui.buildaspell.spell_builder.visuals_color_effect")
                : Component.translatable("gui.buildaspell.spell_builder.visuals_color_custom");
        return Component.translatable("gui.buildaspell.spell_builder.visuals_color", state);
    }

    private Component shapeLabel() {
        return Component.translatable("gui.buildaspell.spell_builder.visuals_shape",
                Component.translatable("gui.buildaspell.shape." + shape.getSerializedName()));
    }

    private Component trailLabel() {
        return Component.translatable("gui.buildaspell.spell_builder.visuals_trail",
                Component.translatable("gui.buildaspell.trail." + TRAILS[trailIndex]));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        // Scrim over the parent screen (panels/content draw first, widgets layer on top via super).
        gfx.fill(0, 0, this.width, this.height, 0xC0101018);

        int cx = this.width / 2;
        int top = this.height / 2 - 70;

        // panel
        int px = cx - 130, py = top - 56, pw = 260, ph = 220;
        gfx.fill(px, py, px + pw, py + ph, PANEL_BG);
        drawOutline(gfx, px, py, pw, ph, PANEL_BORDER);

        gfx.centeredText(this.font, this.title.copy().withStyle(ChatFormatting.BOLD), cx, py + 6, HEADER);
        gfx.centeredText(this.font,
                Component.translatable("gui.buildaspell.spell_builder.visuals_note"), cx, py + 18, TEXT_DIM);

        // live color swatch + simple shape silhouette
        int previewColor = 0xFF000000 | (useDefault ? 0x96C8FF : ((r << 16) | (g << 8) | b));
        int swX = px + 12, swY = py + 30, swSize = 40;
        gfx.fill(swX, swY, swX + swSize, swY + swSize, 0xFF000000);
        drawShapePreview(gfx, swX, swY, swSize, previewColor);
        drawOutline(gfx, swX, swY, swSize, swSize, PANEL_BORDER);

        super.extractRenderState(gfx, mouseX, mouseY, partialTick);
    }

    /** 1px rectangle border via four fills (the extractor has no native outline). */
    private void drawOutline(GuiGraphicsExtractor gfx, int x, int y, int w, int h, int color) {
        gfx.fill(x, y, x + w, y + 1, color);
        gfx.fill(x, y + h - 1, x + w, y + h, color);
        gfx.fill(x, y, x + 1, y + h, color);
        gfx.fill(x + w - 1, y, x + w, y + h, color);
    }

    /** Lightweight 2D silhouette of the chosen shape, tinted by the chosen color. */
    private void drawShapePreview(GuiGraphicsExtractor gfx, int x, int y, int size, int color) {
        int cx = x + size / 2, cy = y + size / 2;
        switch (shape) {
            case CUBE -> gfx.fill(x + 8, y + 8, x + size - 8, y + size - 8, color);
            case RING -> {
                gfx.fill(x + 6, y + 6, x + size - 6, y + size - 6, color);
                gfx.fill(x + 14, y + 14, x + size - 14, y + size - 14, 0xFF000000);
            }
            case SPHERE -> {
                gfx.fill(x + 10, y + 6, x + size - 10, y + size - 6, color);
                gfx.fill(x + 6, y + 10, x + size - 6, y + size - 10, color);
            }
            default -> { // CROSS — a diagonal X using two thin bands
                for (int i = 0; i < size; i++) {
                    gfx.fill(x + i, y + i, x + i + 2, y + i + 2, color);
                    gfx.fill(x + i, y + size - i - 2, x + i + 2, y + size - i, color);
                }
            }
        }
    }

    @Override
    public void onClose() {
        close();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** 0–255 slider that writes through to a setter. */
    private static class RgbSlider extends AbstractSliderButton {
        private final String channel;
        private final java.util.function.IntConsumer setter;

        RgbSlider(int x, int y, int w, String channel, int initial, java.util.function.IntConsumer setter) {
            super(x, y, w, 18, Component.empty(), initial / 255.0);
            this.channel = channel;
            this.setter = setter;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal(channel + ": " + (int) Math.round(value * 255)).withStyle(ChatFormatting.WHITE));
        }

        @Override
        protected void applyValue() {
            setter.accept((int) Math.round(value * 255));
        }
    }
}
