package buildaspell.datagen.categories;

import com.klikli_dev.modonomicon.api.datagen.IndexModeCategoryProvider;
import com.klikli_dev.modonomicon.api.datagen.ModonomiconProviderBase;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import buildaspell.datagen.entries.advanced.AbilityRingEntry;
import buildaspell.datagen.entries.advanced.CommandsConfigEntry;
import buildaspell.datagen.entries.advanced.ImportExportEntry;
import buildaspell.datagen.entries.advanced.PortalSystemEntry;
import buildaspell.datagen.entries.advanced.SpellBuilderEntry;
import net.minecraft.world.item.Items;

public class AdvancedCategory extends IndexModeCategoryProvider {

    public AdvancedCategory(ModonomiconProviderBase parent) {
        super(parent);
    }

    @Override
    protected void generateEntries() {
        add(new SpellBuilderEntry(this).generate());
        add(new AbilityRingEntry(this).generate());
        add(new ImportExportEntry(this).generate());
        add(new PortalSystemEntry(this).generate());
        add(new CommandsConfigEntry(this).generate());
    }

    @Override
    protected String categoryName() {
        return "Advanced Systems";
    }

    @Override
    protected BookIconModel categoryIcon() {
        return BookIconModel.create(Items.ENDER_PEARL);
    }

    @Override
    public String categoryId() {
        return "advanced";
    }
}
