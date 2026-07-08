package buildaspell.datagen.entries.effects;

import com.klikli_dev.modonomicon.api.datagen.CategoryProviderBase;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class SummoningEntry extends EntryProvider {

    public SummoningEntry(CategoryProviderBase parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {
        this.page("summon", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Summon (50 mana)");
        this.pageText("The Summon effect conjures an allied entity at the target location.\\\n\\\nThe type of entity summoned depends on the other effects and modifiers in the spell.\\\n\\\nOn its own, Summon produces a basic ally.\\\n\\\nCombined with specific effects, it can summon Iron Golems, Vexes, Skeletons, or Vindicators.");

        this.page("charm", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Charm (35 mana)");
        this.pageText("Charm pacifies every mob within range: they drop whatever they were attacking and cannot pick a new target while the effect lasts, wandering in a disoriented haze instead.\\\n\\\nThe pacify lasts longer when boosted with the Prolonged or Increased Power modifiers.\\\n\\\nUnlike Summon, Charm works on creatures already in the world rather than conjuring new ones.");
    }

    @Override
    protected String entryName() {
        return "Summoning";
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
        return BookIconModel.create(Items.BONE);
    }

    @Override
    protected String entryId() {
        return "summoning";
    }
}
