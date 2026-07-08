package buildaspell.datagen;

import com.klikli_dev.modonomicon.api.datagen.SingleBookSubProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookModel;
import buildaspell.datagen.categories.*;

import java.util.function.BiConsumer;

public class SpellGuidebook extends SingleBookSubProvider {

    public SpellGuidebook(String modId, BiConsumer<String, String> lang) {
        super(modId, "spell_guidebook", lang);
    }

    @Override
    protected String bookName() {
        return "Arcane Codex";
    }

    @Override
    protected String bookTooltip() {
        return "A comprehensive guide to Build-A-Spell magic";
    }

    @Override
    protected BookModel additionalSetup(BookModel book) {
        // Pull the text column in from the page edges so lines don't bleed into the
        // center gutter / frame ornaments. offsetWidth trims the right edge; offsetX adds
        // a matching left margin (the renderer keeps the right edge fixed as offsetX grows).
        return super.additionalSetup(book)
                .withBookTextOffsetWidth(-6)
                .withBookTextOffsetX(3);
    }

    @Override
    protected void registerDefaultMacros() {
    }

    @Override
    protected void generateCategories() {
        add(new GettingStartedCategory(this).generate());
        add(new ManaSystemCategory(this).generate());
        add(new DeliveryMethodsCategory(this).generate());
        add(new SpellEffectsCategory(this).generate());
        add(new SpellModifiersCategory(this).generate());
        add(new AdvancedCategory(this).generate());
        add(new CombosCategory(this).generate());
    }
}
