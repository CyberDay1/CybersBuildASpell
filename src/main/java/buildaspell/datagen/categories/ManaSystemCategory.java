package buildaspell.datagen.categories;

import com.klikli_dev.modonomicon.api.datagen.IndexModeCategoryProvider;
import com.klikli_dev.modonomicon.api.datagen.ModonomiconProviderBase;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import buildaspell.datagen.entries.mana.AttributesEntry;
import buildaspell.datagen.entries.mana.EnchantmentsEntry;
import buildaspell.datagen.entries.mana.ManaBasicsEntry;
import net.minecraft.world.item.Items;

public class ManaSystemCategory extends IndexModeCategoryProvider {

    public ManaSystemCategory(ModonomiconProviderBase parent) {
        super(parent);
    }

    @Override
    protected void generateEntries() {
        add(new ManaBasicsEntry(this).generate());
        add(new AttributesEntry(this).generate());
        add(new EnchantmentsEntry(this).generate());
    }

    @Override
    protected String categoryName() {
        return "Mana System";
    }

    @Override
    protected BookIconModel categoryIcon() {
        return BookIconModel.create(Items.EXPERIENCE_BOTTLE);
    }

    @Override
    public String categoryId() {
        return "mana_system";
    }
}
