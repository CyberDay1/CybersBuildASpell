package buildaspell.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class SpellImportScreen extends Screen {
    private final Screen parent;
    private final Consumer<String> onImport;
    private EditBox codeField;

    public SpellImportScreen(Screen parent, Consumer<String> onImport) {
        super(Component.literal("Import Spell"));
        this.parent = parent;
        this.onImport = onImport;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        codeField = new EditBox(this.font, centerX - 150, centerY - 20, 300, 20, Component.literal("Spell Code"));
        codeField.setMaxLength(Short.MAX_VALUE);
        codeField.setHint(Component.literal("Paste spell code here"));
        addRenderableWidget(codeField);

        addRenderableWidget(Button.builder(
                Component.literal("Import"),
                button -> {
                    String code = codeField.getValue().trim();
                    if (!code.isEmpty()) {
                        onImport.accept(code);
                        minecraft.setScreenAndShow(parent);
                    }
                }
        ).bounds(centerX - 80, centerY + 20, 70, 20).build());

        addRenderableWidget(Button.builder(
                Component.literal("Cancel"),
                button -> minecraft.setScreenAndShow(parent)
        ).bounds(centerX + 10, centerY + 20, 70, 20).build());

        addRenderableWidget(Button.builder(
                Component.literal("Paste"),
                button -> {
                    if (minecraft != null) {
                        String clipboard = minecraft.keyboardHandler.getClipboard();
                        codeField.setValue(clipboard);
                    }
                }
        ).bounds(centerX - 80, centerY - 50, 160, 20).build());

        setInitialFocus(codeField);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        graphics.centeredText(this.font, "Import Spell", centerX, centerY - 80, 0xFFFFFF);
        graphics.centeredText(this.font, "Paste the spell code below:", centerX, centerY - 60, 0xAAAAAA);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
