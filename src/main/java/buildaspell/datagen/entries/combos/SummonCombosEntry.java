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
        this.pageText("Iron Golem: Combine Summon + Heal with Increased Power (min 3 components) to summon a friendly Iron Golem.\\\nVexes: Combine Summon + Teleport + Damage (min 3 components) to summon a swarm of allied Vexes that attack nearby enemies.");

        this.page("skeleton_vindicator", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Skeletons & Vindicators");
        this.pageText("Skeletons: Combine Summon + Lightning with Increased Power (min 3 components) to summon a squad of Skeleton archers.\\\nVindicators: Combine Summon + Damage with Increased Power x2 + Increased Area (min 4 components) to summon a group of allied Vindicators.");
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
