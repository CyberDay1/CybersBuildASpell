package buildaspell.datagen.entries.combos;

import com.klikli_dev.modonomicon.api.datagen.CategoryProviderBase;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookEntryModel;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class UpheavalCombosEntry extends EntryProvider {

    public UpheavalCombosEntry(CategoryProviderBase parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {
        this.page("earthquake", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Earthquake");
        this.pageText("Combine Slam + Explosion with Increased Area (min 3 components) to trigger an earthquake.\\\nA shockwave tears across the ground, damaging and heaving entities skyward while rupturing the terrain itself. Increased Area widens the quake's reach.");

        this.page("lightning_storm", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Lightning Storm");
        this.pageText("Combine Lightning with Increased Area + Duration (min 3 components) to brew a lightning storm.\\\nDrifting storm clouds gather over the area and rain down real lightning over time. Increased Area calls more clouds across a wider front, while Duration keeps the storm raging longer.");
    }

    @Override
    protected BookEntryModel additionalSetup(BookEntryModel entry) {
        entry.hideWhileLocked(true);
        entry.withCondition(
                this.condition().entryRead(
                        this.modLoc("spell_effects/direct_damage")
                )
        );
        return entry;
    }

    @Override
    protected String entryName() {
        return "Upheaval Combos";
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
        return BookIconModel.create(Items.LIGHTNING_ROD);
    }

    @Override
    protected String entryId() {
        return "upheaval_combos";
    }
}
