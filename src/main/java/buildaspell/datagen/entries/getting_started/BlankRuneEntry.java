package buildaspell.datagen.entries.getting_started;

import com.klikli_dev.modonomicon.api.datagen.CategoryProviderBase;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookSpotlightPageModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class BlankRuneEntry extends EntryProvider {

    public BlankRuneEntry(CategoryProviderBase parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {
        this.page("spotlight", () -> BookSpotlightPageModel.create()
                .withItem(Items.PAPER)
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Blank Rune");
        this.pageText("The Blank Rune is the foundation of all spell creation.\\\nIt gathers magical essence as you practice magic: every hostile mob you slay and every spell you cast feeds power into the rune.");

        this.page("details", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Gathering Essence");
        this.pageText("A Blank Rune fills with essence from two sources. The first is defeating enemies, where bosses such as the Ender Dragon and Wither grant far more than common mobs. The second is casting spells, where costlier spells yield more essence.\\\nOnce it holds enough, the rune automatically transforms into a Spell Rune in your inventory.");

        this.page("values", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Essence Values");
        this.pageText("A Blank Rune needs 200 essence to become a Spell Rune. Each hostile mob you slay grants 1 essence, while bosses such as the Ender Dragon and Wither grant 50. Casting grants essence equal to a quarter of the mana spent: 0.25 per point.\\\nA server admin can retune every one of these values, or switch off cast progression entirely.");
    }

    @Override
    protected String entryName() {
        return "Blank Rune";
    }

    @Override
    protected String entryDescription() {
        return "The foundation of spell creation";
    }

    @Override
    protected Pair<Integer, Integer> entryBackground() {
        return EntryBackground.DEFAULT;
    }

    @Override
    protected BookIconModel entryIcon() {
        return BookIconModel.create(Items.PAPER);
    }

    @Override
    protected String entryId() {
        return "blank_rune";
    }
}
