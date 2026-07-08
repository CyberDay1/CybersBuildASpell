package buildaspell.datagen.entries.delivery;

import com.klikli_dev.modonomicon.api.datagen.CategoryProviderBase;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class TrapDeliveryEntry extends EntryProvider {

    public TrapDeliveryEntry(CategoryProviderBase parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {
        this.page("overview", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Trap Delivery (25 mana)");
        this.pageText("Trap places an armed rune on the surface you are looking at. After a short arming delay, it waits and then triggers the moment any living creature other than you steps within range.\\\nWhen sprung, it casts the spell at the rune and vanishes.");

        this.page("usage", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Setting Traps");
        this.pageText("Unlike a Rune, which fires on its own after charging, a Trap waits for a victim and persists until something approaches or its lifetime runs out.\\\nUse it to guard doorways and chokepoints, or to set ambushes with offensive effects. The trigger radius and lifetime are configurable.");
    }

    @Override
    protected String entryName() {
        return "Trap";
    }

    @Override
    protected String entryDescription() {
        return "An armed rune that triggers on approach";
    }

    @Override
    protected Pair<Integer, Integer> entryBackground() {
        return EntryBackground.DEFAULT;
    }

    @Override
    protected BookIconModel entryIcon() {
        return BookIconModel.create(Items.TRIPWIRE_HOOK);
    }

    @Override
    protected String entryId() {
        return "trap_delivery";
    }
}
