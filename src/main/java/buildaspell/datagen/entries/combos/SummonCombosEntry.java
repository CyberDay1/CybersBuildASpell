package buildaspell.datagen.entries.combos;

import com.klikli_dev.modonomicon.api.datagen.CategoryProviderBase;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookEntryModel;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class SummonCombosEntry extends EntryProvider {

    public SummonCombosEntry(CategoryProviderBase parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {
        this.page("golem_vex", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Iron Golem & Vexes");
        this.pageText("Summoning is a school of pairings: what you weave alongside the calling decides what answers.\\\n\\\nBolster the calling with restorative magic and a friendly Iron Golem answers.\\\n\\\nLace it instead with violence and spatial trickery, and a swarm of allied Vexes flickers into being to harry nearby enemies.");

        this.page("skeleton_vindicator", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Skeletons & Vindicators");
        this.pageText("Call with the storm's own fury and a squad of Skeleton archers rises to your banner.\\\n\\\nPour raw, widened power into a violent calling and ranks of allied Vindicators march out instead.\\\n\\\nBoth armies fight for you, and both take real strength to raise: weak workings summon nothing.");
    }

    @Override
    protected BookEntryModel additionalSetup(BookEntryModel entry) {
        entry.hideWhileLocked(true);
        entry.withCondition(
                this.condition().entryRead(
                        this.modLoc("spell_effects/summoning")
                )
        );
        return entry;
    }

    @Override
    protected String entryName() {
        return "Summoning Combos";
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
        return BookIconModel.create(Items.IRON_BLOCK);
    }

    @Override
    protected String entryId() {
        return "summon_combos";
    }
}
