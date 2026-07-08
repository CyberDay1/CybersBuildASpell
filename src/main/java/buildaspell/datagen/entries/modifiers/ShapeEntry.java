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
        this.pageText("Fill fills the space the spell lands in, and stops where that space stops. It walks from block to touching block instead of measuring distance, so it cannot reach through a wall into a room it never touched.\\\n\\\nConjure and Create Water pour. They settle to the floor and rise a layer at a time, so a hole fills to ground level and a sealed room fills to its ceiling. Cast somewhere genuinely open there is nothing holding the fill, and nothing is placed.\\\n\\\nBreak and Evaporate Water follow the mass instead. Break clears the run of blocks it hit and stops at the open air, and Evaporate drains the body of water it was cast into and stops at its shore.\\\n\\\nNot stackable, and Increased Area does not widen it. Wall and Floor still take priority over Fill. The search reaches 30 blocks by default, which is a limit rather than a shape.\\\n\\\nThe most expensive shape modifier at 40 mana.");
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
