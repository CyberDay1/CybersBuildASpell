package buildaspell.datagen.entries.combos;

import com.klikli_dev.modonomicon.api.datagen.CategoryProviderBase;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookEntryModel;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class FlightEscapeEntry extends EntryProvider {

    public FlightEscapeEntry(CategoryProviderBase parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {
        this.page("creative_flight", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Creative Flight");
        this.pageText("A caster who weaves together every way a body can rise and fall gently may, for a time, forget the ground entirely.\\\n\\\nThis combo grants the caster the ability to fly freely for its duration.\\\n\\\nBest used with Self delivery; Prolonged and Increased Power extend the flight time.");

        this.page("emergency_escape", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Emergency Escape");
        this.pageText("Stack every art of vanishing you know into one desperate working and the spell will choose your exit for you.\\\n\\\nWhen cast, this combo whisks you to a random safe spot far from danger: potentially hundreds of blocks away.\\\n\\\nNot even the caster knows where they will land, which makes it impossible to follow or counter.");
    }

    @Override
    protected BookEntryModel additionalSetup(BookEntryModel entry) {
        entry.hideWhileLocked(true);
        entry.withCondition(
                this.condition().and(
                        this.condition().entryReadBuilder(
                                this.modLoc("spell_effects/movement_force")
                        ),
                        this.condition().entryReadBuilder(
                                this.modLoc("spell_effects/teleportation")
                        )
                )
        );
        return entry;
    }

    @Override
    protected String entryName() {
        return "Flight & Escape";
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
        return BookIconModel.create(Items.ELYTRA);
    }

    @Override
    protected String entryId() {
        return "flight_escape";
    }
}
