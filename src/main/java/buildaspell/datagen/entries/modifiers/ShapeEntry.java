package buildaspell.datagen.entries.modifiers;

import com.klikli_dev.modonomicon.api.datagen.CategoryProviderBase;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class ShapeEntry extends EntryProvider {

    public ShapeEntry(CategoryProviderBase parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {
        this.page("wall_floor", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Wall & Floor");
        this.pageText("Wall (30 mana) reshapes the spell's area of effect into a vertical wall pattern.\\\n\\\nFloor (30 mana) reshapes it into a horizontal floor pattern.\\\n\\\nNeither is stackable.\\\n\\\nThese modifiers are essential for Conjure-based building spells, allowing you to create walls and floors of blocks instantly.");

        this.page("fill", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Fill (40 mana)");
        this.pageText("Fill reshapes the spell's area into a solid sphere around the point the spell lands: a radius of 30 blocks by default.\\\n\\\nNot stackable, and Increased Area does not widen it. The Fill radius replaces the spell's normal area outright.\\\n\\\nFill ignores walls. It measures distance alone, with no line of sight, so the sphere reaches straight through stone into sealed rooms and caves you cannot see. Break will hollow out far more than the room you are standing in, and Conjure will pack every open space in range, including the ones behind a wall.\\\n\\\nCombined with Conjure it raises solid structures instantly, and combined with Break it clears large volumes. It also shapes Create Water and Evaporate Water.\\\n\\\nThe most expensive shape modifier at 40 mana.");
    }

    @Override
    protected String entryName() {
        return "Shape Modifiers";
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
        return BookIconModel.create(Items.BRICKS);
    }

    @Override
    protected String entryId() {
        return "shape";
    }
}
