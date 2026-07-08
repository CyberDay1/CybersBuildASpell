package buildaspell.datagen.entries.mana;

import com.klikli_dev.modonomicon.api.datagen.CategoryProviderBase;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class AttributesEntry extends EntryProvider {

    public AttributesEntry(CategoryProviderBase parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {
        this.page("pool_regen", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Pool & Regeneration");
        this.pageText("Mana Pool determines your maximum mana capacity (default: 100).\\\n\\\nMana Regen controls how quickly mana restores (default: 5/sec).\\\n\\\nBoth can be increased through enchantments at the Arcane Altar and are exposed as entity attributes for cross-mod compatibility.");

        this.page("power_speed", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Spell Power");
        this.pageText("Spell Power scales the magnitude of your spell effects: more Power means more damage, healing, and force (default: 10).\\\n\\\nSpell Power can be raised through enchantments at the Arcane Altar, and is exposed as an entity attribute so equipment and other mods can modify it through the standard attribute system.");
    }

    @Override
    protected String entryName() {
        return "Magical Attributes";
    }

    @Override
    protected String entryDescription() {
        return "Custom attributes for magic";
    }

    @Override
    protected Pair<Integer, Integer> entryBackground() {
        return EntryBackground.DEFAULT;
    }

    @Override
    protected BookIconModel entryIcon() {
        return BookIconModel.create(Items.GOLDEN_APPLE);
    }

    @Override
    protected String entryId() {
        return "attributes";
    }
}
