package buildaspell.datagen.entries.getting_started;

import com.klikli_dev.modonomicon.api.datagen.CategoryProviderBase;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookSpotlightPageModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class WandsEntry extends EntryProvider {

    public WandsEntry(CategoryProviderBase parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {
        this.page("spotlight", () -> BookSpotlightPageModel.create()
                .withItem(Items.BLAZE_ROD)
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Wands");
        this.pageText("A wand is a focus you keep in hand while you cast. Hold any wand and it discounts the mana cost of every spell you cast; right-click and it casts the spell currently selected in your Ability Ring, with no keybind needed.\\\nThere are three tiers, each stronger than the last. Holding two wands never stacks: only the strongest tier applies.");

        this.page("tiers", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("The Three Tiers");
        this.pageText("The Worn Wand is the cheap starter: a modest mana discount and a small boost to Spell Power. The Carved Wand deepens both the discount and the Spell Power bonus, amplifying your effects further. The Runic Wand is the endgame focus, with the steepest discount and the greatest Spell Power.\\\nEvery one of these values is set by the server config, so an admin can tune the numbers to taste.");

        this.page("crafting", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Crafting the Wands");
        this.pageText("Worn Wand: a stick at the bottom, a redstone dust in the center, a gold ingot on top, with a purple dye and a cyan dye on either side of the center.\\\nCarved Wand: combine a Worn Wand with a Spell Rune.\\\nRunic Wand: surround a Carved Wand with a Nether Star, a Dragon Head, and Spell Runes filling the remaining slots.");
    }

    @Override
    protected String entryName() {
        return "Wands";
    }

    @Override
    protected String entryDescription() {
        return "Hand-held casting foci that discount mana and cast on right-click";
    }

    @Override
    protected Pair<Integer, Integer> entryBackground() {
        return EntryBackground.DEFAULT;
    }

    @Override
    protected BookIconModel entryIcon() {
        return BookIconModel.create(Items.BLAZE_ROD);
    }

    @Override
    protected String entryId() {
        return "wands";
    }
}
