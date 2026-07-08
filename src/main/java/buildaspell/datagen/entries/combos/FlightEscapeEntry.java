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
        this.pageText("Combine Launch + Levitation + Slow Fall (min 3 components) to achieve true creative-style flight. This combo grants the caster the ability to fly freely for its duration.\\\nBest used with Self delivery and Duration modifier for extended flight time.");

        this.page("emergency_escape", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Emergency Escape");
        this.pageText("Combine Blink + Recall + Teleport (min 3 components) to create an emergency escape spell.\\\nWhen cast, this combo instantly teleports you to your Mark location, performing a rapid three-stage teleportation sequence that is extremely difficult to follow or counter.");
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
