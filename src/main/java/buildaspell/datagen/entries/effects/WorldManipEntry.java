package buildaspell.datagen.entries.effects;

import com.klikli_dev.modonomicon.api.datagen.CategoryProviderBase;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class WorldManipEntry extends EntryProvider {

    public WorldManipEntry(CategoryProviderBase parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {
        this.page("break_conjure", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Break & Conjure");
        this.pageText("Break (40 mana) destroys the targeted block, dropping it as an item. It also clears spell light, which nothing else can mine.\\\n\\\nConjure (25 mana) places a block at the target location from a configurable list of allowed blocks (default: stone, cobblestone, blackstone, dirt, grass). It builds into air and into anything a block can normally replace: light, grass, snow and water.\\\n\\\nBoth scale with Increased Area for multi-block operations.");

        this.page("light_water", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Light & Water");
        this.pageText("Light (12 mana) fills the area around the target with invisible light sources: a radius of 5 blocks by default, wider with Increased Area. Ideal for lighting caves on the fly.\\\n\\\nThe lights fade on their own after a minute, so a cast brightens a place without permanently rewriting it. The lifetime is server-configurable, and Break clears them early.\\\n\\\nCreate Water (20 mana) places a water source block.\\\n\\\nEvaporate Water (25 mana) removes water source blocks.");

        this.page("growth_reap", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Growth & Reap");
        this.pageText("Growth (30 mana) applies bone meal to crops and plants in the target area, accelerating their growth.\\\n\\\nReap (40 mana) harvests mature crops and automatically replants seeds.\\\n\\\nTogether they form a powerful farming automation system, especially when combined with the Increased Area modifier.");
    }

    @Override
    protected String entryName() {
        return "World Manipulation";
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
        return BookIconModel.create(Items.DIAMOND_PICKAXE);
    }

    @Override
    protected String entryId() {
        return "world_manip";
    }
}
