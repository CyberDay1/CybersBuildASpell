package buildaspell.datagen.entries.getting_started;

import com.klikli_dev.modonomicon.api.datagen.CategoryProviderBase;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookSpotlightPageModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class ArcaneAltarEntry extends EntryProvider {

    public ArcaneAltarEntry(CategoryProviderBase parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {
        this.page("spotlight", () -> BookSpotlightPageModel.create()
                .withItem(Items.ENCHANTING_TABLE)
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Arcane Altar");
        this.pageText("The Arcane Altar is the enchanting station for Build-A-Spell.\\\nUnlike the vanilla Enchanting Table, the Arcane Altar supports three unique enchantments with no level cap, letting you push your magical abilities beyond normal limits.");

        this.page("enchanting", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Enchantments");
        this.pageText("The three enchantments available at the Arcane Altar are Mana Pool (increases maximum mana), Mana Regeneration (speeds mana recovery), and Spell Power (amplifies spell effects).\\\nEach enchantment can be applied multiple times with increasing cost per tier.");
    }

    @Override
    protected String entryName() {
        return "Arcane Altar";
    }

    @Override
    protected String entryDescription() {
        return "The enchanting station for Build-A-Spell";
    }

    @Override
    protected Pair<Integer, Integer> entryBackground() {
        return EntryBackground.DEFAULT;
    }

    @Override
    protected BookIconModel entryIcon() {
        return BookIconModel.create(Items.ENCHANTING_TABLE);
    }

    @Override
    protected String entryId() {
        return "arcane_altar";
    }
}
