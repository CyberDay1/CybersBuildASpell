package buildaspell.datagen.categories;

import com.klikli_dev.modonomicon.api.datagen.IndexModeCategoryProvider;
import com.klikli_dev.modonomicon.api.datagen.ModonomiconProviderBase;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import buildaspell.datagen.entries.getting_started.ArcaneAltarEntry;
import buildaspell.datagen.entries.getting_started.BlankRuneEntry;
import buildaspell.datagen.entries.getting_started.SpellRuneEntry;
import buildaspell.datagen.entries.getting_started.WandsEntry;
import buildaspell.datagen.entries.getting_started.WelcomeEntry;
import net.minecraft.world.item.Items;

public class GettingStartedCategory extends IndexModeCategoryProvider {

    public GettingStartedCategory(ModonomiconProviderBase parent) {
        super(parent);
    }

    @Override
    protected void generateEntries() {
        add(new WelcomeEntry(this).generate());
        add(new BlankRuneEntry(this).generate());
        add(new SpellRuneEntry(this).generate());
        add(new ArcaneAltarEntry(this).generate());
        add(new WandsEntry(this).generate());
    }

    @Override
    protected String categoryName() {
        return "Getting Started";
    }

    @Override
    protected BookIconModel categoryIcon() {
        return BookIconModel.create(Items.BOOK);
    }

    @Override
    public String categoryId() {
        return "getting_started";
    }
}
