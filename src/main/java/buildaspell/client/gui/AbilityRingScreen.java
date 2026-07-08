package buildaspell.client.gui;

import buildaspell.config.ModConfig;
import buildaspell.network.SetActiveSlotPacket;
import buildaspell.registry.ModAttachments;
import buildaspell.spell.PlayerSpellSlots;
import buildaspell.spell.Spell;
import buildaspell.spell.SpellEffect;
import buildaspell.spell.SpellSlot;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AbilityRingScreen extends Screen {
    private static final int RING_RADIUS = 80;
    private static final int SLOT_SIZE = 40;
    private static final int HOVER_GROW = 4;      // hovered slot swells by this many px
    private static final int CORNER_RADIUS = 5;   // slot corner rounding
    private static final int BORDER = 2;          // slot border thickness
    private int hoveredSlot = -1;

    public AbilityRingScreen() {
        super(Component.literal("Ability Ring"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int transparency = (int) (ModConfig.client().abilityRingTransparency.get() * 255);
        int backgroundColor = (transparency << 24) | (GuiTheme.SCRIM & 0x00FFFFFF);
        graphics.fill(0, 0, this.width, this.height, backgroundColor);

        super.render(graphics, mouseX, mouseY, partialTick);

        if (minecraft == null || minecraft.player == null) {
            return;
        }

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        PlayerSpellSlots spellSlots = minecraft.player.getData(ModAttachments.PLAYER_SPELL_SLOTS.get());

        hoveredSlot = -1;

        for (int i = 0; i < 10; i++) {
            double angle = Math.toRadians(i * 36 - 90);
            int slotX = centerX + (int) (Math.cos(angle) * RING_RADIUS);
            int slotY = centerY + (int) (Math.sin(angle) * RING_RADIUS);

            boolean isHovered = mouseX >= slotX - SLOT_SIZE / 2 && mouseX <= slotX + SLOT_SIZE / 2 &&
                    mouseY >= slotY - SLOT_SIZE / 2 && mouseY <= slotY + SLOT_SIZE / 2;

            if (isHovered) {
                hoveredSlot = i;
            }

            boolean isActive = i == spellSlots.getActiveSlot();
            SpellSlot slot = spellSlots.getSlot(i);
            boolean hasSpell = slot != null && slot.hasSpell();
            boolean hasEffect = hasSpell && !slot.getSpell().getEffects().isEmpty();

            int baseColor = hasEffect ? getEffectColor(slot.getSpell().getEffects().get(0)) : 0xFF3A3A46;

            // Hovered slot swells slightly; hit-test stays on the base size to avoid jitter.
            int size = SLOT_SIZE + (isHovered ? HOVER_GROW : 0);
            int half = size / 2;
            int x0 = slotX - half, y0 = slotY - half, x1 = slotX + half, y1 = slotY + half;

            // Drop shadow for depth.
            roundedRect(graphics, x0 + 2, y0 + 3, x1 + 2, y1 + 3, CORNER_RADIUS, 0x55000000);

            // Border: active pulses amethyst, hover glows cyan-mint, otherwise a darkened tint of the fill.
            int borderColor;
            if (isActive) {
                float p = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 400.0 * Math.PI);
                borderColor = lerpColor(GuiTheme.AMETHYST_DIM, 0xFFD8B0FF, p);
            } else if (isHovered) {
                borderColor = GuiTheme.HEADER;
            } else {
                borderColor = 0xFF000000 | darkenColor(baseColor, 0.7f);
            }
            roundedRect(graphics, x0, y0, x1, y1, CORNER_RADIUS, borderColor);

            // Inner face: top-lit vertical gradient.
            int topColor, botColor;
            if (hasSpell) {
                topColor = lightenColor(baseColor, 1.18f);
                botColor = 0xFF000000 | darkenColor(baseColor, 0.5f);
            } else {
                // Empty slot: recessed teal-obsidian, matching the shared panel family.
                topColor = 0xFF163038;
                botColor = 0xFF0A1518;
            }
            roundedGradient(graphics, x0 + BORDER, y0 + BORDER, x1 - BORDER, y1 - BORDER,
                    CORNER_RADIUS - 1, topColor, botColor);

            // Soft top sheen.
            roundedRect(graphics, x0 + BORDER, y0 + BORDER, x1 - BORDER, y0 + BORDER + 3,
                    CORNER_RADIUS - 1, 0x28FFFFFF);

            if (hasSpell) {
                String name = slot.getName().isEmpty() ? "Spell " + (i + 1) : slot.getName();
                int maxWidth = size - 8;
                if (font.width(name) > maxWidth) {
                    name = font.plainSubstrByWidth(name, maxWidth - 6) + "..";
                }
                int textWidth = font.width(name);
                graphics.drawString(font, name, slotX - textWidth / 2, slotY - 6, 0xFFFFFFFF, true);

                // Slot-number badge, bottom-right corner.
                String num = String.valueOf(i + 1);
                graphics.drawString(font, num, x1 - BORDER - font.width(num) - 2, y1 - BORDER - 9, 0xFFC8C8D2, true);
            } else {
                String label = String.valueOf(i + 1);
                int textWidth = font.width(label);
                graphics.drawString(font, label, slotX - textWidth / 2, slotY - 4, 0xFFBCBCC6, true);
            }
        }

        if (hoveredSlot >= 0) {
            SpellSlot slot = spellSlots.getSlot(hoveredSlot);
            if (slot != null && slot.hasSpell()) {
                Spell spell = slot.getSpell();
                String spellName = slot.getName().isEmpty() ? "Spell " + (hoveredSlot + 1) : slot.getName();

                List<Component> tooltipComponents = new ArrayList<>();
                tooltipComponents.add(Component.literal(spellName)
                        .withStyle(ChatFormatting.WHITE)
                        .withStyle(ChatFormatting.BOLD));
                tooltipComponents.add(Component.empty());

                if (spell.getDelivery() != null) {
                    tooltipComponents.add(Component.literal("Delivery: ")
                            .withStyle(ChatFormatting.BLUE)
                            .append(Component.literal(formatName(spell.getDelivery().getSerializedName()))
                                    .withStyle(ChatFormatting.WHITE)));
                }

                if (!spell.getEffects().isEmpty()) {
                    tooltipComponents.add(Component.literal("Effects:")
                            .withStyle(ChatFormatting.RED));
                    for (SpellEffect effect : spell.getEffects()) {
                        tooltipComponents.add(Component.literal("  ")
                                .append(Component.literal(formatName(effect.getSerializedName()))
                                        .withStyle(ChatFormatting.WHITE)));
                    }
                }

                if (!spell.getModifiers().isEmpty()) {
                    tooltipComponents.add(Component.literal("Modifiers: ")
                            .withStyle(ChatFormatting.GREEN)
                            .append(Component.literal(String.valueOf(spell.getModifiers().size()))
                                    .withStyle(ChatFormatting.WHITE)));
                }

                tooltipComponents.add(Component.empty());
                tooltipComponents.add(Component.literal("Mana Cost: ")
                        .withStyle(ChatFormatting.YELLOW)
                        .append(Component.literal(String.format("%.1f", spell.getManaCost()))
                                .withStyle(ChatFormatting.WHITE)));
                tooltipComponents.add(Component.literal("Range: ")
                        .withStyle(ChatFormatting.AQUA)
                        .append(Component.literal(String.format("%.1f", spell.getRange()) + " blocks")
                                .withStyle(ChatFormatting.WHITE)));

                List<FormattedCharSequence> lines = tooltipComponents.stream()
                        .map(Component::getVisualOrderText).collect(Collectors.toList());
                int tooltipWidth = lines.stream().mapToInt(font::width).max().orElse(0);
                int tooltipHeight = lines.size() * 10;
                int tx = mouseX + 12;
                int ty = mouseY - 12;
                if (tx + tooltipWidth + 6 > this.width) tx = mouseX - tooltipWidth - 16;
                if (ty + tooltipHeight + 6 > this.height) ty = this.height - tooltipHeight - 6;
                // Lift the tooltip above the ring and keep the body fully opaque so the
                // slot labels underneath it can't bleed through.
                graphics.pose().pushPose();
                graphics.pose().translate(0f, 0f, 400f);
                graphics.fill(tx - 3, ty - 3, tx + tooltipWidth + 3, ty + tooltipHeight + 3, 0xFF0E2026);
                graphics.fill(tx - 2, ty - 2, tx + tooltipWidth + 2, ty + tooltipHeight + 2, 0xFF0E2026);
                graphics.fill(tx - 3, ty - 3, tx + tooltipWidth + 3, ty - 2, 0xFFAE74E8);
                graphics.fill(tx - 3, ty + tooltipHeight + 2, tx + tooltipWidth + 3, ty + tooltipHeight + 3, 0xFF3C6A74);
                for (int j = 0; j < lines.size(); j++) {
                    graphics.drawString(font, lines.get(j), tx, ty + j * 10, 0xFFFFFFFF, false);
                }
                graphics.pose().popPose();
            } else {
                centerPanel(graphics, centerX, centerY - 16, centerY + 28, 52);
                graphics.drawCenteredString(font,
                        Component.literal("Empty Slot " + (hoveredSlot + 1)).withStyle(ChatFormatting.GRAY),
                        centerX, centerY - 10, 0xFFFFFFFF);
                graphics.drawCenteredString(font,
                        Component.literal("No spell configured").withStyle(ChatFormatting.DARK_GRAY),
                        centerX, centerY + 5, 0xFF888888);
                graphics.drawCenteredString(font,
                        Component.literal("ESC to close"),
                        centerX, centerY + 20, 0xFF888888);
            }
        } else {
            centerPanel(graphics, centerX, centerY - 16, centerY + 22, 46);
            graphics.drawCenteredString(font,
                    Component.literal("Select a Spell"),
                    centerX, centerY - 10, 0xFFFFFFFF);
            graphics.drawCenteredString(font,
                    Component.literal("ESC to close"),
                    centerX, centerY + 10, 0xFF888888);
        }
    }

    private void centerPanel(GuiGraphics graphics, int cx, int top, int bottom, int halfWidth) {
        // Amethyst frame over a teal-obsidian body — the shared panel language.
        roundedRect(graphics, cx - halfWidth - 1, top - 1, cx + halfWidth + 1, bottom + 1, 6, 0x90AE74E8);
        roundedGradient(graphics, cx - halfWidth, top, cx + halfWidth, bottom, 6, 0xF0153038, 0xF00A1518);
    }

    /** Filled rounded rectangle (circular corners of the given radius). */
    private void roundedRect(GuiGraphics graphics, int x0, int y0, int x1, int y1, int radius, int color) {
        GuiTheme.roundedRect(graphics, x0, y0, x1, y1, radius, color);
    }

    /** Rounded rectangle filled with a top-to-bottom vertical gradient. */
    private void roundedGradient(GuiGraphics graphics, int x0, int y0, int x1, int y1, int radius, int topColor, int bottomColor) {
        GuiTheme.roundedGradient(graphics, x0, y0, x1, y1, radius, topColor, bottomColor);
    }

    private int lerpColor(int colorA, int colorB, float t) {
        return GuiTheme.lerpColor(colorA, colorB, t);
    }

    private int lightenColor(int color, float factor) {
        return GuiTheme.lightenColor(color, factor);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && hoveredSlot >= 0) {
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f, 1.0f);
            }
            PacketDistributor.sendToServer(new SetActiveSlotPacket(hoveredSlot));
            this.onClose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        int slot = -1;

        if (keyCode >= org.lwjgl.glfw.GLFW.GLFW_KEY_1 && keyCode <= org.lwjgl.glfw.GLFW.GLFW_KEY_9) {
            slot = keyCode - org.lwjgl.glfw.GLFW.GLFW_KEY_1;
        } else if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_0) {
            slot = 9;
        }

        if (slot >= 0 && slot < 10) {
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f, 1.0f);
            }
            PacketDistributor.sendToServer(new SetActiveSlotPacket(slot));
            this.onClose();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private String formatName(String id) {
        String[] words = id.split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!result.isEmpty()) {
                result.append(" ");
            }
            result.append(word.substring(0, 1).toUpperCase()).append(word.substring(1));
        }
        return result.toString();
    }

    private int getEffectColor(SpellEffect effect) {
        if (effect == null) return 0xFF888888;
        return switch (effect) {
            case DAMAGE -> 0xFFFF4444;
            case IGNITE -> 0xFFFF8800;
            case FREEZE -> 0xFF88DDFF;
            case HEAL -> 0xFFFF88AA;
            case POISON -> 0xFF88FF44;
            case WITHER -> 0xFF444444;
            case LIGHTNING -> 0xFFFFFF44;
            case TELEPORT, BLINK, RECALL -> 0xFF8844FF;
            case EXPLOSION -> 0xFFFF2200;
            case PULL, PUSH -> 0xFFAABBCC;
            case SHIELD -> 0xFFFFDD44;
            case SUMMON -> 0xFF44AAFF;
            case LAUNCH, YEET, SLAM -> 0xFFFFAA00;
            case LEVITATION, SLOW_FALL -> 0xFFDDDDFF;
            case GROWTH, REAP -> 0xFF44AA44;
            case CREATE_WATER, EVAPORATE_WATER -> 0xFF4488FF;
            case LIGHT -> 0xFFFFFFAA;
            case INVISIBILITY -> 0xFF888899;
            case SPEED, HASTE -> 0xFF44DDFF;
            case SWAP -> 0xFFDD88FF;
            case MARK -> 0xFFFF4488;
            case PICKUP -> 0xFFCCBB88;
            case CLEANSE -> 0xFFAAFFAA;
            case CHARM, BLIND -> 0xFF9944AA;
            case BREAK, CONJURE -> 0xFFAA8866;
            case SATURATION -> 0xFFFFAA44;
            case SLOW, WEAKEN, ROOT -> 0xFF6677AA;
            case STRENGTHEN, RESIST -> 0xFFCC5544;
            case REGENERATE -> 0xFFFF66AA;
            case NIGHT_VISION, WATER_BREATHING -> 0xFF4488DD;
            case GRAPPLE, GUST -> 0xFFBBDDEE;
        };
    }

    private int darkenColor(int color, float factor) {
        return GuiTheme.darkenColor(color, factor);
    }
}
