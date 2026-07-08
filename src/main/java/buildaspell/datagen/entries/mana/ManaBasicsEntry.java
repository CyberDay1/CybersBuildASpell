package buildaspell.datagen.entries.mana;

import com.klikli_dev.modonomicon.api.datagen.CategoryProviderBase;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class ManaBasicsEntry extends EntryProvider {

    public ManaBasicsEntry(CategoryProviderBase parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {
        this.page("overview", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Mana Overview");
        this.pageText("Every player has a mana pool that fuels spell casting. The base mana pool holds 100 mana points and regenerates at a rate of 5 mana per second.\\\nThe mana bar HUD can be toggled with the M key.");

        this.page("cost", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Mana Cost Formula");
        this.pageText("Each spell's mana cost is calculated by adding the base costs of the delivery method, all effects, and all modifiers.\\\nThe total is then multiplied by the server's global cost multiplier and any per-component cost multipliers set in the config.");

        this.page("recovery", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Mana Recovery");
        this.pageText("Mana regenerates passively over time based on your Mana Regen attribute. The default rate of 5 per second can be increased through the Mana Regeneration enchantment at the Arcane Altar.\\\nRegeneration is continuous: your pool refills steadily whether you are casting or not.");
    }

    @Override
    protected String entryName() {
        return "Mana Basics";
    }

    @Override
    protected String entryDescription() {
        return "Understanding the mana system";
    }

    @Override
    protected Pair<Integer, Integer> entryBackground() {
        return EntryBackground.DEFAULT;
    }

    @Override
    protected BookIconModel entryIcon() {
        return BookIconModel.create(Items.EXPERIENCE_BOTTLE);
    }

    @Override
    protected String entryId() {
        return "mana_basics";
    }
}
