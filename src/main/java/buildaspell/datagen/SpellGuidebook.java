package buildaspell.datagen;

import com.klikli_dev.modonomicon.api.datagen.SingleBookSubProvider;
import buildaspell.datagen.categories.*;

public class SpellGuidebook extends SingleBookSubProvider {

    public static final String BOOK_ID = "spell_guidebook";

    // Modonomicon 2.x no longer takes the lang consumer here — BookProvider injects it from the
    // shared LanguageProviderCache.
    //
    // The arguments are (bookId, modId), and they used to be passed the other way round, giving the
    // book the id spell_guidebook:buildaspell. That has to be corrected: BookProvider looks the
    // book's research up under <mod container id>:<bookId>, so the research the combos gating
    // generates is only found when the book's namespace is the real mod id. Nothing is lost by
    // fixing it here — this branch has never shipped a book to migrate.
    public SpellGuidebook(String modId) {
        super(BOOK_ID, modId);
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
