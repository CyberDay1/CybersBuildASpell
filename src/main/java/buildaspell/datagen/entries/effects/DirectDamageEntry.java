package buildaspell.datagen.entries.effects;

import com.klikli_dev.modonomicon.api.datagen.CategoryProviderBase;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class DirectDamageEntry extends EntryProvider {

    public DirectDamageEntry(CategoryProviderBase parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {
        this.page("damage", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Damage (5 mana)");
        this.pageText("The most basic offensive spell effect.\\\n\\\nDeals direct damage to the target based on your Spell Power attribute.\\\n\\\nAt only 5 base mana, it is the cheapest effect in the game and serves as the foundation for many combat spells.");

        this.page("lightning", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Lightning (55 mana)");
        this.pageText("Strikes the target with a bolt of lightning, dealing heavy damage and setting them on fire.\\\n\\\nAt 55 base mana it is expensive, but devastating.\\\n\\\nLightning strikes can also transform mobs (e.g., villagers to witches, creepers to charged creepers) just like natural lightning.");

        this.page("explosion", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Explosion (60 mana)");
        this.pageText("Creates an explosion at the target location.\\\n\\\nThe most expensive direct damage effect at 60 base mana.\\\n\\\nBlast power grows with each Increased Power stack (Increased Area adds a little too).\\\n\\\nUse with caution: at two or more Increased Power stacks the blast starts breaking blocks.\\\n\\\nNullify protects creatures from the blast, not the terrain.");
    }

    @Override
    protected String entryName() {
        return "Direct Damage";
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
        return BookIconModel.create(Items.IRON_SWORD);
    }

    @Override
    protected String entryId() {
        return "direct_damage";
    }
}
