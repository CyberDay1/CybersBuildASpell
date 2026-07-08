package buildaspell.datagen.entries.advanced;

import com.klikli_dev.modonomicon.api.datagen.CategoryProviderBase;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class ImportExportEntry extends EntryProvider {

    public ImportExportEntry(CategoryProviderBase parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {
        this.page("export", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Exporting Spells");
        this.pageText("Use /buildaspell spell export <slot> (alias /cbas spell export <slot>) to print the spell in that slot as a Base64-encoded string. This string can be shared with other players, posted online, or saved as a backup.\\\nThe export captures the complete spell configuration including all components and their order.");

        this.page("import", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Importing Spells");
        this.pageText("Press I to open the Import Spell screen. Paste a Base64 spell string into the text field and confirm to load it onto a Blank Rune in your inventory.\\\nThis allows you to use spells created by other players or restore backed-up configurations.");
    }

    @Override
    protected String entryName() {
        return "Import & Export";
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
        return BookIconModel.create(Items.PAPER);
    }

    @Override
    protected String entryId() {
        return "import_export";
    }
}
