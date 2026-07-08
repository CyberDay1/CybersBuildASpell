package buildaspell.datagen.entries.combos;

import com.klikli_dev.modonomicon.api.datagen.CategoryProviderBase;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookEntryModel;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class FloodCombosEntry extends EntryProvider {

    public FloodCombosEntry(CategoryProviderBase parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {
        this.page("flood", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Flood");
        this.pageText("Combine Create Water with Increased Area + Chain (min 4 components) to unleash a devastating flood.\\\nWater source blocks are placed rapidly across a wide area, filling in terrain and creating a massive body of water. Use with caution: this can dramatically reshape the landscape.");

        this.page("flood_lava", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Flood Lava");
        this.pageText("Combine Create Water + Ignite with Increased Area + Chain (min 6 components) to create a lava flood instead of water.\\\nThis is the most expensive combo in the game, requiring 6 minimum components. The resulting lava flow is devastating to both entities and terrain.");

        this.page("geyser", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Geyser");
        this.pageText("Combine Create Water + Launch with Increased Area (min 3 components) to erupt a roaring geyser.\\\nA column of water blasts upward from the target, hurling every entity caught above it high into the air. The jet is purely visual and leaves no water behind, making it a handy mobility tool or a brutal fall-trap. Increased Area widens the eruption.");
    }

    @Override
    protected BookEntryModel additionalSetup(BookEntryModel entry) {
        entry.hideWhileLocked(true);
        entry.withCondition(
                this.condition().entryRead(
                        this.modLoc("spell_effects/world_manip")
                )
        );
        return entry;
    }

    @Override
    protected String entryName() {
        return "Flood Combos";
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
        return BookIconModel.create(Items.WATER_BUCKET);
    }

    @Override
    protected String entryId() {
        return "flood_combos";
    }
}
