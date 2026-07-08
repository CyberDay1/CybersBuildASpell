package buildaspell.datagen.entries.advanced;

import com.klikli_dev.modonomicon.api.datagen.CategoryProviderBase;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class PortalSystemEntry extends EntryProvider {

    public PortalSystemEntry(CategoryProviderBase parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {
        this.page("overview", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Void Rifts");
        this.pageText("Void Rifts are stable portals created by the Void Rift combo spell. Each rift is a swirling gateway: step into one and it carries you to the destination you have dialed, even across dimensions.");

        this.page("creation", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Creating Portals");
        this.pageText("To create a Void Rift, cast a spell containing the Teleport effect with Duration and Increased Area modifiers. This triggers the Void Rift combo.\\\nEach portal must be named and can be linked to any other named portal you have created.");

        this.page("linking", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Portal Linking");
        this.pageText("Portals are linked by name: dial a destination portal's name to connect them. Connected portals work bidirectionally.\\\nVoid Rifts work across dimensions, letting you create instant travel networks between the Overworld, Nether, and End. The server config can limit the maximum portals per player.");
    }

    @Override
    protected String entryName() {
        return "Portal System";
    }

    @Override
    protected String entryDescription() {
        return "";
    }

    @Override
    protected Pair<Integer, Integer> entryBackground() {
        return EntryBackground.DEFAULT;
    }

    @Override
    protected BookIconModel entryIcon() {
        return BookIconModel.create(Items.CRYING_OBSIDIAN);
    }

    @Override
    protected String entryId() {
        return "portal_system";
    }
}
