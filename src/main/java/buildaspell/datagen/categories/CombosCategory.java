package buildaspell.datagen.categories;

import com.klikli_dev.modonomicon.api.datagen.IndexModeCategoryProvider;
import com.klikli_dev.modonomicon.api.datagen.ModonomiconProviderBase;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import buildaspell.datagen.entries.combos.ArcaneSynergiesEntry;
import buildaspell.datagen.entries.combos.CataclysmCombosEntry;
import buildaspell.datagen.entries.combos.FlightEscapeEntry;
import buildaspell.datagen.entries.combos.FloodCombosEntry;
import buildaspell.datagen.entries.combos.GravityCombosEntry;
import buildaspell.datagen.entries.combos.PortalFortressEntry;
import buildaspell.datagen.entries.combos.SanctuaryCombosEntry;
import buildaspell.datagen.entries.combos.SummonCombosEntry;
import buildaspell.datagen.entries.combos.UpheavalCombosEntry;
import net.minecraft.world.item.Items;

public class CombosCategory extends IndexModeCategoryProvider {

    public CombosCategory(ModonomiconProviderBase parent) {
        super(parent);
    }

    @Override
    protected void generateEntries() {
        add(new ArcaneSynergiesEntry(this).generate());
        add(new SummonCombosEntry(this).generate());
        add(new GravityCombosEntry(this).generate());
        add(new FlightEscapeEntry(this).generate());
        add(new PortalFortressEntry(this).generate());
        add(new FloodCombosEntry(this).generate());
        add(new CataclysmCombosEntry(this).generate());
        add(new UpheavalCombosEntry(this).generate());
        add(new SanctuaryCombosEntry(this).generate());
    }

    @Override
    protected String categoryName() {
        return "Secret Combos";
    }

    @Override
    protected BookIconModel categoryIcon() {
        return BookIconModel.create(Items.NETHER_STAR);
    }

    @Override
    public String categoryId() {
        return "combos";
    }
}
