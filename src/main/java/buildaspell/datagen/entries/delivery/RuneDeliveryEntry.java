package buildaspell.datagen.entries.delivery;

import com.klikli_dev.modonomicon.api.datagen.CategoryProviderBase;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class RuneDeliveryEntry extends EntryProvider {

    public RuneDeliveryEntry(CategoryProviderBase parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {
        this.page("overview", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Rune Delivery (20 mana)");
        this.pageText("The Rune delivery method places a glowing spell rune on the ground at the targeted location. The rune persists for a short duration and triggers its spell effects when an entity steps on or near it.");

        this.page("tactics", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Tactical Usage");
        this.pageText("Runes are excellent for setting traps and area denial. Place damage or status effect runes at chokepoints, or use healing runes to create safe zones.\\\nThe rune activates on contact and then dissipates.");
    }

    @Override
    protected String entryName() {
        return "Rune";
    }

    @Override
    protected String entryDescription() {
        return "Place spell runes on the ground";
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
        return "rune_delivery";
    }
}
