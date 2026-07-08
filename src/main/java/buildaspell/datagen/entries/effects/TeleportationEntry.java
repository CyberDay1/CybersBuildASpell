package buildaspell.datagen.entries.effects;

import com.klikli_dev.modonomicon.api.datagen.CategoryProviderBase;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class TeleportationEntry extends EntryProvider {

    public TeleportationEntry(CategoryProviderBase parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {
        this.page("teleport_blink", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Teleport & Blink");
        this.pageText("Teleport (50 mana) moves you, the caster, to wherever the spell lands.\\\n\\\nWith Self delivery, you teleport forward toward where you're looking.\\\n\\\nBlink (35 mana) is a short-range instant teleport in the direction you're facing, cheaper but with limited range.");

        this.page("swap_mark_recall", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Swap, Mark & Recall");
        this.pageText("Swap (45 mana) exchanges positions between the caster and the target.\\\n\\\nMark (30 mana) places an invisible waypoint at the target location.\\\n\\\nRecall (40 mana) teleports the caster back to the most recently placed Mark.\\\n\\\nEach caster keeps one Mark at a time, and Recall returns you to its coordinates in your current dimension.");

        this.page("combos_hint", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Teleportation Combos");
        this.pageText("Teleportation effects are key ingredients in several powerful combos.\\\n\\\nCombining teleportation with movement effects can unlock flight capabilities, while mixing with other schools produces unique synergies.\\\n\\\nExperiment with different combinations to discover hidden techniques.");
    }

    @Override
    protected String entryName() {
        return "Teleportation";
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
        return BookIconModel.create(Items.ENDER_PEARL);
    }

    @Override
    protected String entryId() {
        return "teleportation";
    }
}
