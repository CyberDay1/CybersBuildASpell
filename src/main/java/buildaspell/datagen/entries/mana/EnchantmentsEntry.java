package buildaspell.datagen.entries.mana;

import com.klikli_dev.modonomicon.api.datagen.CategoryProviderBase;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class EnchantmentsEntry extends EntryProvider {

    public EnchantmentsEntry(CategoryProviderBase parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {
        this.page("overview", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Arcane Enchantments");
        this.pageText("The Arcane Altar offers three enchantments exclusive to Build-A-Spell.\\\n\\\nUnlike vanilla enchantments, these have no level cap: you can enchant the same piece of armor or tool repeatedly, stacking the effect higher each time.");

        this.page("scaling", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Cost Scaling");
        this.pageText("Each successive level costs more experience than the last, scaling in tiers.\\\n\\\nEarly levels are cheap, but high-level enchantments require significant experience investment.\\\n\\\nThe three enchantments are: Mana Pool (+10 maximum mana per level), Mana Regeneration (+2 mana per second per level), and Spell Power (+5 Spell Power per level).");
    }

    @Override
    protected String entryName() {
        return "Enchantments";
    }

    @Override
    protected String entryDescription() {
        return "Arcane Altar enchantments";
    }

    @Override
    protected Pair<Integer, Integer> entryBackground() {
        return EntryBackground.DEFAULT;
    }

    @Override
    protected BookIconModel entryIcon() {
        return BookIconModel.create(Items.ENCHANTED_BOOK);
    }

    @Override
    protected String entryId() {
        return "enchantments";
    }
}
