package buildaspell.datagen.entries.combos;

import com.klikli_dev.modonomicon.api.datagen.CategoryProviderBase;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookEntryModel;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class SanctuaryCombosEntry extends EntryProvider {

    public SanctuaryCombosEntry(CategoryProviderBase parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {
        this.page("sanctuary", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Sanctuary");
        this.pageText("Mending magic, poured out over ground instead of a body and left to keep working, consecrates the earth itself.\\\n\\\nThe ground is hallowed, wrapping every player who stands within it (yourself included) in a protective ward of regeneration, resistance, and absorption.\\\n\\\nHostile mobs are never sheltered.\\\n\\\nIncreased Power strengthens the ward and Duration makes it last longer.");
    }

    @Override
    protected BookEntryModel additionalSetup(BookEntryModel entry) {
        entry.hideWhileLocked(true);
        entry.withCondition(
                this.condition().entryRead(
                        this.modLoc("spell_effects/healing_buffs")
                )
        );
        return entry;
    }

    @Override
    protected String entryName() {
        return "Sanctuary Combos";
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
        return BookIconModel.create(Items.TOTEM_OF_UNDYING);
    }

    @Override
    protected String entryId() {
        return "sanctuary_combos";
    }
}
