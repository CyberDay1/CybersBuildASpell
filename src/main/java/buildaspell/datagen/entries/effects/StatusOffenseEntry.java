package buildaspell.datagen.entries.effects;

import com.klikli_dev.modonomicon.api.datagen.CategoryProviderBase;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class StatusOffenseEntry extends EntryProvider {

    public StatusOffenseEntry(CategoryProviderBase parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {
        this.page("poison_wither", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Poison & Wither");
        this.pageText("Poison (30 mana) applies the Poison effect, dealing damage over time but never killing the target.\\\n\\\nWither (40 mana) applies the Wither effect, which deals damage over time and CAN kill.\\\n\\\nBoth durations scale with the Prolonged modifier.");

        this.page("blind_ignite", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Blind & Ignite");
        this.pageText("Blind (25 mana) applies the Blindness effect, severely limiting the target's vision range.\\\n\\\nIgnite (15 mana) sets the target on fire, dealing continuous fire damage.\\\n\\\nIgnite is one of the cheapest offensive effects and pairs well with the Prolonged modifier.");

        this.page("freeze", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Freeze");
        this.pageText("Freeze (20 mana) applies a freezing effect to the target, slowing their movement and dealing periodic frost damage similar to being inside powdered snow.\\\n\\\nIt is particularly effective against Blaze and Strider mobs which take extra freeze damage.");

        this.page("slow_weaken", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Slow & Weaken");
        this.pageText("Slow (20 mana) applies Slowness, dragging the target's movement to a crawl.\\\n\\\nWeaken (25 mana) applies Weakness, sapping the damage of their melee attacks.\\\n\\\nBoth deepen with Increased Power and last longer with Prolonged.\\\n\\\nNullify does not stop them: status effects are not damage.");

        this.page("root", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Root");
        this.pageText("Root (30 mana) locks the target in place, pinning their feet so they can neither walk nor jump for a short time.\\\n\\\nIt is the ultimate setup tool: root a fleeing enemy, then follow up with a slower, heavier spell.\\\n\\\nDuration scales with Prolonged.");
    }

    @Override
    protected String entryName() {
        return "Status Effects (Offense)";
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
        return BookIconModel.create(Items.SPIDER_EYE);
    }

    @Override
    protected String entryId() {
        return "status_offense";
    }
}
