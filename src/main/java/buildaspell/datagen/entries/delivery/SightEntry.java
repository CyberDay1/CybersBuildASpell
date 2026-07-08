package buildaspell.datagen.entries.delivery;

import com.klikli_dev.modonomicon.api.datagen.CategoryProviderBase;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class SightEntry extends EntryProvider {

    public SightEntry(CategoryProviderBase parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {
        this.page("overview", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Sight Delivery (15 mana)");
        this.pageText("The Sight delivery method performs a raycast from the caster's eyes to the block or entity under the crosshair, up to 20 blocks away.\\\n\\\nIt costs 15 base mana and is perfect for precise, targeted spells.\\\n\\\nIf nothing solid is under the crosshair, the spell lands at the far end of the ray instead.");
    }

    @Override
    protected String entryName() {
        return "Sight";
    }

    @Override
    protected String entryDescription() {
        return "Target what you are looking at";
    }

    @Override
    protected Pair<Integer, Integer> entryBackground() {
        return EntryBackground.DEFAULT;
    }

    @Override
    protected BookIconModel entryIcon() {
        return BookIconModel.create(Items.ENDER_EYE);
    }

    @Override
    protected String entryId() {
        return "sight";
    }
}
