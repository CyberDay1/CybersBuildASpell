package buildaspell.datagen.entries.modifiers;

import com.klikli_dev.modonomicon.api.datagen.CategoryProviderBase;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class TimingEntry extends EntryProvider {

    public TimingEntry(CategoryProviderBase parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {
        this.page("prolonged_delay", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Prolonged & Delay");
        this.pageText("Prolonged (18 mana) increases the active duration of status effects applied by the spell. Stackable for longer lasting effects.\\\nDelay (8 mana) adds a time delay before the spell activates after casting. Stackable: each copy adds more delay. Useful for timed traps.");

        this.page("duration", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Duration (35 mana)");
        this.pageText("Duration is a more powerful version of Prolonged, significantly extending effect durations. It is stackable and a key ingredient in the Void Rift combo.\\\nDuration is expensive but essential for maintaining long-lasting buffs or persistent area effects.");

        this.page("linger", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Linger (25 mana)");
        this.pageText("Linger causes the spell's effects to persist at the target location as a lingering area of effect, similar to a lingering potion. Any entity entering the area receives the spell's effects.\\\nThe duration and area scale with the Duration and Increased Area modifiers respectively.");

        this.page("echo", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Echo (40 mana)");
        this.pageText("Echo causes the spell to re-trigger after a short delay, effectively casting it twice. Stackable: each additional Echo adds another re-trigger.\\\nAt 40 mana per stack, it is expensive but effectively multiplies the spell's output. Excellent for sustained damage or repeated healing.");
    }

    @Override
    protected String entryName() {
        return "Timing";
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
        return BookIconModel.create(Items.CLOCK);
    }

    @Override
    protected String entryId() {
        return "timing";
    }
}
