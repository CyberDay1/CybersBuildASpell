package buildaspell.datagen.entries.advanced;

import com.klikli_dev.modonomicon.api.datagen.CategoryProviderBase;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class AbilityRingEntry extends EntryProvider {

    public AbilityRingEntry(CategoryProviderBase parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {
        this.page("overview", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Ability Ring (V Key)");
        this.pageText("The Ability Ring is a radial menu for quick spell selection.\\\n\\\nPress V to open it and hover over a slot to select that spell.\\\n\\\nThe ring supports 10 spell slots, each holding one Spell Rune.\\\n\\\nRelease V to select the highlighted spell for casting.");

        this.page("keybinds", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Slot Keybinds");
        this.pageText("The first five slots can also be cast directly with their own keybinds (Cast Spell 1 through Cast Spell 5), bypassing the ring entirely for faster access.\\\n\\\nThe remaining slots are reached through the ring itself.\\\n\\\nConfigure these keybinds in the Controls menu under the 'Build a Spell' category.");
    }

    @Override
    protected String entryName() {
        return "Ability Ring";
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
        return BookIconModel.create(Items.RECOVERY_COMPASS);
    }

    @Override
    protected String entryId() {
        return "ability_ring";
    }
}
