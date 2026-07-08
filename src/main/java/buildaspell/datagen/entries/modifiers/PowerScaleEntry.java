package buildaspell.datagen.entries.modifiers;

import com.klikli_dev.modonomicon.api.datagen.CategoryProviderBase;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class PowerScaleEntry extends EntryProvider {

    public PowerScaleEntry(CategoryProviderBase parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {
        this.page("increased_power", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Increased Power (20 mana)");
        this.pageText("Increased Power amplifies the strength of all effects in the spell.\\\n\\\nEach stack multiplies the spell's potency further.\\\n\\\nThis is a stackable modifier — add multiple copies for greater amplification.\\\n\\\nIt is the go-to modifier for maximizing damage or healing output.");

        this.page("increased_area", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Increased Area (15 mana)");
        this.pageText("Increased Area expands the area of effect for spells that affect a region.\\\n\\\nEach stack increases the radius further.\\\n\\\nThis is stackable and particularly powerful with effects like Explosion, Break, Conjure, Growth, and Reap.\\\n\\\nIt is also a key component in many secret combos.");

        this.page("fortunate_son", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Fortunate Son (25 mana)");
        this.pageText("Fortunate Son enriches what your spells yield.\\\n\\\nOn Break, it applies Fortune to the broken block, yielding more drops from ores and other fortune-affected blocks.\\\n\\\nOn Reap, it adds bonus crop drops the same way.\\\n\\\nOn damage spells, it acts as Looting: enemies slain by the spell drop extra loot.\\\n\\\nStackable: each stack raises the fortune and looting level.\\\n\\\nBest combined with Break and Increased Area for efficient mining.");
    }

    @Override
    protected String entryName() {
        return "Power & Scale";
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
        return BookIconModel.create(Items.GLOWSTONE_DUST);
    }

    @Override
    protected String entryId() {
        return "power_scale";
    }
}
