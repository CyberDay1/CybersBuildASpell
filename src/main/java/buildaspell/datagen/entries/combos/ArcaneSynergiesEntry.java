package buildaspell.datagen.entries.combos;

import com.klikli_dev.modonomicon.api.datagen.CategoryProviderBase;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class ArcaneSynergiesEntry extends EntryProvider {

    public ArcaneSynergiesEntry(CategoryProviderBase parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {
        this.page("teaser", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Hidden Combinations");
        this.pageText("Certain spell configurations resonate into something far greater than their parts.\\\n\\\nHidden combinations wait in these pages, and rumor hints at what each one becomes.\\\n\\\nThe Summoning arts are said to call iron guardians, swarming vexes, skeleton archers, and ranks of armored vindicators.\\\n\\\nMovement and Force can tear open a black hole, raise a roaring tornado, grant lasting flight, or snap you free in a desperate escape.\\\n\\\nWorld Manipulation can drown the land beneath water or lava, while Teleportation can split open a void rift or conjure an instant fortress.\\\n\\\nThe exact recipe for each stays sealed until you study the spell effect it draws upon.\\\n\\\nRead an effect's chapter in full, and the combinations built on it reveal themselves here on their own.");
    }

    @Override
    protected String entryName() {
        return "Arcane Synergies";
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
        return BookIconModel.create(Items.NETHER_STAR);
    }

    @Override
    protected String entryId() {
        return "arcane_synergies";
    }
}
