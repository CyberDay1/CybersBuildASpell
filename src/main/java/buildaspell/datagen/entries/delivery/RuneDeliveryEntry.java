package buildaspell.datagen.entries.delivery;

import com.klikli_dev.modonomicon.api.datagen.CategoryProviderBase;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import buildaspell.registry.ModItems;

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
        this.pageText("The Rune delivery method inscribes a glowing spell rune on the ground at your feet.\\\n\\\nThe rune charges for about a second, then releases its spell effects on its own.\\\n\\\nEach Duration modifier extends the charge time, letting you tune when it goes off.");

        this.page("tactics", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Tactical Usage");
        this.pageText("Runes are timed charges: drop one and move, and it detonates where you stood.\\\n\\\nUse damage or status runes to cover a retreat, or healing runes to bless ground you are about to fight on.\\\n\\\nFor a rune that waits for an enemy instead of a timer, see the Trap delivery.");
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
        return BookIconModel.create(ModItems.SPELL_RUNE.get());
    }

    @Override
    protected String entryId() {
        return "rune_delivery";
    }
}
