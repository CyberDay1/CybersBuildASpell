package buildaspell.datagen.entries.getting_started;

import com.klikli_dev.modonomicon.api.datagen.CategoryProviderBase;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class WelcomeEntry extends EntryProvider {

    public WelcomeEntry(CategoryProviderBase parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {
        this.page("intro", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Welcome to Build-A-Spell");
        this.pageText("The Arcane Codex is your guide to mastering the art of spell creation.\\\n\\\nWithin these pages you will find everything you need to craft powerful spells, from basic rune creation to advanced combo techniques.");

        this.page("overview", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("How to Use This Book");
        this.pageText("Navigate the node graph by clicking on category icons at the top.\\\n\\\nEach node represents a topic: click it to read its pages.\\\n\\\nSome entries are hidden until you have read their prerequisites.\\\n\\\nExplore freely and discover the secrets of magic.");
    }

    @Override
    protected String entryName() {
        return "Welcome";
    }

    @Override
    protected String entryDescription() {
        return "An introduction to the Arcane Codex";
    }

    @Override
    protected Pair<Integer, Integer> entryBackground() {
        return EntryBackground.DEFAULT;
    }

    @Override
    protected BookIconModel entryIcon() {
        return BookIconModel.create(Items.ENCHANTED_BOOK);
    }

    @Override
    protected String entryId() {
        return "welcome";
    }
}
