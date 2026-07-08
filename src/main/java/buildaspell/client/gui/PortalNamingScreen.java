package buildaspell.client.gui;

import buildaspell.network.NamePortalPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;

public class PortalNamingScreen extends Screen {
    // Palette shared with the Arcane Altar / Spell Builder screens (teal panel + amethyst accents).
    private static final int COLOR_BORDER = 0xFF3C6A74; // teal frame edge
    private static final int COLOR_HEADER = 0xFFB8ECF0; // title text
    private static final int COLOR_TEXT   = 0xFFDCECEC; // primary text
    private static final int COLOR_SCRIM  = 0xFF132028; // opaque dark teal backdrop

    private final UUID portalUUID;
    private final String currentName;
    private EditBox nameField;

    public PortalNamingScreen(UUID portalUUID, String currentName) {
        super(Component.literal("Name Portal"));
        this.portalUUID = portalUUID;
        this.currentName = currentName;
    }

    @Override
    protected void init() {
        super.init();

        nameField = new EditBox(this.font, this.width / 2 - 100, this.height / 2 - 10, 200, 20, Component.literal("Portal Name"));
        nameField.setMaxLength(32);
        nameField.setValue(currentName);
        addRenderableWidget(nameField);
        // Route keyboard focus to the field at the Screen level. Setting the widget's own focus
        // flag alone leaves the Screen's focused listener null, so charTyped/keyPressed are
        // dispatched to nothing — which is why the box could be clicked but never typed into.
        setInitialFocus(nameField);

        addRenderableWidget(Button.builder(Component.literal("Confirm"), button -> {
            String newName = nameField.getValue().trim();
            if (!newName.isEmpty()) {
                PacketDistributor.sendToServer(new NamePortalPacket(portalUUID, newName));
            }
            onClose();
        }).bounds(this.width / 2 - 105, this.height / 2 + 20, 100, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> {
            onClose();
        }).bounds(this.width / 2 + 5, this.height / 2 + 20, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        // Opaque themed backdrop behind the field + buttons.
        graphics.fill(0, 0, this.width, this.height, COLOR_SCRIM);
        super.render(graphics, mouseX, mouseY, partialTick);

        // Teal frame around the field + buttons so the dialog reads as an intentional themed menu.
        int fx = this.width / 2 - 115;
        int fy = this.height / 2 - 48;
        int fw = 230;
        int fh = 96;
        graphics.fill(fx, fy, fx + fw, fy + 1, COLOR_BORDER);
        graphics.fill(fx, fy + fh - 1, fx + fw, fy + fh, COLOR_BORDER);
        graphics.fill(fx, fy, fx + 1, fy + fh, COLOR_BORDER);
        graphics.fill(fx + fw - 1, fy, fx + fw, fy + fh, COLOR_BORDER);

        graphics.drawCenteredString(this.font, "Name This Rift", this.width / 2, this.height / 2 - 40, COLOR_HEADER);
        graphics.drawCenteredString(this.font, "Enter Portal Name:", this.width / 2, this.height / 2 - 28, COLOR_TEXT);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
