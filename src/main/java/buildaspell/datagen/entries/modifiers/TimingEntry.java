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
        this.pageText("Prolonged (18 mana) increases the active duration of status effects applied to targets: poison, freeze, ignite, root, and the like.\\\n\\\nStackable for longer lasting effects. It does NOT extend summoned zones or constructs; for that, see Duration.\\\n\\\nDelay (8 mana) adds a time delay before the spell activates after casting.\\\n\\\nStackable: each copy adds more delay.\\\n\\\nUseful for timed traps.");

        this.page("duration", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Duration (35 mana)");
        this.pageText("Duration turns the spell into a persistent area: instead of firing once, the spell lingers where it lands and re-applies its effects in steady pulses.\\\n\\\nEach stack extends how long the area lasts. It also prolongs summoned constructs and storms: blizzards, lightning storms, sanctuaries, black holes, and rune traps. This is the opposite of Prolonged, which lengthens status effects on targets rather than the zone itself.\\\n\\\nDuration is expensive, but it is the key to zone control: pulsing damage fields, healing circles, or lasting terrain effects.\\\n\\\nIf the spell forms a combo, the combo takes over instead.");

        this.page("linger", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Linger (25 mana)");
        this.pageText("Linger makes the spell settle where it lands as a persistent area, re-applying its effects to anything inside on a steady pulse: a lingering-potion-style cloud.\\\n\\\nUnlike Duration, Linger is a cheaper one-time enabler at a fixed base lifetime. Stack Duration on top to extend how long the area lasts, and Increased Area to widen it.\\\n\\\nIf the spell forms a combo, the combo takes over instead.");

        this.page("echo", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Echo (40 mana)");
        this.pageText("Echo causes the spell to re-trigger after a short delay.\\\n\\\nStackable: each additional Echo adds another re-trigger.\\\n\\\nEach echo strikes at reduced power compared to the one before it, so echoes multiply the spell's output with diminishing returns.\\\n\\\nExcellent for sustained damage or repeated healing.");
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
