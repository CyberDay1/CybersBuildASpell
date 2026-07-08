package buildaspell.datagen.entries.advanced;

import com.klikli_dev.modonomicon.api.datagen.CategoryProviderBase;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class SpellBuilderEntry extends EntryProvider {

    public SpellBuilderEntry(CategoryProviderBase parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {
        this.page("overview", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Spell Builder (G Key)");
        this.pageText("The Spell Builder is the core interface for creating spells. Press G to open it while holding a Blank Rune.\\\nThe interface presents a drag-and-drop workspace where you assemble your spell from a delivery method, effects, and modifiers.");

        this.page("components", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Building a Spell");
        this.pageText("Every spell starts with a Delivery Method: this determines how the spell reaches its target.\\\nThen add Effects to define what the spell does (damage, heal, teleport, etc.), and Modifiers to customize behavior (increase power, add pierce, etc.). Components are ordered left to right.");

        this.page("cost", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Mana Cost Display");
        this.pageText("The Spell Builder shows the total mana cost of your current spell configuration in real time. As you add or remove components, the cost updates automatically.\\\nThe cost accounts for all base costs, modifiers, and any server-configured cost multipliers. Spells can have up to 30 components.");
    }

    @Override
    protected String entryName() {
        return "Spell Builder";
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
        return BookIconModel.create(Items.WRITABLE_BOOK);
    }

    @Override
    protected String entryId() {
        return "spell_builder";
    }
}
