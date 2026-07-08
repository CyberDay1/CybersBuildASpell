package buildaspell.datagen.entries.combos;

import com.klikli_dev.modonomicon.api.datagen.CategoryProviderBase;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookEntryModel;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class CataclysmCombosEntry extends EntryProvider {

    public CataclysmCombosEntry(CategoryProviderBase parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {
        this.page("meteor_strike", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Meteor Strike");
        this.pageText("Hurl fire and ruin at the heavens with enough raw power behind it, and the heavens hurl it back.\\\n\\\nA volley of huge fireballs rains from the sky onto the target area, each detonating in an explosion and setting the ground ablaze.\\\n\\\nIncreased Area widens the bombardment and adds more meteors; Increased Power makes each blast bigger, capping at two stacks.");

        this.page("firestorm", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Firestorm");
        this.pageText("Flame and blast, spread wide rather than piled high, settle over the land as a burning rain.\\\n\\\nA rain of small fireballs falls across the area, leaving lingering flames burning on the ground where they land.\\\n\\\nLighter and wider than a Meteor Strike, it blankets a broad zone in fire.\\\n\\\nIncreased Area widens the storm.");
    }

    @Override
    protected BookEntryModel additionalSetup(BookEntryModel entry) {
        entry.hideWhileLocked(true);
        entry.withCondition(
                this.condition().entryRead(
                        this.modLoc("spell_effects/direct_damage")
                )
        );
        return entry;
    }

    @Override
    protected String entryName() {
        return "Cataclysm Combos";
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
        return BookIconModel.create(Items.FIRE_CHARGE);
    }

    @Override
    protected String entryId() {
        return "cataclysm_combos";
    }
}
