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
        this.pageText("Combine Teleport with Duration + Increased Area (min 3 components) to open a Void Rift portal.\\\nThese portals allow instant travel between any two linked locations, even across dimensions. See the Portal System entry in Advanced Systems for full details.");

        this.page("fortress", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Fortress");
        this.pageText("Combine Conjure with Increased Area + Duration (min 2 components) to instantly construct a protective fortress around the caster.\\\nThe fortress is built from the configured Conjure block list and persists for the spell's duration. Excellent for creating instant shelter in emergencies.");
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
