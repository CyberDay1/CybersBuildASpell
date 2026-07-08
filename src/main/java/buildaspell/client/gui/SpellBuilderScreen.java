package buildaspell.client.gui;

import buildaspell.client.ClientComponentRegistry;
import buildaspell.config.ModConfig;
import buildaspell.network.SaveSpellPacket;
import buildaspell.network.SyncComponentRegistryPacket;
import buildaspell.spell.*;
import buildaspell.spell.data.ComponentDisplay;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Spell crafting screen — arcane-themed, icon-driven, drag-and-drop builder.
 * <p>
 * Geometry is computed once per frame into a flat {@link SlotBox} list that
 * drives both rendering and hit-testing, so drop-zone resolution, reordering,
 * and removal all read from the same source of truth.
 */
public class SpellBuilderScreen extends Screen {

    // ── Spell chain model ──────────────────────────────────────────────
    private static class EffectGroup {
        SpellEffect effect;            // null => datapack-authored effect
        DatapackEffect dataEffect;     // null => built-in enum effect
        List<SpellModifier> modifiers = new ArrayList<>();
        EffectGroup(SpellEffect effect) { this.effect = effect; }
        EffectGroup(DatapackEffect dataEffect) { this.dataEffect = dataEffect; }
        EffectGroup(EffectGroup other) {
            this.effect = other.effect;
            this.dataEffect = other.dataEffect;
            this.modifiers = new ArrayList<>(other.modifiers);
        }
        /** The palette-style handle (a {@link SpellEffect} or {@link DatapackEffect}) for icon/name/cost. */
        Object component() { return effect != null ? effect : dataEffect; }
    }

    /** Rebuild the ordered effect-group chain from a spell's components (enum + datapack effects). */
    private static List<EffectGroup> groupsFromComponents(List<SpellComponent> comps) {
        List<EffectGroup> groups = new ArrayList<>();
        EffectGroup current = null;
        for (SpellComponent c : comps) {
            if (c instanceof SpellComponent.Effect e) {
                current = new EffectGroup(e.effect());
                groups.add(current);
            } else if (c instanceof SpellComponent.DataEffect de) {
                current = new EffectGroup(datapackHandle(de.effectId()));
                groups.add(current);
            } else if (c instanceof SpellComponent.Modifier m && current != null) {
                current.modifiers.add(m.modifier());
            }
        }
        return groups;
    }

    /** Resolve a saved datapack-effect id back to a palette handle, pulling display from the synced cache. */
    private static DatapackEffect datapackHandle(Identifier id) {
        for (SyncComponentRegistryPacket.Entry e : ClientComponentRegistry.effects()) {
            if (e.id().equals(id)) return new DatapackEffect(id, e.display());
        }
        return new DatapackEffect(id, ComponentDisplay.EMPTY);
    }

    /** Split a spell's delivery-level (roster) modifiers out for the editor's Delivery zone chips. */
    private static List<SpellModifier> deliveryModifiersFromSpell(Spell spell) {
        return new ArrayList<>(spell.getDeliveryModifiers());
    }

    private enum SlotKind { DELIVERY, DELIVERY_MODIFIER, ADD_DELIVERY_MODIFIER, EFFECT, MODIFIER, ADD_MODIFIER, ADD_EFFECT }

    /** A positioned slot in the build chain; effectIndex/modIndex are -1 when N/A. */
    private record SlotBox(SlotKind kind, int effectIndex, int modIndex, int x, int y, int size) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + size && my >= y && my < y + size;
        }
    }

    /** A row in the grouped palette: a section header (component == null) or a draggable component. */
    private record PalRow(Object component, Component header, int accent, int y, int h) {
        boolean isHeader() { return component == null; }
    }

    // ── Theme (cyber-arcane: teal obsidian panels, mint-cyan + steel) ──
    private static final int SCRIM            = 0xFF132028;   // fully opaque: hides vanilla menu-blur regardless of video setting
    private static final int PANEL_BG         = 0xF20E2026;
    private static final int PANEL_BORDER     = 0xFF26464E;
    private static final int PANEL_BORDER_HI  = 0xFF3C6A74;
    private static final int HEADER           = 0xFFB8ECF0;
    private static final int ACCENT_GOLD      = 0xFFAE74E8;   // signature amethyst-purple accent
    private static final int TEXT             = 0xFFDCECEC;
    private static final int TEXT_DIM         = 0xFF7C9498;
    private static final int SLOT_BG          = 0xFF06100F;
    private static final int SLOT_BORDER      = 0xFF7E54B8;
    private static final int SLOT_HOVER       = 0x50AE74E8;
    private static final int DELIVERY_ACCENT  = 0xFF52A6E6;
    private static final int EFFECT_ACCENT    = 0xFFE6885A;
    private static final int MODIFIER_ACCENT  = 0xFF86E05A;
    private static final int WARN_OUTLINE     = 0xFFE0A93A;   // amber: a placed chip that does nothing under the current delivery

    private static final int SLOT      = 22;
    private static final int ROW_H     = 40;
    private static final int PAL_ROW_H = 20;   // palette component row (18 icon + gap)
    private static final int PAL_HDR_H = 13;   // palette section-header row
    private static final int PAL_PAD_R = 9;    // right padding reserved for the scrollbar

    // ── Layout (computed in init) ──────────────────────────────────────
    private int palX, palY, palW, palH;
    private int buildX, buildY, buildW, buildH;
    private int slotPaneX, slotPaneY, slotPaneW;

    // ── State ──────────────────────────────────────────────────────────
    private final PlayerSpellData spellData;
    private final PlayerSpellSlots spellSlots;
    private int selectedSlot = 0;
    private EditBox nameField;
    private EditBox searchField;
    private Button saveButton;

    private DeliveryMethod selectedDelivery = null;
    private SpellVisual selectedVisual = SpellVisual.DEFAULT;
    /** Delivery-level (roster) modifiers pinned to the Delivery zone; repeats = stack count. */
    private List<SpellModifier> deliveryModifiers = new ArrayList<>();
    private List<EffectGroup> effectGroups = new ArrayList<>();
    private final List<Object> availableComponents = new ArrayList<>();
    private final List<Object> filteredComponents = new ArrayList<>();

    private int paletteScroll = 0;
    private int buildScrollY = 0;

    // drag state
    private Object dragging = null;
    private boolean dragFromBuild = false;
    private double pressX, pressY;
    private SlotBox pressedBuildBox = null;   // build-slot press pending click(select)-vs-drag(pickup) resolution

    /** Effect group currently selected in the build chain; click-added modifiers target it. -1 = none. */
    private int selectedEffectIndex = -1;

    private boolean comboAligned;

    /**
     * True once this screen instance has performed its one-time "open on slot 1" load.
     * <p>
     * {@code init()} runs again every time the screen is re-laid-out — returning from a sub-screen
     * (Visuals, Import) goes through {@code Screen#init(int,int)} → {@code repositionElements()} →
     * {@code rebuildWidgets()} → {@code init()}, and so does a window resize or fullscreen toggle via
     * {@code Screen#resize}. Widgets must be rebuilt on every one of those passes, but the build state
     * must not: re-running the slot-1 load would throw away the in-progress spell.
     */
    private boolean autoLoaded;

    // cross-open persistence
    private static DeliveryMethod lastDelivery = null;
    private static List<SpellModifier> lastDeliveryModifiers = null;
    private static List<EffectGroup> lastEffectGroups = null;
    private static String lastSpellName = "";
    private static SpellVisual lastVisual = SpellVisual.DEFAULT;

    // Set when an imported spell was staged for the next construction, so init() knows not to
    // clobber it with the default slot-1 auto-load.
    private static boolean pendingImport = false;

    public static void clearStaticState() {
        lastDelivery = null;
        lastDeliveryModifiers = null;
        lastEffectGroups = null;
        lastSpellName = "";
        lastVisual = SpellVisual.DEFAULT;
    }

    public static void loadImportedSpell(Spell spell) {
        lastDelivery = spell.getDelivery();
        lastDeliveryModifiers = deliveryModifiersFromSpell(spell);
        lastSpellName = "";
        lastVisual = spell.getVisual();
        lastEffectGroups = groupsFromComponents(spell.getComponents());
        pendingImport = true;
    }

    public SpellBuilderScreen(PlayerSpellData spellData, PlayerSpellSlots spellSlots) {
        super(Component.translatable("gui.buildaspell.spell_builder.title"));
        this.spellData = spellData;
        this.spellSlots = spellSlots;

        // Only show components that are both unlocked AND enabled by the (synced) server config.
        for (DeliveryMethod m : spellData.getUnlockedDeliveryMethods())
            if (ModConfig.isDeliveryEnabled(m)) availableComponents.add(m);
        for (SpellEffect e : spellData.getUnlockedEffects())
            if (ModConfig.isEffectEnabled(e)) availableComponents.add(e);
        for (SpellModifier m : spellData.getUnlockedModifiers())
            if (ModConfig.isModifierEnabled(m)) availableComponents.add(m);

        // Datapack-authored effects bypass the unlock progression — always available.
        for (SyncComponentRegistryPacket.Entry e : ClientComponentRegistry.effects())
            availableComponents.add(new DatapackEffect(e.id(), e.display()));

        if (lastDelivery != null) selectedDelivery = lastDelivery;
        if (lastDeliveryModifiers != null) deliveryModifiers = new ArrayList<>(lastDeliveryModifiers);
        if (lastVisual != null) selectedVisual = lastVisual;
        if (lastEffectGroups != null) {
            effectGroups = new ArrayList<>();
            for (EffectGroup g : lastEffectGroups) effectGroups.add(new EffectGroup(g));
        }
    }

    @Override
    protected void init() {
        super.init();

        // Carry the live widget state across a re-init. On the first pass the widgets don't exist yet,
        // so the name falls back to the cross-open static; on later passes it comes from what the
        // player has actually typed, which must survive a sub-screen round trip or a window resize.
        String carriedName = nameField != null ? nameField.getValue() : lastSpellName;
        String carriedSearch = searchField != null ? searchField.getValue() : "";
        int carriedPaletteScroll = paletteScroll;

        int margin = 8;
        palX = margin;
        palY = 44;
        palW = 150;
        palH = this.height - palY - 12;

        slotPaneW = 116;
        slotPaneX = this.width - slotPaneW - margin;
        slotPaneY = 44;

        buildX = palX + palW + 10;
        buildY = 44;
        buildW = Math.max(160, slotPaneX - buildX - 10);
        buildH = this.height - buildY - 40;

        // name field (top centre)
        nameField = new EditBox(this.font, this.width / 2 - 90, 20, 180, 18,
                Component.translatable("gui.buildaspell.spell_builder.spell_name"));
        nameField.setMaxLength(32);
        nameField.setHint(Component.translatable("gui.buildaspell.spell_builder.name_hint")
                .withStyle(ChatFormatting.DARK_GRAY));
        nameField.setValue(carriedName);
        addRenderableWidget(nameField);

        // search field (top of palette)
        searchField = new EditBox(this.font, palX + 2, palY - 22, palW - 4, 16,
                Component.translatable("gui.buildaspell.spell_builder.search"));
        searchField.setMaxLength(32);
        searchField.setHint(Component.translatable("gui.buildaspell.spell_builder.search_hint")
                .withStyle(ChatFormatting.DARK_GRAY));
        searchField.setResponder(s -> updateFilteredComponents());
        if (!carriedSearch.isEmpty()) searchField.setValue(carriedSearch);
        addRenderableWidget(searchField);

        updateFilteredComponents();
        // updateFilteredComponents zeroes the palette scroll; the filter text is unchanged across a
        // re-init, so put the player's scroll position back. Render clamps it if the pane got shorter.
        paletteScroll = carriedPaletteScroll;
        buildScrollY = Math.max(0, Math.min(maxBuildScroll(), buildScrollY));

        // bottom action bar
        int by = this.height - 26;
        int bw = 40, gap = 3;
        int totalW = bw * 8 + gap * 7;
        int bx = buildX + (buildW - totalW) / 2;
        saveButton = addRenderableWidget(Button.builder(Component.translatable("gui.buildaspell.save"),
                b -> saveSpell()).bounds(bx, by, bw, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.buildaspell.clear"),
                b -> clearSpell()).bounds(bx + (bw + gap), by, bw, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.buildaspell.load"),
                b -> loadSelectedSlot()).bounds(bx + 2 * (bw + gap), by, bw, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.buildaspell.spell_builder.visuals"),
                b -> openVisuals()).bounds(bx + 3 * (bw + gap), by, bw, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.buildaspell.export"),
                b -> exportSpell()).bounds(bx + 4 * (bw + gap), by, bw, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.buildaspell.import"),
                b -> importSpell()).bounds(bx + 5 * (bw + gap), by, bw, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.buildaspell.copy"),
                b -> copySpellCode()).bounds(bx + 6 * (bw + gap), by, bw, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.buildaspell.spell_builder.to_power"),
                b -> exportToPower()).bounds(bx + 7 * (bw + gap), by, bw, 18).build());

        // Default view: highlight slot 1 and load its spell so the book opens on a ready-to-cast
        // page. An import staged just before construction keeps its spell instead of being replaced.
        // This runs on the FIRST init only: every later pass is a widget rebuild (sub-screen return,
        // window resize, fullscreen toggle) and must leave the in-progress build alone.
        if (!autoLoaded) {
            autoLoaded = true;
            if (pendingImport) {
                pendingImport = false;
            } else {
                selectAndLoadSlot(0);
            }
        }
    }

    private void openVisuals() {
        if (minecraft == null) return;
        minecraft.setScreen(new SpellVisualScreen(this, selectedVisual, v -> selectedVisual = v));
    }

    // ── Build-chain geometry ───────────────────────────────────────────
    private List<SlotBox> buildLayout() {
        List<SlotBox> boxes = new ArrayList<>();
        int colX = buildX + 14;
        int top = buildY + 24 - buildScrollY;

        boxes.add(new SlotBox(SlotKind.DELIVERY, -1, -1, colX, top, SLOT));

        // Delivery-level modifier chips sit to the RIGHT of the delivery slot on the same row, exactly
        // like effect modifiers sitting beside their effect. modIndex carries the chip's index into
        // deliveryModifiers.
        int delModX = colX + SLOT + 16;
        for (int j = 0; j < deliveryModifiers.size(); j++) {
            boxes.add(new SlotBox(SlotKind.DELIVERY_MODIFIER, -1, j, delModX, top, SLOT));
            delModX += SLOT + 8;
        }
        boxes.add(new SlotBox(SlotKind.ADD_DELIVERY_MODIFIER, -1, -1, delModX, top, SLOT));

        for (int i = 0; i < effectGroups.size(); i++) {
            int ey = top + (i + 1) * ROW_H;
            boxes.add(new SlotBox(SlotKind.EFFECT, i, -1, colX, ey, SLOT));

            int modX = colX + SLOT + 16;
            EffectGroup g = effectGroups.get(i);
            for (int j = 0; j < g.modifiers.size(); j++) {
                boxes.add(new SlotBox(SlotKind.MODIFIER, i, j, modX, ey, SLOT));
                modX += SLOT + 8;
            }
            boxes.add(new SlotBox(SlotKind.ADD_MODIFIER, i, -1, modX, ey, SLOT));
        }

        int addY = top + (effectGroups.size() + 1) * ROW_H;
        boxes.add(new SlotBox(SlotKind.ADD_EFFECT, -1, -1, colX, addY, SLOT));
        return boxes;
    }

    private int maxBuildScroll() {
        int contentH = (effectGroups.size() + 2) * ROW_H + 34;
        return Math.max(0, contentH - buildH);
    }

    // ── Render ─────────────────────────────────────────────────────────
    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        g.fill(0, 0, width, height, SCRIM);

        comboAligned = isComboAligned();
        SpellCombo combo = SpellCombo.detect(buildSpellFromSlots());

        // title
        Component title = this.title.copy().withStyle(ChatFormatting.BOLD);
        g.text(font, title, width / 2 - font.width(title) / 2, 8, HEADER, false);

        renderPalette(g, mouseX, mouseY);
        renderBuildArea(g, mouseX, mouseY, combo);
        renderSlotPane(g, mouseX, mouseY);
        renderInfoBar(g, combo);

        super.extractRenderState(g, mouseX, mouseY, partial);

        // drag ghost
        if (dragging != null) {
            drawIcon(g, dragging, (int) mouseX - 9, (int) mouseY - 9);
        }

        // tooltip for hovered build/palette component
        if (dragging == null) {
            boolean deliveryWarn = false;
            Object hovered = paletteComponentAt(mouseX, mouseY);
            if (hovered != null) {
                deliveryWarn = isPaletteEntryDisabled(hovered);
            } else {
                SlotBox box = slotBoxAt(mouseX, mouseY);
                if (box != null) {
                    hovered = componentInBox(box);
                    // A placed delivery-modifier chip that's inert under the current delivery.
                    if (box.kind() == SlotKind.DELIVERY_MODIFIER && hovered instanceof SpellModifier m) {
                        deliveryWarn = !ModifierApplicability.isDeliveryModifierUseful(m, selectedDelivery);
                    }
                }
            }
            if (hovered != null) renderComponentTooltip(g, hovered, mouseX, mouseY, deliveryWarn);
        }
    }

    private void drawPanel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        GuiTheme.drawPanel(g, x, y, w, h);
    }

    private static int lerpColor(int a, int b, float t) {
        return GuiTheme.lerpColor(a, b, t);
    }

    private void drawSlot(GuiGraphicsExtractor g, int x, int y, int size, int accent, boolean hovered) {
        GuiTheme.drawSlot(g, x, y, size, accent, hovered);
    }

    /** Build the ordered palette rows: a header + its members for each non-empty type section. */
    private List<PalRow> buildPaletteRows() {
        List<PalRow> rows = new ArrayList<>();
        int y = 0;
        y = addPaletteSection(rows, y, "palette_delivery", DELIVERY_ACCENT, c -> c instanceof DeliveryMethod);
        y = addPaletteSection(rows, y, "palette_delivery_modifiers", DELIVERY_ACCENT,
                c -> c instanceof SpellModifier m && ModifierApplicability.isDeliveryLevel(m));
        y = addPaletteSection(rows, y, "palette_effects", EFFECT_ACCENT,
                c -> c instanceof SpellEffect || c instanceof DatapackEffect);
        addPaletteSection(rows, y, "palette_modifiers", MODIFIER_ACCENT,
                c -> c instanceof SpellModifier m && !ModifierApplicability.isDeliveryLevel(m));
        return rows;
    }

    private int addPaletteSection(List<PalRow> rows, int y, String key, int accent, Predicate<Object> type) {
        List<Object> members = new ArrayList<>();
        for (Object c : filteredComponents) if (type.test(c)) members.add(c);
        if (members.isEmpty()) return y;
        rows.add(new PalRow(null, Component.translatable("gui.buildaspell.spell_builder." + key), accent, y, PAL_HDR_H));
        y += PAL_HDR_H;
        for (Object c : members) {
            rows.add(new PalRow(c, null, accent, y, PAL_ROW_H));
            y += PAL_ROW_H;
        }
        return y + 4; // gap before the next section
    }

    private int paletteContentH(List<PalRow> rows) {
        if (rows.isEmpty()) return 0;
        PalRow last = rows.get(rows.size() - 1);
        return last.y() + last.h();
    }

    private void renderPalette(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        drawPanel(g, palX, palY, palW, palH);
        int innerX = palX + 4;
        int innerY = palY + 4;
        int viewH = palH - 8;
        int rightX = palX + palW - PAL_PAD_R;   // content edge (scrollbar lives to the right)

        List<PalRow> rows = buildPaletteRows();
        int contentH = paletteContentH(rows);
        int maxScroll = Math.max(0, contentH - viewH);
        if (paletteScroll > maxScroll) paletteScroll = maxScroll;
        if (paletteScroll < 0) paletteScroll = 0;

        g.enableScissor(palX + 1, palY + 1, palX + palW - 1, palY + palH - 1);
        for (PalRow r : rows) {
            int ry = innerY + r.y() - paletteScroll;
            if (ry + r.h() < palY || ry > palY + palH) continue;
            if (r.isHeader()) {
                // accent tab + label (chip-style header)
                g.fill(innerX, ry + 2, innerX + 3, ry + r.h() - 1, r.accent());
                g.text(font, r.header().copy().withStyle(ChatFormatting.BOLD), innerX + 8, ry + 3, r.accent(), false);
                g.fill(innerX, ry + r.h() - 1, rightX, ry + r.h(), PANEL_BORDER);
            } else {
                boolean disabled = isPaletteEntryDisabled(r.component());
                boolean hovered = mouseX >= innerX && mouseX < rightX && mouseY >= ry && mouseY < ry + r.h() - 1;
                if (hovered && !disabled) g.fill(innerX, ry, rightX, ry + r.h() - 1, SLOT_HOVER);
                drawSlot(g, innerX, ry, 18, disabled ? TEXT_DIM : r.accent(), false);
                drawIcon(g, r.component(), innerX + 1, ry + 1);
                drawNameFit(g, componentName(r.component()), innerX + 22, ry + 5, rightX - (innerX + 22),
                        disabled ? TEXT_DIM : TEXT);
                if (disabled) {
                    // grey scrim over the row to read as unavailable under the current delivery
                    g.fill(innerX, ry, rightX, ry + r.h() - 1, 0x99101A18);
                }
            }
        }
        g.disableScissor();

        // modern scrollbar: thin track + accent thumb
        if (maxScroll > 0) {
            int barX = palX + palW - 5;
            int trackY = palY + 4;
            g.fill(barX, trackY, barX + 3, trackY + viewH, 0x30000000);
            int thumbH = Math.max(18, (int) ((float) viewH / contentH * viewH));
            int thumbY = trackY + (int) ((float) paletteScroll / maxScroll * (viewH - thumbH));
            g.fill(barX, thumbY, barX + 3, thumbY + thumbH, ACCENT_GOLD);
        }

        if (filteredComponents.isEmpty()) {
            Component none = Component.translatable("gui.buildaspell.spell_builder.no_results");
            g.text(font, none, palX + 6, palY + 8, TEXT_DIM, false);
        }
    }

    private void drawNameFit(GuiGraphicsExtractor g, Component name, int x, int y, int maxW, int color) {
        if (font.width(name) <= maxW) {
            g.text(font, name, x, y, color, false);
        } else {
            String s = font.plainSubstrByWidth(name.getString(), maxW - 6) + "…";
            g.text(font, Component.literal(s), x, y, color, false);
        }
    }

    private void renderBuildArea(GuiGraphicsExtractor g, int mouseX, int mouseY, SpellCombo combo) {
        drawPanel(g, buildX, buildY, buildW, buildH);

        if (combo != null) {
            String name = prettyName(combo.getId());
            Component c = Component.literal("✦ " + name).withStyle(ChatFormatting.BOLD);
            g.text(font, c, buildX + buildW - font.width(c) - 8, buildY + 4, ACCENT_GOLD, false);
        }

        g.enableScissor(buildX + 1, buildY + 1, buildX + buildW - 1, buildY + buildH - 1);
        List<SlotBox> boxes = buildLayout();

        // connector lines (delivery → effects → add-effect, down the spine)
        int spineX = buildX + 14 + SLOT / 2;
        SlotBox prev = null;
        for (SlotBox b : boxes) {
            if (b.kind() == SlotKind.EFFECT || b.kind() == SlotKind.ADD_EFFECT) {
                if (prev != null) {
                    g.fill(spineX, prev.y() + SLOT, spineX + 1, b.y(), PANEL_BORDER_HI);
                }
                prev = b;
            } else if (b.kind() == SlotKind.DELIVERY) {
                prev = b;
            } else if (b.kind() == SlotKind.MODIFIER || b.kind() == SlotKind.ADD_MODIFIER
                    || b.kind() == SlotKind.DELIVERY_MODIFIER || b.kind() == SlotKind.ADD_DELIVERY_MODIFIER) {
                // horizontal stub from the delivery/effect slot to the chip
                g.fill(b.x() - 16, b.y() + SLOT / 2, b.x(), b.y() + SLOT / 2 + 1, PANEL_BORDER_HI);
            }
        }

        // labels + slots
        for (SlotBox b : boxes) {
            boolean hovered = b.contains(mouseX, mouseY);
            boolean isDropZone = dragging != null && acceptsDrag(b, dragging);
            switch (b.kind()) {
                case DELIVERY -> {
                    g.text(font, Component.translatable("gui.buildaspell.spell_builder.delivery"),
                            b.x(), b.y() - 10, DELIVERY_ACCENT, false);
                    drawSlot(g, b.x(), b.y(), SLOT, DELIVERY_ACCENT, hovered);
                    if (selectedDelivery != null) {
                        drawIcon(g, selectedDelivery, b.x() + 3, b.y() + 3);
                        drawSlotParticles(g, b.x(), b.y(), SLOT, DELIVERY_ACCENT, 1);
                    } else {
                        drawPlus(g, b.x(), b.y(), DELIVERY_ACCENT);
                    }
                }
                case DELIVERY_MODIFIER -> {
                    SpellModifier chipMod = deliveryModifiers.get(b.modIndex());
                    boolean invalid = !ModifierApplicability.isDeliveryModifierUseful(chipMod, selectedDelivery);
                    drawSlot(g, b.x(), b.y(), SLOT, MODIFIER_ACCENT, hovered);
                    drawIcon(g, chipMod, b.x() + 3, b.y() + 3);
                    drawSlotParticles(g, b.x(), b.y(), SLOT, MODIFIER_ACCENT, 2000 + b.modIndex());
                    if (invalid) {
                        // Placed chip that does nothing under the current delivery: keep it (don't
                        // destroy the player's work) but flag it with an amber warning outline.
                        int x0 = b.x() - 1, y0 = b.y() - 1, x1 = b.x() + b.size() + 1, y1 = b.y() + b.size() + 1;
                        g.fill(x0, y0, x1, y0 + 1, WARN_OUTLINE);
                        g.fill(x0, y1 - 1, x1, y1, WARN_OUTLINE);
                        g.fill(x0, y0, x0 + 1, y1, WARN_OUTLINE);
                        g.fill(x1 - 1, y0, x1, y1, WARN_OUTLINE);
                    }
                }
                case ADD_DELIVERY_MODIFIER -> {
                    drawSlot(g, b.x(), b.y(), SLOT, MODIFIER_ACCENT, hovered);
                    drawPlus(g, b.x(), b.y(), MODIFIER_ACCENT);
                }
                case EFFECT -> {
                    drawSlot(g, b.x(), b.y(), SLOT, EFFECT_ACCENT, hovered);
                    drawIcon(g, effectGroups.get(b.effectIndex()).component(), b.x() + 3, b.y() + 3);
                    drawSlotParticles(g, b.x(), b.y(), SLOT, EFFECT_ACCENT, 100 + b.effectIndex());
                    if (b.effectIndex() == selectedEffectIndex) {
                        int x0 = b.x() - 2, y0 = b.y() - 2, x1 = b.x() + b.size() + 2, y1 = b.y() + b.size() + 2;
                        g.fill(x0, y0, x1, y0 + 1, HEADER);
                        g.fill(x0, y1 - 1, x1, y1, HEADER);
                        g.fill(x0, y0, x0 + 1, y1, HEADER);
                        g.fill(x1 - 1, y0, x1, y1, HEADER);
                    }
                }
                case MODIFIER -> {
                    drawSlot(g, b.x(), b.y(), SLOT, MODIFIER_ACCENT, hovered);
                    drawIcon(g, effectGroups.get(b.effectIndex()).modifiers.get(b.modIndex()), b.x() + 3, b.y() + 3);
                    drawSlotParticles(g, b.x(), b.y(), SLOT, MODIFIER_ACCENT, 1000 + b.effectIndex() * 16 + b.modIndex());
                }
                case ADD_MODIFIER -> {
                    drawSlot(g, b.x(), b.y(), SLOT, MODIFIER_ACCENT, hovered);
                    drawPlus(g, b.x(), b.y(), MODIFIER_ACCENT);
                }
                case ADD_EFFECT -> {
                    g.text(font, Component.translatable("gui.buildaspell.spell_builder.add_effect"),
                            b.x(), b.y() - 10, EFFECT_ACCENT, false);
                    drawSlot(g, b.x(), b.y(), SLOT, EFFECT_ACCENT, hovered);
                    drawPlus(g, b.x(), b.y(), EFFECT_ACCENT);
                }
            }
            if (isDropZone) {
                int c = hovered ? 0x66FFFFFF : 0x33FFFFFF;
                g.fill(b.x(), b.y(), b.x() + b.size(), b.y() + b.size(), c);
            }
            if (comboAligned && (b.kind() == SlotKind.EFFECT || b.kind() == SlotKind.MODIFIER)) {
                g.fill(b.x() - 1, b.y() - 1, b.x() + b.size() + 1, b.y(), ACCENT_GOLD);
                g.fill(b.x() - 1, b.y() + b.size(), b.x() + b.size() + 1, b.y() + b.size() + 1, ACCENT_GOLD);
            }
        }
        g.disableScissor();
    }

    /** Subtle arcane shimmer: a few accent motes that rise and fade within a filled slot. */
    private void drawSlotParticles(GuiGraphicsExtractor g, int x, int y, int size, int accent, int seed) {
        GuiTheme.drawSlotParticles(g, x, y, size, accent, seed);
    }

    private void renderSlotPane(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int paneH = this.height - slotPaneY - 12;
        drawPanel(g, slotPaneX, slotPaneY, slotPaneW, paneH);
        g.text(font, Component.translatable("gui.buildaspell.spell_builder.slots_header"),
                slotPaneX + 6, slotPaneY + 4, HEADER, false);

        int rowH = 16, top = slotPaneY + 16;
        for (int i = 0; i < 10; i++) {
            int y = top + i * rowH;
            boolean selected = i == selectedSlot;
            boolean hovered = mouseX >= slotPaneX + 3 && mouseX < slotPaneX + slotPaneW - 3
                    && mouseY >= y && mouseY < y + rowH - 2;
            int bg = selected ? 0xFF173238 : (hovered ? 0xFF12262B : 0xFF0A171A);
            g.fill(slotPaneX + 3, y, slotPaneX + slotPaneW - 3, y + rowH - 2, bg);
            if (selected) g.fill(slotPaneX + 3, y, slotPaneX + 4, y + rowH - 2, ACCENT_GOLD);

            String num = String.valueOf(i + 1);
            g.text(font, num, slotPaneX + 22 - font.width(num), y + 4, ACCENT_GOLD, false);

            SpellSlot slot = spellSlots.getSlot(i);
            Component label;
            boolean filled = slot != null && slot.hasSpell();
            if (filled) {
                label = slot.getName().isEmpty()
                        ? Component.translatable("gui.buildaspell.spell_builder.unnamed_spell")
                        : Component.literal(slot.getName());
            } else {
                label = Component.translatable("gui.buildaspell.spell_builder.empty_slot");
            }
            int labelX = slotPaneX + 27;
            int maxW = slotPaneX + slotPaneW - 6 - labelX;
            if (font.width(label) > maxW) {
                label = Component.literal(font.plainSubstrByWidth(label.getString(), maxW - 6) + "…");
            }
            g.text(font, label, labelX, y + 4, filled ? TEXT : TEXT_DIM, false);
        }
    }

    private void renderInfoBar(GuiGraphicsExtractor g, SpellCombo combo) {
        Spell spell = buildSpellFromSlots();
        int y = this.height - 38;
        Component mana = Component.translatable("gui.buildaspell.spell_builder.mana_cost",
                String.format("%.1f", spell.getManaCost()));
        Component range = Component.translatable("gui.buildaspell.spell_builder.range",
                String.format("%.1f", spell.getRange()));
        g.text(font, mana, buildX + 4, y, ACCENT_GOLD, false);
        g.text(font, range, buildX + 4 + font.width(mana) + 16, y, ACCENT_GOLD, false);
        if (saveButton != null) saveButton.active = selectedDelivery != null && !effectGroups.isEmpty();
    }

    private void drawIcon(GuiGraphicsExtractor g, Object component, int x, int y) {
        ItemStack stack = ComponentIcons.forComponent(component);
        g.item(stack, x, y);
    }

    private void drawPlus(GuiGraphicsExtractor g, int x, int y, int color) {
        int cx = x + SLOT / 2, cy = y + SLOT / 2;
        int dim = (color & 0x00FFFFFF) | 0x77000000;
        g.fill(cx - 4, cy - 1, cx + 5, cy + 2, dim);
        g.fill(cx - 1, cy - 4, cx + 2, cy + 5, dim);
    }

    private void renderComponentTooltip(GuiGraphicsExtractor g, Object comp, double mouseX, double mouseY, boolean deliveryWarn) {
        List<Component> lines = new ArrayList<>();
        lines.add(componentName(comp).copy().withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));
        lines.add(componentType(comp).copy().withStyle(ChatFormatting.GRAY));
        lines.add(componentDesc(comp).copy().withStyle(ChatFormatting.DARK_GRAY));
        lines.add(Component.translatable("gui.buildaspell.spell_builder.mana_format",
                String.format("%.1f", componentCost(comp))).withStyle(ChatFormatting.YELLOW));

        if (deliveryWarn) {
            lines.add(Component.translatable("gui.buildaspell.spell_builder.needs_projectile")
                    .withStyle(ChatFormatting.GOLD));
        }

        List<String> hints = comboHints(comp);
        if (!hints.isEmpty()) {
            lines.add(Component.empty());
            lines.add(Component.literal("Combos:").withStyle(ChatFormatting.GOLD));
            for (String h : hints) lines.add(Component.literal("  " + h).withStyle(ChatFormatting.DARK_AQUA));
        }
        drawTooltip(g, lines, (int) mouseX, (int) mouseY);
    }

    private void drawTooltip(GuiGraphicsExtractor g, List<Component> comps, int mouseX, int mouseY) {
        List<FormattedCharSequence> lines = comps.stream()
                .map(Component::getVisualOrderText).collect(Collectors.toList());
        int w = lines.stream().mapToInt(font::width).max().orElse(0);
        int h = lines.size() * 10;
        int tx = mouseX + 12, ty = mouseY - 12;
        if (tx + w + 6 > width) tx = mouseX - w - 16;
        if (ty + h + 6 > height) ty = height - h - 6;
        if (ty < 4) ty = 4;
        g.fill(tx - 4, ty - 4, tx + w + 4, ty + h + 4, 0xF2050D0E);
        g.fill(tx - 4, ty - 4, tx + w + 4, ty - 3, PANEL_BORDER_HI);
        g.fill(tx - 4, ty + h + 3, tx + w + 4, ty + h + 4, PANEL_BORDER);
        g.fill(tx - 4, ty - 4, tx - 3, ty + h + 4, PANEL_BORDER);
        g.fill(tx + w + 3, ty - 4, tx + w + 4, ty + h + 4, PANEL_BORDER);
        for (int i = 0; i < lines.size(); i++) {
            g.text(font, lines.get(i), tx, ty + i * 10, TEXT, false);
        }
    }

    // ── Hit-testing ────────────────────────────────────────────────────
    private SlotBox slotBoxAt(double mx, double my) {
        if (mx < buildX || mx >= buildX + buildW || my < buildY || my >= buildY + buildH) return null;
        for (SlotBox b : buildLayout()) if (b.contains(mx, my)) return b;
        return null;
    }

    private Object componentInBox(SlotBox b) {
        return switch (b.kind()) {
            case DELIVERY -> selectedDelivery;
            case DELIVERY_MODIFIER -> deliveryModifiers.get(b.modIndex());
            case EFFECT -> effectGroups.get(b.effectIndex()).component();
            case MODIFIER -> effectGroups.get(b.effectIndex()).modifiers.get(b.modIndex());
            default -> null;
        };
    }

    private Object paletteComponentAt(double mx, double my) {
        if (mx < palX || mx >= palX + palW || my < palY || my >= palY + palH) return null;
        int innerX = palX + 4, innerY = palY + 4, rightX = palX + palW - PAL_PAD_R;
        for (PalRow r : buildPaletteRows()) {
            if (r.isHeader()) continue;
            int ry = innerY + r.y() - paletteScroll;
            if (mx >= innerX && mx < rightX && my >= ry && my < ry + r.h() - 1) return r.component();
        }
        return null;
    }

    private int slotPaneRowAt(double mx, double my) {
        if (mx < slotPaneX + 3 || mx >= slotPaneX + slotPaneW - 3) return -1;
        int top = slotPaneY + 16, rowH = 16;
        int row = (int) ((my - top) / rowH);
        return (row >= 0 && row < 10 && my >= top) ? row : -1;
    }

    private boolean acceptsDrag(SlotBox b, Object drag) {
        if (drag instanceof DeliveryMethod) return b.kind() == SlotKind.DELIVERY;
        if (drag instanceof SpellEffect || drag instanceof DatapackEffect)
            return b.kind() == SlotKind.EFFECT || b.kind() == SlotKind.ADD_EFFECT;
        if (drag instanceof SpellModifier m) {
            // Roster (delivery-level) modifiers only land in the Delivery zone; effect-bound
            // modifiers only land on an effect. This keeps the two buckets honest.
            if (ModifierApplicability.isDeliveryLevel(m))
                return b.kind() == SlotKind.DELIVERY_MODIFIER || b.kind() == SlotKind.ADD_DELIVERY_MODIFIER;
            return b.kind() == SlotKind.MODIFIER || b.kind() == SlotKind.ADD_MODIFIER;
        }
        return false;
    }

    // ── Input ──────────────────────────────────────────────────────────
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean forwarded) {
        if (super.mouseClicked(event, forwarded)) return true;
        double mx = event.x(), my = event.y();
        int button = event.button();

        if (button == 0) {
            int row = slotPaneRowAt(mx, my);
            if (row >= 0) { selectAndLoadSlot(row); return true; }

            Object palComp = paletteComponentAt(mx, my);
            if (palComp != null) {
                // A dimmed delivery-modifier (invalid under the current delivery) can't be picked up;
                // surface the same reason the warn-guard would give instead of starting a drag.
                if (isPaletteEntryDisabled(palComp) && palComp instanceof SpellModifier m) {
                    warnIfUselessDelivery(m);
                    return true;
                }
                dragging = palComp;
                dragFromBuild = false;
                pressX = mx; pressY = my;
                return true;
            }

            SlotBox box = slotBoxAt(mx, my);
            if (box != null && componentInBox(box) != null) {
                // Defer the pickup-vs-select decision until mouseReleased/mouseDragged so a plain
                // left-click selects (non-destructive) while a drag still picks the component up.
                pressedBuildBox = box;
                pressX = mx; pressY = my;
                return true;
            }
        }

        if (button == 1) {
            SlotBox box = slotBoxAt(mx, my);
            if (box != null && componentInBox(box) != null) { removeFromBuild(box); return true; }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        // Promote a deferred build-slot press into a real pickup once the cursor moves enough.
        if (event.button() == 0 && pressedBuildBox != null && dragging == null
                && Math.hypot(event.x() - pressX, event.y() - pressY) >= 4) {
            Object comp = componentInBox(pressedBuildBox);
            if (comp != null) {
                dragging = comp;
                dragFromBuild = true;
                removeFromBuild(pressedBuildBox);
            }
            pressedBuildBox = null;
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        // A deferred build-slot press that never became a drag = a plain click → select.
        if (event.button() == 0 && pressedBuildBox != null && dragging == null) {
            SlotBox box = pressedBuildBox;
            pressedBuildBox = null;
            int idx = box.effectIndex();
            if (box.kind() == SlotKind.EFFECT) {
                selectedEffectIndex = (selectedEffectIndex == idx) ? -1 : idx; // toggle
            } else if (box.kind() == SlotKind.MODIFIER) {
                selectedEffectIndex = idx; // selecting a modifier targets its parent effect
            }
            return true;
        }
        if (event.button() == 0 && dragging != null) {
            double mx = event.x(), my = event.y();
            Object drag = dragging;
            dragging = null;

            SlotBox target = slotBoxAt(mx, my);
            if (target != null && acceptsDrag(target, drag)) {
                applyDrop(target, drag);
            } else if (!dragFromBuild) {
                // a click (negligible movement) on the palette = add to chain
                double dist = Math.hypot(mx - pressX, my - pressY);
                if (dist < 5) addComponentToSpell(drag);
            }
            // build-origin drag dropped on nothing == removed (already taken out on press)
            dragFromBuild = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    private void applyDrop(SlotBox target, Object drag) {
        switch (target.kind()) {
            case DELIVERY -> { if (drag instanceof DeliveryMethod d) selectedDelivery = d; }
            case DELIVERY_MODIFIER -> {
                if (drag instanceof SpellModifier m) {
                    if (warnIfUselessDelivery(m)) return;
                    deliveryModifiers.set(target.modIndex(), m);
                }
            }
            case ADD_DELIVERY_MODIFIER -> {
                if (drag instanceof SpellModifier m) {
                    if (warnIfUselessDelivery(m)) return;
                    deliveryModifiers.add(m);
                }
            }
            case EFFECT -> {
                EffectGroup g = effectGroups.get(target.effectIndex());
                if (drag instanceof SpellEffect e) { g.effect = e; g.dataEffect = null; }
                else if (drag instanceof DatapackEffect de) { g.dataEffect = de; g.effect = null; }
            }
            case ADD_EFFECT -> {
                if (drag instanceof SpellEffect e) { effectGroups.add(new EffectGroup(e)); selectedEffectIndex = effectGroups.size() - 1; }
                else if (drag instanceof DatapackEffect de) { effectGroups.add(new EffectGroup(de)); selectedEffectIndex = effectGroups.size() - 1; }
            }
            case MODIFIER -> {
                if (drag instanceof SpellModifier m) {
                    EffectGroup g = effectGroups.get(target.effectIndex());
                    if (warnIfUseless(m, g)) return;
                    g.modifiers.set(target.modIndex(), m);
                }
            }
            case ADD_MODIFIER -> {
                if (drag instanceof SpellModifier m) {
                    EffectGroup g = effectGroups.get(target.effectIndex());
                    if (warnIfUseless(m, g)) return;
                    g.modifiers.add(m);
                }
            }
        }
    }

    private void removeFromBuild(SlotBox box) {
        switch (box.kind()) {
            case DELIVERY -> selectedDelivery = null;
            case DELIVERY_MODIFIER -> deliveryModifiers.remove(box.modIndex());
            case EFFECT -> {
                int removed = box.effectIndex();
                effectGroups.remove(removed);
                if (selectedEffectIndex == removed) selectedEffectIndex = -1;
                else if (selectedEffectIndex > removed) selectedEffectIndex--;
            }
            case MODIFIER -> effectGroups.get(box.effectIndex()).modifiers.remove(box.modIndex());
            default -> {}
        }
    }

    private void addComponentToSpell(Object comp) {
        if (comp instanceof DeliveryMethod d) selectedDelivery = d;
        else if (comp instanceof SpellEffect e) { effectGroups.add(new EffectGroup(e)); selectedEffectIndex = effectGroups.size() - 1; }
        else if (comp instanceof DatapackEffect de) { effectGroups.add(new EffectGroup(de)); selectedEffectIndex = effectGroups.size() - 1; }
        else if (comp instanceof SpellModifier m && ModifierApplicability.isDeliveryLevel(m)) {
            // Roster modifiers attach to the Delivery zone regardless of any selected effect.
            if (warnIfUselessDelivery(m)) return;
            deliveryModifiers.add(m);
        }
        else if (comp instanceof SpellModifier m && !effectGroups.isEmpty()) {
            int target = (selectedEffectIndex >= 0 && selectedEffectIndex < effectGroups.size())
                    ? selectedEffectIndex : effectGroups.size() - 1;
            EffectGroup g = effectGroups.get(target);
            if (warnIfUseless(m, g)) return;
            g.modifiers.add(m);
        }
    }

    /**
     * Rejects modifiers that would do nothing on the target effect group (e.g. Chain on Light, or a
     * projectile modifier on a non-projectile delivery), telling the player why. Datapack-authored
     * effects are always allowed since their modifier handling is unknown to the client.
     *
     * @return true if the modifier was rejected (caller should not add it).
     */
    private boolean warnIfUseless(SpellModifier m, EffectGroup g) {
        if (g.effect == null) return false; // datapack effect — unknown consumption, allow
        if (ModifierApplicability.isUseful(m, g.effect, selectedDelivery)) return false;
        feedback(Component.translatable("gui.buildaspell.spell_builder.modifier_no_effect",
                componentName(m), componentName(g.component())), ChatFormatting.YELLOW);
        return true;
    }

    /**
     * @return true if a palette entry should render dimmed/disabled. Only delivery-level (roster)
     *         modifiers gate on the selected delivery — a projectile-only roster modifier is disabled
     *         when the chosen delivery isn't a projectile. Effects and effect-bound modifiers never
     *         dim here (their usefulness depends on the target effect, resolved on drop).
     */
    private boolean isPaletteEntryDisabled(Object comp) {
        if (comp instanceof SpellModifier m && ModifierApplicability.isDeliveryLevel(m)) {
            return !ModifierApplicability.isDeliveryModifierUseful(m, selectedDelivery);
        }
        return false;
    }

    /**
     * Rejects a delivery-level (roster) modifier that would do nothing under the current delivery
     * (e.g. a projectile-only modifier like Pierce/Chain on a non-projectile delivery), telling the
     * player why.
     *
     * @return true if the modifier was rejected (caller should not add it).
     */
    private boolean warnIfUselessDelivery(SpellModifier m) {
        if (ModifierApplicability.isDeliveryModifierUseful(m, selectedDelivery)) return false;
        feedback(Component.translatable("gui.buildaspell.spell_builder.modifier_no_delivery",
                componentName(m)), ChatFormatting.YELLOW);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= buildX && mouseX < buildX + buildW && mouseY >= buildY && mouseY < buildY + buildH) {
            int max = maxBuildScroll();
            buildScrollY = Math.max(0, Math.min(max, buildScrollY - (int) (scrollY * 16)));
            return true;
        }
        if (mouseX >= palX && mouseX < palX + palW && mouseY >= palY && mouseY < palY + palH) {
            int max = Math.max(0, paletteContentH(buildPaletteRows()) - (palH - 8));
            paletteScroll = Math.max(0, Math.min(max, paletteScroll - (int) (scrollY * 16)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return super.charTyped(event);
    }

    // ── Component metadata ─────────────────────────────────────────────
    private void updateFilteredComponents() {
        filteredComponents.clear();
        String search = searchField == null ? "" : searchField.getValue().toLowerCase();
        if (search.isEmpty()) {
            filteredComponents.addAll(availableComponents);
        } else {
            for (Object c : availableComponents) {
                if (componentId(c).toLowerCase().contains(search)
                        || componentName(c).getString().toLowerCase().contains(search)
                        || componentDesc(c).getString().toLowerCase().contains(search)
                        || typeKey(c).contains(search)) {
                    filteredComponents.add(c);
                }
            }
        }
        paletteScroll = 0;
    }

    private String typeKey(Object c) {
        if (c instanceof DeliveryMethod) return "delivery";
        if (c instanceof SpellEffect) return "effect";
        if (c instanceof DatapackEffect) return "effect";
        if (c instanceof SpellModifier) return "modifier";
        return "component";
    }

    private String componentId(Object c) {
        if (c instanceof DeliveryMethod d) return d.getSerializedName();
        if (c instanceof SpellEffect e) return e.getSerializedName();
        if (c instanceof DatapackEffect de) return de.id().toString();
        if (c instanceof SpellModifier m) return m.getSerializedName();
        return "";
    }

    private Component componentName(Object c) {
        if (c instanceof DatapackEffect de) {
            return de.display().name()
                    .map(Component::literal)
                    .map(Component.class::cast)
                    .orElseGet(() -> Component.literal(prettyName(de.id().getPath())));
        }
        return Component.translatable(typeKey(c) + ".buildaspell." + componentId(c) + ".name");
    }

    private Component componentDesc(Object c) {
        if (c instanceof DatapackEffect de) return Component.literal(de.id().toString());
        return Component.translatable(typeKey(c) + ".buildaspell." + componentId(c) + ".desc");
    }

    private Component componentType(Object c) {
        return Component.translatable("gui.buildaspell.component_type." + typeKey(c));
    }

    private int accentColor(Object c) {
        if (c instanceof DeliveryMethod) return DELIVERY_ACCENT;
        if (c instanceof SpellEffect) return EFFECT_ACCENT;
        if (c instanceof DatapackEffect de) return de.display().color().map(col -> 0xFF000000 | col).orElse(EFFECT_ACCENT);
        if (c instanceof SpellModifier) return MODIFIER_ACCENT;
        return SLOT_BORDER;
    }

    private float componentCost(Object c) {
        if (c instanceof DeliveryMethod d) return d.getBaseCost();
        if (c instanceof SpellEffect e) return e.getBaseCost();
        if (c instanceof DatapackEffect de) return de.display().cost().orElse(0.0).floatValue();
        if (c instanceof SpellModifier m) return m.getBaseCost();
        return 0;
    }

    private static String prettyName(String id) {
        String s = id.replace('_', ' ');
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private boolean isComboAligned() {
        return SpellCombo.detect(buildSpellFromSlots()) != null;
    }

    private List<String> comboHints(Object component) {
        List<String> hints = new ArrayList<>();
        Spell current = buildSpellFromSlots();
        for (SpellCombo combo : SpellCombo.values()) {
            boolean participates =
                    (component instanceof SpellEffect e && combo.getRequiredEffects().contains(e))
                 || (component instanceof SpellModifier m && combo.getRequiredModifiers().containsKey(m));
            if (!participates) continue;

            String name = prettyName(combo.getId());
            if (combo.matches(current)) {
                hints.add(name + " [ACTIVE]");
            } else {
                List<String> missing = new ArrayList<>();
                java.util.Set<SpellEffect> have = new java.util.HashSet<>(current.getEffects());
                for (SpellEffect req : combo.getRequiredEffects())
                    if (!have.contains(req)) missing.add(req.getSerializedName());
                java.util.Map<SpellModifier, Integer> counts = current.getModifierCounts();
                for (var entry : combo.getRequiredModifiers().entrySet()) {
                    int has = counts.getOrDefault(entry.getKey(), 0);
                    if (has < entry.getValue())
                        missing.add(entry.getKey().getSerializedName() + " x" + (entry.getValue() - has));
                }
                hints.add(missing.isEmpty() ? name + " (need more components)"
                        : name + " (need: " + String.join(", ", missing) + ")");
            }
        }
        return hints;
    }

    private Spell buildSpellFromSlots() {
        Spell spell = new Spell();
        spell.setDelivery(selectedDelivery);
        for (SpellModifier m : deliveryModifiers) spell.addDeliveryModifier(m);
        for (EffectGroup g : effectGroups) {
            if (g.effect != null) spell.addComponent(new SpellComponent.Effect(g.effect));
            else if (g.dataEffect != null) spell.addComponent(new SpellComponent.DataEffect(g.dataEffect.id()));
            for (SpellModifier m : g.modifiers) spell.addComponent(new SpellComponent.Modifier(m));
        }
        spell.setVisual(selectedVisual);
        return spell;
    }

    // ── Actions ────────────────────────────────────────────────────────
    private void feedback(Component msg, ChatFormatting color) {
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.sendSystemMessage(msg.copy().withStyle(color));
        }
    }

    private void clearSpell() {
        selectedDelivery = null;
        selectedVisual = SpellVisual.DEFAULT;
        deliveryModifiers.clear();
        effectGroups.clear();
        selectedEffectIndex = -1;
        nameField.setValue("");
    }

    /**
     * Select a slot row and, if it holds a spell, load that spell into the build area immediately.
     * Clicking an empty slot just moves the selection (so you don't lose an in-progress build). This
     * is the click-to-load path; the "Load" button uses {@link #loadSelectedSlot} for its feedback.
     */
    private void selectAndLoadSlot(int index) {
        selectedSlot = index;
        SpellSlot slot = spellSlots.getSlot(index);
        if (slot == null || !slot.hasSpell()) return;
        clearSpell();
        nameField.setValue(slot.getName());
        Spell spell = slot.getSpell();
        selectedDelivery = spell.getDelivery();
        selectedVisual = spell.getVisual();
        deliveryModifiers = deliveryModifiersFromSpell(spell);
        effectGroups = groupsFromComponents(spell.getComponents());
    }

    private void loadSelectedSlot() {
        SpellSlot slot = spellSlots.getSlot(selectedSlot);
        if (slot == null || !slot.hasSpell()) {
            feedback(Component.translatable("gui.buildaspell.spell_builder.slot_empty", selectedSlot + 1),
                    ChatFormatting.RED);
            return;
        }
        clearSpell();
        nameField.setValue(slot.getName());
        Spell spell = slot.getSpell();
        selectedDelivery = spell.getDelivery();
        selectedVisual = spell.getVisual();
        deliveryModifiers = deliveryModifiersFromSpell(spell);
        effectGroups = groupsFromComponents(spell.getComponents());
        feedback(Component.translatable("gui.buildaspell.spell_builder.spell_loaded"), ChatFormatting.GREEN);
    }

    private void saveSpell() {
        Spell spell = buildSpellFromSlots();
        if (spell.getDelivery() == null) {
            feedback(Component.translatable("gui.buildaspell.spell_builder.require_delivery"), ChatFormatting.RED);
            return;
        }
        if (!spell.hasAnyEffect()) {
            feedback(Component.translatable("gui.buildaspell.spell_builder.require_effect"), ChatFormatting.RED);
            return;
        }
        String name = nameField.getValue();
        List<String> ids = new ArrayList<>();
        List<String> types = new ArrayList<>();
        // Delivery-level (roster) modifiers are sent as plain "modifier" entries; the server-side
        // Spell constructor's migration re-buckets them into the delivery-modifier list. We emit
        // them first so they never disturb the effect-first ordering of the component stream.
        for (SpellModifier m : spell.getDeliveryModifiers()) { ids.add(m.getSerializedName()); types.add("modifier"); }
        for (SpellComponent c : spell.getComponents()) {
            if (c instanceof SpellComponent.Effect e) { ids.add(e.effect().getSerializedName()); types.add("effect"); }
            else if (c instanceof SpellComponent.DataEffect de) { ids.add(de.effectId().toString()); types.add("data_effect"); }
            else if (c instanceof SpellComponent.Modifier m) { ids.add(m.modifier().getSerializedName()); types.add("modifier"); }
            else if (c instanceof SpellComponent.CompatEffect ce) { ids.add(ce.effectId()); types.add("compat_effect"); }
        }
        persistState(name);
        ClientPacketDistributor.sendToServer(new SaveSpellPacket(
                selectedSlot, name, spell.getDelivery().getSerializedName(), ids, types,
                selectedVisual.color(), selectedVisual.shape().getSerializedName(), selectedVisual.trail()));
        feedback(Component.translatable("gui.buildaspell.spell_builder.spell_saved", selectedSlot + 1),
                ChatFormatting.GREEN);
    }

    private void exportSpell() {
        Spell spell = buildSpellFromSlots();
        if (spell.getDelivery() == null) {
            feedback(Component.translatable("gui.buildaspell.spell_builder.no_export"), ChatFormatting.RED);
            return;
        }
        feedback(Component.translatable("gui.buildaspell.spell_builder.exported"), ChatFormatting.GREEN);
        feedback(Component.literal(SpellExporter.encode(spell)), ChatFormatting.YELLOW);
    }

    private void importSpell() {
        if (minecraft == null) return;
        minecraft.setScreen(new SpellImportScreen(this, code -> {
            Spell imported = SpellExporter.decode(code);
            if (imported == null) {
                feedback(Component.translatable("gui.buildaspell.spell_builder.invalid_code"), ChatFormatting.RED);
                return;
            }
            clearSpell();
            if (imported.getDelivery() != null) selectedDelivery = imported.getDelivery();
            selectedVisual = imported.getVisual();
            deliveryModifiers = deliveryModifiersFromSpell(imported);
            effectGroups = groupsFromComponents(imported.getComponents());
            feedback(Component.translatable("gui.buildaspell.spell_builder.imported"), ChatFormatting.GREEN);
        }));
    }

    private void copySpellCode() {
        Spell spell = buildSpellFromSlots();
        if (spell.getDelivery() == null) {
            feedback(Component.translatable("gui.buildaspell.spell_builder.no_copy"), ChatFormatting.RED);
            return;
        }
        if (minecraft != null) {
            minecraft.keyboardHandler.setClipboard(SpellExporter.encode(spell));
            feedback(Component.translatable("gui.buildaspell.spell_builder.copied"), ChatFormatting.GREEN);
        }
    }

    private void exportToPower() {
        Spell spell = buildSpellFromSlots();
        // Only a castable spell (delivery set + at least one effect) can be baked into a power.
        if (!spell.hasSpell()) {
            feedback(Component.translatable("gui.buildaspell.spell_builder.no_power"), ChatFormatting.RED);
            return;
        }
        if (minecraft != null) {
            minecraft.keyboardHandler.setClipboard(SpellExporter.toNeoOriginsPower(spell));
            feedback(Component.translatable("gui.buildaspell.spell_builder.power_copied"), ChatFormatting.GREEN);
        }
    }

    private void persistState(String name) {
        lastDelivery = selectedDelivery;
        lastDeliveryModifiers = new ArrayList<>(deliveryModifiers);
        lastVisual = selectedVisual;
        lastEffectGroups = new ArrayList<>();
        for (EffectGroup g : effectGroups) lastEffectGroups.add(new EffectGroup(g));
        lastSpellName = name;
    }

    @Override
    public void onClose() {
        lastSpellName = nameField.getValue();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
