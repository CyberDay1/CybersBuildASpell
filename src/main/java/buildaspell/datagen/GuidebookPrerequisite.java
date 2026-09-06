package buildaspell.datagen;

import com.klikli_dev.modonomicon.api.datagen.book.BookEntryModel;
import net.minecraft.resources.Identifier;

/**
 * Modonomicon 2.x dropped {@code ConditionHelper.entryRead(Identifier)}. The replacement,
 * {@code entryViewedOnce}, wants the prerequisite's {@link BookEntryModel}, but the combos entries
 * gate on entries built by other categories, which are not reachable from here.
 *
 * <p>{@code entryViewedOnce} reads only the model's id and name, so a stub carrying those two is
 * exact.
 */
public final class GuidebookPrerequisite {

    private GuidebookPrerequisite() {
    }

    /**
     * @param entryId the prerequisite entry, as {@code provider.modLoc("<category>/<entry>")}
     */
    public static BookEntryModel of(Identifier entryId) {
        // The name is baked into the generated condition as a tooltip translation key, so it has to
        // match the key the prerequisite's own entry registers: book.<modId>.<bookId>.<path>.name,
        // where the entry id's namespace is already the modId.
        String name = "book." + entryId.getNamespace() + "." + SpellGuidebook.BOOK_ID + "."
                + entryId.getPath().replace('/', '.') + ".name";
        return BookEntryModel.create(entryId, name);
    }
}
