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
        this.pageText("Combine Launch + Explosion + Ignite with Increased Power x2 (min 5 components) to call down a meteor strike.\\\nA volley of huge fireballs rains from the sky onto the target area, each detonating in an explosion and setting the ground ablaze. Increased Power widens the bombardment and adds more meteors, though its effect caps at two stacks.");

        this.page("firestorm", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Firestorm");
        this.pageText("Combine Ignite + Explosion with Increased Area (min 3 components) to summon a firestorm.\\\nA rain of small fireballs falls across the area, leaving lingering flames burning on the ground where they land. Lighter and wider than a Meteor Strike, it blankets a broad zone in fire. Increased Area widens the storm.");
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
