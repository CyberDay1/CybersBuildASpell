package buildaspell.datagen.entries.combos;

import com.klikli_dev.modonomicon.api.datagen.CategoryProviderBase;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookEntryModel;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class GravityCombosEntry extends EntryProvider {

    public GravityCombosEntry(CategoryProviderBase parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {
        this.page("black_hole", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Black Hole");
        this.pageText("Somewhere in the meeting of inward force and folded space, a gravitational singularity waits to be found.\\\n\\\nWiden the working generously: a narrow spell cannot hold it.\\\n\\\nThe Black Hole pulls all nearby entities toward a single point with immense force.\\\n\\\nOne of the most powerful area control combos.");

        this.page("tornado", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Tornado");
        this.pageText("Draw your foes in and hurl them at the sky in the same breath, over a wide enough stretch of ground, and the wind itself takes over.\\\n\\\nEntities caught in the area are pulled inward and launched skyward repeatedly, creating a deadly vortex of wind and force.");

        this.page("blizzard", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Blizzard");
        this.pageText("Bitter cold, a hungry wind, room to rage, and time to linger: give a storm all four and it will answer.\\\n\\\nA freezing storm settles over the area, chilling every creature caught inside while the wind drags them toward its heart.\\\n\\\nIncreased Area widens the storm and Duration prolongs the freeze.");
    }

    @Override
    protected BookEntryModel additionalSetup(BookEntryModel entry) {
        entry.hideWhileLocked(true);
        entry.withCondition(
                this.condition().entryRead(
                        this.modLoc("spell_effects/movement_force")
                )
        );
        return entry;
    }

    @Override
    protected String entryName() {
        return "Gravity Combos";
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
        return BookIconModel.create(Items.SCULK_SHRIEKER);
    }

    @Override
    protected String entryId() {
        return "gravity_combos";
    }
}
