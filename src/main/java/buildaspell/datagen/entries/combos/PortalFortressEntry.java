package buildaspell.datagen.entries.combos;

import com.klikli_dev.modonomicon.api.datagen.CategoryProviderBase;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookEntryModel;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class PortalFortressEntry extends EntryProvider {

    public PortalFortressEntry(CategoryProviderBase parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {
        this.page("void_rift", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Void Rift");
        this.pageText("A translocation held open long enough, and stretched wide enough, stops being a journey and becomes a door.\\\n\\\nVoid Rift portals allow instant travel between any two linked locations, even across dimensions.\\\n\\\nSee the Portal System entry in Advanced Systems for full details.");

        this.page("fortress", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Fortress");
        this.pageText("Conjuring that is asked to cover ground and to endure will raise walls instead of blocks.\\\n\\\nThe fortress is a hollow dome of impenetrable magical barrier raised around the caster; it fades away on its own, lasting longer with each Duration stack.\\\n\\\nExcellent for creating instant shelter in emergencies.");
    }

    @Override
    protected BookEntryModel additionalSetup(BookEntryModel entry) {
        entry.hideWhileLocked(true);
        entry.withCondition(
                this.condition().and(
                        this.condition().entryReadBuilder(
                                this.modLoc("spell_effects/teleportation")
                        ),
                        this.condition().entryReadBuilder(
                                this.modLoc("spell_effects/world_manip")
                        )
                )
        );
        return entry;
    }

    @Override
    protected String entryName() {
        return "Portal & Fortress";
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
        return BookIconModel.create(Items.OBSIDIAN);
    }

    @Override
    protected String entryId() {
        return "portal_fortress";
    }
}
