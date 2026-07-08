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
        this.pageText("Drive a blast downward into the earth across a wide enough span and the ground itself convulses.\\\n\\\nA shockwave tears across the ground, damaging and heaving entities skyward while rupturing the terrain itself.\\\n\\\nIncreased Area widens the quake's reach.");

        this.page("lightning_storm", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Lightning Storm");
        this.pageText("A bolt is an instant; a storm is a bolt given breadth and time.\\\n\\\nDrifting storm clouds gather over the area and rain down real lightning over time.\\\n\\\nIncreased Area calls more clouds across a wider front, while Duration keeps the storm raging longer.");
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
