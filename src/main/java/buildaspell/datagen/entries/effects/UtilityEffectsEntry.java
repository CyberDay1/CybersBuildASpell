package buildaspell.datagen.entries.effects;

import com.klikli_dev.modonomicon.api.datagen.CategoryProviderBase;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class UtilityEffectsEntry extends EntryProvider {

    public UtilityEffectsEntry(CategoryProviderBase parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {
        this.page("pickup", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Pickup (15 mana)");
        this.pageText("Pickup collects all dropped items in the target area and teleports them into the caster's inventory.\\\n\\\nAt 15 mana, it is cheap and incredibly convenient for collecting loot after combat or mining.\\\n\\\nScales with Increased Area to cover a larger collection radius.");

        this.page("vision_breathing", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Night Vision & Water Breathing");
        this.pageText("Night Vision (15 mana) lets the target see clearly in the dark, while Water Breathing (15 mana) lets them breathe underwater.\\\n\\\nBoth are cheap exploration utilities and last longer with the Prolonged modifier.\\\n\\\nCast on an area to cover your whole party at once.");
    }

    @Override
    protected String entryName() {
        return "Utility";
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
        return BookIconModel.create(Items.HOPPER);
    }

    @Override
    protected String entryId() {
        return "utility";
    }
}
