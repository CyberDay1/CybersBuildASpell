package buildaspell.datagen.categories;

import com.klikli_dev.modonomicon.api.datagen.IndexModeCategoryProvider;
import com.klikli_dev.modonomicon.api.datagen.ModonomiconProviderBase;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import buildaspell.datagen.entries.modifiers.PowerScaleEntry;
import buildaspell.datagen.entries.modifiers.ProjectileModsEntry;
import buildaspell.datagen.entries.modifiers.ShapeEntry;
import buildaspell.datagen.entries.modifiers.SpecialEntry;
import buildaspell.datagen.entries.modifiers.TimingEntry;
import net.minecraft.world.item.Items;

public class SpellModifiersCategory extends IndexModeCategoryProvider {

    public SpellModifiersCategory(ModonomiconProviderBase parent) {
        super(parent);
    }

    @Override
    protected void generateEntries() {
        add(new PowerScaleEntry(this).generate());
        add(new TimingEntry(this).generate());
        add(new ProjectileModsEntry(this).generate());
        add(new ShapeEntry(this).generate());
        add(new SpecialEntry(this).generate());
    }

    @Override
    protected String categoryName() {
        return "Spell Modifiers";
    }

    @Override
    protected BookIconModel categoryIcon() {
        return BookIconModel.create(Items.REDSTONE);
    }

    @Override
    public String categoryId() {
        return "spell_modifiers";
    }
}
