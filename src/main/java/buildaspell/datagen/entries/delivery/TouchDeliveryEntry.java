package buildaspell.datagen.entries.delivery;

import com.klikli_dev.modonomicon.api.datagen.CategoryProviderBase;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class TouchDeliveryEntry extends EntryProvider {

    public TouchDeliveryEntry(CategoryProviderBase parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {
        this.page("overview", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Touch Delivery (15 mana)");
        this.pageText("Touch holds the spell ready in your hand instead of casting it right away.\\\n\\\nThe spell is released the next time you strike an entity in melee or right-click an entity or block.\\\n\\\nIf you do not use the charge in time, it fades away.");

        this.page("usage", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Using Touch");
        this.pageText("Touch is the cheapest way to land a spell exactly on a target, with no projectile to dodge.\\\n\\\nIt pairs naturally with damage and status effects: charge the spell, then deliver it with a sword swing.\\\n\\\nOnly one charge is held at a time, so casting Touch again replaces the previous one.");
    }

    @Override
    protected String entryName() {
        return "Touch";
    }

    @Override
    protected String entryDescription() {
        return "The next hit applies the spell";
    }

    @Override
    protected Pair<Integer, Integer> entryBackground() {
        return EntryBackground.DEFAULT;
    }

    @Override
    protected BookIconModel entryIcon() {
        return BookIconModel.create(Items.GOLDEN_SWORD);
    }

    @Override
    protected String entryId() {
        return "touch_delivery";
    }
}
