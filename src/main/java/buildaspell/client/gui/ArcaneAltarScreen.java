package buildaspell.client.gui;

import buildaspell.block.entity.ArcaneAltarBlockEntity;
import buildaspell.enchanting.EnchantmentCostManager;
import buildaspell.menu.ArcaneAltarMenu;
import buildaspell.network.ArcaneAltarEnchantPacket;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.Map;

public class ArcaneAltarScreen extends AbstractContainerScreen<ArcaneAltarMenu> {

    // Palette + draw helpers come from the shared GuiTheme (teal + amethyst).
    private static final int COLOR_BG        = GuiTheme.SCRIM;
    private static final int COLOR_BORDER    = GuiTheme.BORDER_HI;   // teal panel edge
    private static final int COLOR_SLOT      = GuiTheme.SLOT_BG;     // slot interior
    private static final int COLOR_SLOT_HI   = GuiTheme.SLOT_BORDER; // amethyst slot frame
    private static final int COLOR_TEXT      = GuiTheme.TEXT;        // primary text
    private static final int COLOR_HEADER    = GuiTheme.HEADER;      // title text
    private static final int COLOR_HIGHLIGHT = GuiTheme.HOVER;       // amethyst selection

    // Ingredient side-panel geometry (local coords, relative to leftPos/topPos).
    private static final int INGREDIENT_PANEL_X = 190;
    private static final int INGREDIENT_PANEL_W = 78;
    private static final int INGREDIENT_PANEL_TOP = 16;
    private static final int INGREDIENT_LEGEND_Y =
            ArcaneAltarMenu.INGREDIENT_GRID_Y + 2 * ArcaneAltarMenu.INGREDIENT_SLOT_PITCH + 8;
    private static final int INGREDIENT_LEGEND_ROW = 16;
    private static final int INGREDIENT_PANEL_PAD = 8;

    // The three types the altar offers, and the plain "what does this do" tooltip for each.
    private static final Identifier MANA_POOL = ArcaneAltarBlockEntity.MANA_POOL_ENCHANTMENT;
    private static final Identifier MANA_REGEN = ArcaneAltarBlockEntity.MANA_REGENERATION_ENCHANTMENT;
    private static final Identifier SPELL_POWER = ArcaneAltarBlockEntity.SPELL_POWER_ENCHANTMENT;
    private static final Component MANA_POOL_DESC = Component.literal("Increases maximum mana capacity");
    private static final Component MANA_REGEN_DESC = Component.literal("Increases mana regeneration rate");
    private static final Component SPELL_POWER_DESC = Component.literal("Increases spell damage and effect strength");

    private int selectedLevel = 1;
    private Identifier selectedEnchantment = null;
    private boolean showHelp = false;
    private boolean showIngredients = false;

    // Stored so we can draw a selection highlight / toggle visibility for the help overlay.
    private Button manaPoolBtn;
    private Button manaRegenBtn;
    private Button spellPowerBtn;
    private Button enchantBtn;
    private Button minusBtn;
    private Button plusBtn;
    private Button helpBtn;
    private Button ingredientsBtn;

    public ArcaneAltarScreen(ArcaneAltarMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 212);
        this.titleLabelY = 6;
        this.inventoryLabelY = 118;
    }

    @Override
    protected void init() {
        super.init();

        // Three enchantment-type buttons stacked in the left column.
        int bx = leftPos + 8;
        int bw = 86;
        int bh = 18;

        manaPoolBtn = addRenderableWidget(ThemedButton.of(
                Component.literal("Mana Pool"), bx, topPos + 22, bw, bh,
                btn -> selectEnchantment(MANA_POOL)));
        manaPoolBtn.setTooltip(Tooltip.create(MANA_POOL_DESC));

        manaRegenBtn = addRenderableWidget(ThemedButton.of(
                Component.literal("Mana Regen"), bx, topPos + 42, bw, bh,
                btn -> selectEnchantment(MANA_REGEN)));
        manaRegenBtn.setTooltip(Tooltip.create(MANA_REGEN_DESC));

        spellPowerBtn = addRenderableWidget(ThemedButton.of(
                Component.literal("Spell Power"), bx, topPos + 62, bw, bh,
                btn -> selectEnchantment(SPELL_POWER)));
        spellPowerBtn.setTooltip(Tooltip.create(SPELL_POWER_DESC));

        // Level controls below the type buttons.
        minusBtn = addRenderableWidget(ThemedButton.of(
                Component.literal("-"), bx, topPos + 86, 18, 18, btn -> adjustLevel(-1)));
        plusBtn = addRenderableWidget(ThemedButton.of(
                Component.literal("+"), bx + bw - 18, topPos + 86, 18, 18, btn -> adjustLevel(1)));

        // Enchant button on the right column, under the input slot + cost readout.
        enchantBtn = addRenderableWidget(ThemedButton.of(
                Component.literal("Enchant"), leftPos + 104, topPos + 86, 64, 18, btn -> enchant()));

        // Side tabs on the right edge: "?" help, and below it the ingredient tab.
        // Their panels open to the right of the GUI so nothing overflows the working area.
        helpBtn = addRenderableWidget(ThemedButton.of(
                Component.literal("?"), leftPos + imageWidth + 1, topPos + 6, 14, 14, btn -> toggleHelp()));
        helpBtn.setTooltip(Tooltip.create(Component.literal("What do these do?")));

        ingredientsBtn = addRenderableWidget(ThemedButton.of(
                Component.literal("+"), leftPos + imageWidth + 1, topPos + 22, 14, 14, btn -> toggleIngredients()));
        ingredientsBtn.setTooltip(Tooltip.create(Component.literal("Ingredients")));

        // Keep the client menu's ingredient-slot visibility in sync with the tab.
        menu.ingredientTabOpen = showIngredients;
    }

    private void toggleHelp() {
        showHelp = !showHelp;
        if (showHelp) showIngredients = false;
        menu.ingredientTabOpen = showIngredients;
    }

    private void toggleIngredients() {
        showIngredients = !showIngredients;
        if (showIngredients) showHelp = false;
        menu.ingredientTabOpen = showIngredients;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        ItemStack target = menu.getSlot(0).getItem();

        // Each type only counts on the kind of item that reads it (Spell Power off the main hand,
        // the mana pair off worn armor), so offer only the ones the placed item can use. The
        // server enforces the same rule; this just stops the player paying to find out.
        updateTypeButton(manaPoolBtn, MANA_POOL, MANA_POOL_DESC, target);
        updateTypeButton(manaRegenBtn, MANA_REGEN, MANA_REGEN_DESC, target);
        updateTypeButton(spellPowerBtn, SPELL_POWER, SPELL_POWER_DESC, target);

        if (enchantBtn != null) {
            // Clickable as soon as a usable type is chosen and an item sits in the altar. XP and
            // materials are still left to the server, which messages the player if something's
            // short — so the button never silently greys out for a cost the player can see.
            boolean pairingOk = ArcaneAltarBlockEntity.isValidEnchantTarget(selectedEnchantment, target);
            enchantBtn.active = selectedEnchantment != null && !target.isEmpty() && pairingOk;
            enchantBtn.setTooltip(selectedEnchantment != null && !target.isEmpty() && !pairingOk
                    ? Tooltip.create(ArcaneAltarBlockEntity.enchantTargetRequirement(selectedEnchantment))
                    : null);
        }
    }

    /**
     * Greys a type button out when the placed item would never read that enchantment, and swaps its
     * tooltip for the reason. With the altar empty every type stays offered so the player can price
     * one up before committing an item.
     */
    private void updateTypeButton(Button button, Identifier enchantmentId, Component description, ItemStack target) {
        if (button == null) return;
        boolean usable = target.isEmpty() || ArcaneAltarBlockEntity.isValidEnchantTarget(enchantmentId, target);
        button.active = usable;
        button.setTooltip(Tooltip.create(usable
                ? description
                : ArcaneAltarBlockEntity.enchantTargetRequirement(enchantmentId)));
    }

    /** The current level of the selected enchantment already on the placed item (0 if none). */
    private int currentEnchantLevel() {
        if (selectedEnchantment == null || minecraft == null || minecraft.level == null) return 0;
        ItemStack item = menu.getSlot(0).getItem();
        if (item.isEmpty()) return 0;
        ResourceKey<net.minecraft.world.item.enchantment.Enchantment> key =
                ResourceKey.create(Registries.ENCHANTMENT, selectedEnchantment);
        return minecraft.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(key)
                .map(h -> EnchantmentHelper.getItemEnchantmentLevel(h, item)).orElse(0);
    }

    /** The full material + XP bill to climb from the item's current level to the target level. */
    private EnchantmentCostManager.CumulativeCost cumulativeCost() {
        return EnchantmentCostManager.getCumulativeCost(
                selectedEnchantment.toString(), currentEnchantLevel(), selectedLevel);
    }

    private int countInIngredients(Item item) {
        int start = ArcaneAltarBlockEntity.INGREDIENT_START;
        int total = 0;
        for (int i = start; i < start + ArcaneAltarBlockEntity.INGREDIENT_COUNT; i++) {
            ItemStack stack = menu.getSlot(i).getItem();
            if (stack.getItem() == item) total += stack.getCount();
        }
        return total;
    }

    /** Whether the selected type is one the placed item will actually read (empty altar counts as OK). */
    private boolean pairingOk() {
        ItemStack target = menu.getSlot(0).getItem();
        return target.isEmpty() || ArcaneAltarBlockEntity.isValidEnchantTarget(selectedEnchantment, target);
    }

    private boolean canAffordEnchant() {
        if (selectedEnchantment == null || minecraft == null || minecraft.player == null) return false;
        if (selectedLevel <= currentEnchantLevel()) return false;
        EnchantmentCostManager.CumulativeCost cost = cumulativeCost();
        if (EnchantmentCostManager.totalXpPoints(minecraft.player) < cost.xpPoints()) return false;
        for (Map.Entry<Item, Integer> need : cost.items().entrySet()) {
            if (countInIngredients(need.getKey()) < need.getValue()) return false;
        }
        return true;
    }

    private void selectEnchantment(Identifier enchantmentId) {
        this.selectedEnchantment = enchantmentId;
    }

    private void adjustLevel(int delta) {
        selectedLevel = Math.max(1, Math.min(20, selectedLevel + delta));
    }

    private void enchant() {
        if (selectedEnchantment == null || minecraft == null || minecraft.player == null) return;
        ClientPacketDistributor.sendToServer(new ArcaneAltarEnchantPacket(selectedEnchantment.toString(), selectedLevel));
    }

    private void renderBg(GuiGraphicsExtractor graphics) {
        int x = leftPos, y = topPos, w = imageWidth, h = imageHeight;

        // Dark arcane background
        graphics.fill(x, y, x + w, y + h, COLOR_BG);

        // Working-area card (title down to the inventory separator): gradient body,
        // teal double-frame, amethyst corner brackets — the shared GuiTheme panel.
        GuiTheme.drawPanel(graphics, x + 4, y + 16, w - 8, 94);

        // Outer purple border (1px, all four sides)
        graphics.fill(x,         y,         x + w,     y + 1,     COLOR_BORDER);
        graphics.fill(x,         y + h - 1, x + w,     y + h,     COLOR_BORDER);
        graphics.fill(x,         y,         x + 1,     y + h,     COLOR_BORDER);
        graphics.fill(x + w - 1, y,         x + w,     y + h,     COLOR_BORDER);

        // Separator above player inventory
        graphics.fill(x + 7, y + 114, x + w - 7, y + 115, COLOR_BORDER);

        // Input slot: amethyst frame so it reads as the focal "put an item here" slot.
        // Centred (x=128 → centre 136) directly above the cost readout + Enchant button.
        drawSlot(graphics, x + 128, y + 24, COLOR_SLOT_HI);

        // Player inventory slot backgrounds (rows base 130, hotbar 188) — softer teal frame.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlot(graphics, x + 8 + col * 18, y + 130 + row * 18, COLOR_BORDER);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlot(graphics, x + 8 + col * 18, y + 188, COLOR_BORDER);
        }

        // Ingredient tab: side panel + the ingredient slot backgrounds. Drawn here (before the
        // slot pass) so the actual slot items render on top of these frames.
        if (showIngredients) {
            GuiTheme.drawPanel(graphics, x + INGREDIENT_PANEL_X, y + INGREDIENT_PANEL_TOP,
                    INGREDIENT_PANEL_W, ingredientPanelHeight());
            for (int i = 0; i < ArcaneAltarBlockEntity.INGREDIENT_COUNT; i++) {
                int col = i % 2, row = i / 2;
                drawSlot(graphics,
                        x + ArcaneAltarMenu.INGREDIENT_GRID_X + col * ArcaneAltarMenu.INGREDIENT_SLOT_PITCH,
                        y + ArcaneAltarMenu.INGREDIENT_GRID_Y + row * ArcaneAltarMenu.INGREDIENT_SLOT_PITCH,
                        COLOR_SLOT_HI);
            }
        }
    }

    private int ingredientLegendLines() {
        if (selectedEnchantment == null || minecraft == null || minecraft.player == null) return 1;
        if (!pairingOk()) return 1;
        if (selectedLevel <= currentEnchantLevel()) return 1;
        return cumulativeCost().items().size() + 1;
    }

    private int ingredientPanelHeight() {
        int legendBottom = INGREDIENT_LEGEND_Y + ingredientLegendLines() * INGREDIENT_LEGEND_ROW;
        return legendBottom + INGREDIENT_PANEL_PAD - INGREDIENT_PANEL_TOP;
    }

    /**
     * Ghosts the fixed material of every EMPTY ingredient slot so players can see what each slot
     * wants without hovering. Drawn in the main pass: the item then a translucent wash on top.
     */
    private void renderIngredientGhosts(GuiGraphicsExtractor graphics) {
        int start = ArcaneAltarBlockEntity.INGREDIENT_START;
        for (int i = 0; i < ArcaneAltarBlockEntity.INGREDIENT_COUNT; i++) {
            Item mat = ArcaneAltarBlockEntity.ingredientMaterial(i);
            if (mat == null) continue;
            var slot = menu.getSlot(start + i);
            if (!slot.getItem().isEmpty()) continue;
            int sx = leftPos + slot.x;
            int sy = topPos + slot.y;
            graphics.item(new ItemStack(mat), sx, sy);
            graphics.fill(sx, sy, sx + 16, sy + 16, 0x40000000);
        }
    }

    /** Draws an 18x18 inset slot background centred on the 16x16 item position (slotX/slotY). */
    private void drawSlot(GuiGraphicsExtractor graphics, int slotX, int slotY, int frameColor) {
        graphics.fill(slotX - 1, slotY - 1, slotX + 17, slotY + 17, frameColor);
        graphics.fill(slotX,     slotY,     slotX + 16, slotY + 16, COLOR_SLOT);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, titleLabelX, titleLabelY, COLOR_HEADER, false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xAAAAAA, false);

        // "Item" hint centred over the input slot (slot centre is local x=136), sitting in
        // the title bar above the panel frame so it no longer overlaps the border line.
        graphics.text(font, "Item", 136 - font.width("Item") / 2, 6, COLOR_TEXT, false);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // Background drawn in absolute coords before the translated label/slot pass.
        renderBg(graphics);
        super.extractContents(graphics, mouseX, mouseY, partialTick);

        // Amethyst wash over the currently-selected enchantment button.
        Button sel = selectedButton();
        if (sel != null) {
            GuiTheme.roundedRect(graphics, sel.getX(), sel.getY(),
                    sel.getX() + sel.getWidth(), sel.getY() + sel.getHeight(), 3, COLOR_HIGHLIGHT);
        }

        // Level readout, centred between the - / + buttons
        int levelCenterX = leftPos + 8 + 86 / 2;
        graphics.centeredText(font, "Lv " + selectedLevel, levelCenterX, topPos + 90, 0xFFFFFFFF);

        // Cumulative cost summary in the right column, under the input slot: the total
        // materials (icons + counts) and XP to climb from the item's current level to the
        // target. Red when unaffordable, but the Enchant button stays clickable so the
        // server can say exactly what's short. Full itemized bill lives in the ingredient tab.
        if (selectedEnchantment != null && minecraft != null && minecraft.player != null) {
            int current = currentEnchantLevel();
            int cx = leftPos + 104;
            if (!pairingOk()) {
                // No price for a combination that would do nothing; the Enchant button's tooltip
                // carries the explanation.
                graphics.text(font, Component.translatable("gui.buildaspell.arcane_altar.mismatch"),
                        cx, topPos + 48, 0xFFE06A6A, false);
            } else if (selectedLevel <= current) {
                graphics.text(font, "Have Lv " + current, cx, topPos + 48, 0xFFE06A6A, false);
            } else {
                int costColor = canAffordEnchant() ? COLOR_TEXT : 0xFFE06A6A;
                EnchantmentCostManager.CumulativeCost cost = cumulativeCost();
                graphics.text(font, "Cost \u2192 Lv " + selectedLevel, cx, topPos + 44, costColor, false);
                // Compact row: just the material icons (no count digits — those overflow the
                // narrow column and overlap). The itemized have/need counts live in the + tab.
                int ix = cx;
                for (Map.Entry<Item, Integer> need : cost.items().entrySet()) {
                    graphics.item(new ItemStack(need.getKey()), ix, topPos + 55);
                    ix += 16;
                }
                graphics.text(font, cost.xpPoints() + " XP", cx, topPos + 74, costColor, false);
            }
        }

        if (showHelp) {
            renderHelpPanel(graphics);
        }
        if (showIngredients) {
            renderIngredientGhosts(graphics);
            renderIngredientBill(graphics);
        }
    }

    /**
     * A help panel rendered beside the GUI (right side, or left if there's no room) so it
     * never overflows or covers the working area. Text is wrapped to the panel width and
     * covers what each enchant does, the flow, and how the cost scales.
     */
    private void renderHelpPanel(GuiGraphicsExtractor graphics) {
        int pw = 132, pad = 8;
        int px = leftPos + imageWidth + 16;   // clear of the side tabs
        if (px + pw > this.width) {
            px = leftPos - pw - 4;   // fall back to the left if the right runs off-screen
        }
        int contentW = pw - pad * 2;

        java.util.List<net.minecraft.util.FormattedCharSequence> lines = new java.util.ArrayList<>();
        addWrapped(lines, "§dMana Pool§r raises your maximum mana.", contentW);
        addWrapped(lines, "§dMana Regen§r restores mana faster.", contentW);
        addWrapped(lines, "§dSpell Power§r boosts spell damage and effect strength.", contentW);
        addWrapped(lines, "", contentW);
        addWrapped(lines, Component.translatable("gui.buildaspell.arcane_altar.help.flow"), contentW);
        addWrapped(lines, "", contentW);
        addWrapped(lines, Component.translatable("gui.buildaspell.arcane_altar.help.targets"), contentW);
        addWrapped(lines, "", contentW);
        addWrapped(lines, "Open the §5+§r tab and drop the materials into the ingredient slots.", contentW);
        addWrapped(lines, "", contentW);
        addWrapped(lines, "Reaching a level charges every level along the way (iron → gold → diamond → netherite) plus XP — no cheap skips to max.", contentW);
        addWrapped(lines, "", contentW);
        addWrapped(lines, "With the materials in and enough XP, one click enchants all the way up.", contentW);

        int headerH = 13;
        int ph = pad + headerH + lines.size() * 10 + pad;

        GuiTheme.drawPanel(graphics, px, topPos, pw, ph);
        int tx = px + pad;
        int ty = topPos + pad;
        graphics.text(font, Component.literal("How it works").withStyle(net.minecraft.ChatFormatting.BOLD),
                tx, ty, COLOR_HEADER, false);
        ty += headerH;
        for (net.minecraft.util.FormattedCharSequence line : lines) {
            graphics.text(font, line, tx, ty, COLOR_TEXT, false);
            ty += 10;
        }
    }

    private void addWrapped(java.util.List<net.minecraft.util.FormattedCharSequence> out, String text, int maxWidth) {
        if (text.isEmpty()) {
            out.add(net.minecraft.util.FormattedCharSequence.EMPTY);
            return;
        }
        out.addAll(font.split(Component.literal(text), maxWidth));
    }

    private void addWrapped(java.util.List<net.minecraft.util.FormattedCharSequence> out, Component text, int maxWidth) {
        out.addAll(font.split(text, maxWidth));
    }

    /** The itemized "have / need" bill shown inside the open ingredient tab, plus XP. */
    private void renderIngredientBill(GuiGraphicsExtractor graphics) {
        int tx = leftPos + INGREDIENT_PANEL_X + 6;
        graphics.text(font, Component.literal("Ingredients").withStyle(net.minecraft.ChatFormatting.BOLD),
                tx, topPos + 22, COLOR_HEADER, false);

        int ty = topPos + INGREDIENT_LEGEND_Y;
        if (selectedEnchantment == null || minecraft == null || minecraft.player == null) {
            graphics.text(font, "Pick a type", tx, ty, COLOR_TEXT, false);
            return;
        }
        if (!pairingOk()) {
            graphics.text(font, Component.translatable("gui.buildaspell.arcane_altar.mismatch"),
                    tx, ty, 0xFFE06A6A, false);
            return;
        }
        int current = currentEnchantLevel();
        if (selectedLevel <= current) {
            graphics.text(font, "Already Lv " + current, tx, ty, 0xFFE06A6A, false);
            return;
        }

        EnchantmentCostManager.CumulativeCost cost = cumulativeCost();
        for (Map.Entry<Item, Integer> need : cost.items().entrySet()) {
            int have = countInIngredients(need.getKey());
            graphics.item(new ItemStack(need.getKey()), tx, ty - 4);
            int c = have >= need.getValue() ? 0xFF8CE0A0 : 0xFFE06A6A;
            graphics.text(font, have + " / " + need.getValue(), tx + 20, ty, c, false);
            ty += 16;
        }
        int xpHave = EnchantmentCostManager.totalXpPoints(minecraft.player);
        int xpColor = xpHave >= cost.xpPoints() ? 0xFF8CE0A0 : 0xFFE06A6A;
        graphics.text(font, "XP " + xpHave + " / " + cost.xpPoints(), tx, ty + 2, xpColor, false);
    }

    private Button selectedButton() {
        if (selectedEnchantment == null) return null;
        return switch (selectedEnchantment.getPath()) {
            case "mana_pool"          -> manaPoolBtn;
            case "mana_regeneration"  -> manaRegenBtn;
            case "spell_power"        -> spellPowerBtn;
            default -> null;
        };
    }
}
