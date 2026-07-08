package buildaspell.client.gui;

import buildaspell.network.DialPortalPacket;
import buildaspell.network.ResizePortalPacket;
import buildaspell.portal.PortalInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class PortalDialScreen extends Screen {
    // Palette shared with the Arcane Altar / Spell Builder screens (teal + amethyst accents).
    private static final int COLOR_HEADER = 0xFFB8ECF0; // title text
    private static final int COLOR_TEXT   = 0xFFDCECEC; // primary text
    private static final int COLOR_SUBTLE = 0xFF7E9AA0; // muted teal-grey
    private static final int COLOR_SCRIM  = 0xFF132028; // opaque dark teal backdrop

    private final UUID sourcePortalUUID;
    private final List<PortalInfo> discoveredPortals;
    private final float minSize;
    private final float maxSize;
    private float portalWidth;
    private float portalHeight;
    private PortalList portalList;

    public PortalDialScreen(UUID sourcePortalUUID, List<PortalInfo> discoveredPortals,
                            float currentWidth, float currentHeight,
                            float minSize, float maxSize) {
        super(Component.literal("Dial Portal"));
        this.sourcePortalUUID = sourcePortalUUID;
        this.discoveredPortals = discoveredPortals;
        this.portalWidth = currentWidth;
        this.portalHeight = currentHeight;
        this.minSize = minSize;
        this.maxSize = maxSize;
    }

    @Override
    protected void init() {
        super.init();

        int sliderAreaTop = this.height - 75;

        portalList = new PortalList(this.minecraft, this.width, sliderAreaTop - 55, 50, 35);

        for (PortalInfo info : discoveredPortals) {
            if (!info.getPortalUUID().equals(sourcePortalUUID)) {
                portalList.addEntry(portalList.new PortalEntry(info));
            }
        }

        addRenderableWidget(portalList);

        // Width slider: - / value / +
        int sliderX = this.width / 2 - 120;
        addRenderableWidget(Button.builder(Component.literal("-"), button -> {
            portalWidth = snapToStep(Math.max(minSize, portalWidth - 0.5f));
            sendResize();
        }).bounds(sliderX, sliderAreaTop, 20, 20).build());

        addRenderableWidget(Button.builder(Component.literal("+"), button -> {
            portalWidth = snapToStep(Math.min(maxSize, portalWidth + 0.5f));
            sendResize();
        }).bounds(sliderX + 90, sliderAreaTop, 20, 20).build());

        // Height slider: - / value / +
        int sliderX2 = this.width / 2 + 10;
        addRenderableWidget(Button.builder(Component.literal("-"), button -> {
            portalHeight = snapToStep(Math.max(minSize, portalHeight - 0.5f));
            sendResize();
        }).bounds(sliderX2, sliderAreaTop, 20, 20).build());

        addRenderableWidget(Button.builder(Component.literal("+"), button -> {
            portalHeight = snapToStep(Math.min(maxSize, portalHeight + 0.5f));
            sendResize();
        }).bounds(sliderX2 + 90, sliderAreaTop, 20, 20).build());

        // Clear Dial button
        addRenderableWidget(Button.builder(Component.literal("Clear Dial"), button -> {
            ClientPacketDistributor.sendToServer(new DialPortalPacket(sourcePortalUUID, null));
            onClose();
        }).bounds(this.width / 2 - 50, this.height - 30, 100, 20).build());
    }

    private float snapToStep(float value) {
        return Math.round(value * 2.0f) / 2.0f; // step 0.5
    }

    private void sendResize() {
        ClientPacketDistributor.sendToServer(new ResizePortalPacket(sourcePortalUUID, portalWidth, portalHeight));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // Opaque themed backdrop drawn BEFORE super so it sits BEHIND the portal list + slider
        // widgets — otherwise the list entries render unreadably over the bright in-world portal.
        graphics.fill(0, 0, this.width, this.height, COLOR_SCRIM);

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        graphics.centeredText(this.font, "Select Destination Portal", this.width / 2, 20, COLOR_HEADER);
        int portalCount = (int) discoveredPortals.stream()
                .filter(p -> !p.getPortalUUID().equals(sourcePortalUUID)).count();
        graphics.centeredText(this.font, "Discovered Portals: " + portalCount, this.width / 2, 35, COLOR_SUBTLE);

        // Render slider labels and values
        int sliderAreaTop = this.height - 75;
        int sliderX = this.width / 2 - 120;
        int sliderX2 = this.width / 2 + 10;

        graphics.text(this.font, String.format("Width: %.1f", portalWidth), sliderX + 25, sliderAreaTop + 6, COLOR_TEXT, false);
        graphics.text(this.font, String.format("Height: %.1f", portalHeight), sliderX2 + 25, sliderAreaTop + 6, COLOR_TEXT, false);
    }

    private void dialPortal(UUID targetUUID) {
        ClientPacketDistributor.sendToServer(new DialPortalPacket(sourcePortalUUID, targetUUID));
        onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    class PortalList extends ObjectSelectionList<PortalList.PortalEntry> {

        public PortalList(Minecraft mc, int width, int height, int top, int itemHeight) {
            super(mc, width, height, top, itemHeight);
        }

        public int addEntry(PortalEntry entry) {
            return super.addEntry(entry);
        }

        @Override
        public void setSelected(@Nullable PortalEntry entry) {
            super.setSelected(entry);
            if (entry != null) {
                dialPortal(entry.portalInfo.getPortalUUID());
            }
        }

        @Override
        public int getRowWidth() {
            return 300;
        }

        class PortalEntry extends ObjectSelectionList.Entry<PortalEntry> {
            final PortalInfo portalInfo;

            PortalEntry(PortalInfo portalInfo) {
                this.portalInfo = portalInfo;
            }

            @Override
            public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovering, float partialTick) {
                int left = getX();
                int top = getY();
                int width = getWidth();
                int height = getHeight();

                graphics.text(font, portalInfo.getName(), left + 5, top + 2, 0xFFFFFF, false);

                String dimensionName = portalInfo.getDimension().identifier().getPath();
                String coords = String.format("%s @ %.0f, %.0f, %.0f",
                        dimensionName,
                        portalInfo.getPosition().x,
                        portalInfo.getPosition().y,
                        portalInfo.getPosition().z);
                graphics.text(font, coords, left + 5, top + 14, 0xAAAAAA, false);

                if (hovering) {
                    graphics.fill(left, top, left + width, top + height, 0x30FFFFFF);
                }
            }

            @Override
            public Component getNarration() {
                String dimensionName = portalInfo.getDimension().identifier().getPath();
                return Component.literal(portalInfo.getName() + " in " + dimensionName);
            }
        }
    }
}
