package buildaspell.datagen.entries.delivery;

import com.klikli_dev.modonomicon.api.datagen.CategoryProviderBase;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class TrackingEntry extends EntryProvider {

    public TrackingEntry(CategoryProviderBase parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {
        this.page("overview", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Tracking Delivery (35 mana)");
        this.pageText("The Tracking delivery method fires a homing projectile that seeks out the nearest valid target. At 35 base mana it is the most expensive delivery method, but its ability to lock onto and follow targets makes it devastating in combat. The projectile adjusts its trajectory mid-flight to home in on its target.");
    }

    @Override
    protected String entryName() {
        return "Tracking";
    }

    @Override
    protected String entryDescription() {
        return "Fire a homing projectile";
    }

    @Override
    protected Pair<Integer, Integer> entryBackground() {
        return EntryBackground.DEFAULT;
    }

    @Override
    protected BookIconModel entryIcon() {
        return BookIconModel.create(Items.COMPASS);
    }

    @Override
    protected String entryId() {
        return "tracking";
    }
}
