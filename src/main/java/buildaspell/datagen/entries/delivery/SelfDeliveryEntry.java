package buildaspell.datagen.entries.delivery;

import com.klikli_dev.modonomicon.api.datagen.CategoryProviderBase;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class SelfDeliveryEntry extends EntryProvider {

    public SelfDeliveryEntry(CategoryProviderBase parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {
        this.page("overview", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Self Delivery (5 mana)");
        this.pageText("The Self delivery method targets the caster directly.\\\n\\\nIt is the cheapest delivery method at 5 base mana cost, making it ideal for buff spells like Shield, Speed, Heal, and Invisibility.\\\n\\\nThe spell takes effect instantly on the caster with no projectile or targeting required.");
    }

    @Override
    protected String entryName() {
        return "Self";
    }

    @Override
    protected String entryDescription() {
        return "Target yourself with spells";
    }

    @Override
    protected Pair<Integer, Integer> entryBackground() {
        return EntryBackground.DEFAULT;
    }

    @Override
    protected BookIconModel entryIcon() {
        return BookIconModel.create(Items.LEATHER_CHESTPLATE);
    }

    @Override
    protected String entryId() {
        return "self_delivery";
    }
}
