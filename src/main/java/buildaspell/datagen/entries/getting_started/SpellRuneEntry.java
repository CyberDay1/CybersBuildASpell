package buildaspell.datagen.entries.getting_started;

import com.klikli_dev.modonomicon.api.datagen.CategoryProviderBase;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookSpotlightPageModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class SpellRuneEntry extends EntryProvider {

    public SpellRuneEntry(CategoryProviderBase parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {
        this.page("spotlight", () -> BookSpotlightPageModel.create()
                .withItem(Items.AMETHYST_SHARD)
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Spell Rune");
        this.pageText("A Spell Rune is a Blank Rune that has been inscribed with a spell using the Spell Builder.\\\nEach Spell Rune holds a single spell configuration and can be assigned to one of 10 slots in the Ability Ring for quick casting.");

        this.page("usage", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Using Spell Runes");
        this.pageText("Open the Spell Builder (G key) to design your spell on a Blank Rune. Once saved, the Blank Rune transforms into a Spell Rune.\\\nAssign it to your Ability Ring (V key) and use the Cast Spell key or direct slot keybinds to cast.");
    }

    @Override
    protected String entryName() {
        return "Spell Rune";
    }

    @Override
    protected String entryDescription() {
        return "A rune inscribed with a spell";
    }

    @Override
    protected Pair<Integer, Integer> entryBackground() {
        return EntryBackground.DEFAULT;
    }

    @Override
    protected BookIconModel entryIcon() {
        return BookIconModel.create(Items.AMETHYST_SHARD);
    }

    @Override
    protected String entryId() {
        return "spell_rune";
    }
}
