package buildaspell.datagen.categories;

import com.klikli_dev.modonomicon.api.datagen.IndexModeCategoryProvider;
import com.klikli_dev.modonomicon.api.datagen.ModonomiconProviderBase;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import buildaspell.datagen.entries.delivery.CastDeliveryEntry;
import buildaspell.datagen.entries.delivery.RuneDeliveryEntry;
import buildaspell.datagen.entries.delivery.SelfDeliveryEntry;
import buildaspell.datagen.entries.delivery.SightEntry;
import buildaspell.datagen.entries.delivery.TouchDeliveryEntry;
import buildaspell.datagen.entries.delivery.TrackingEntry;
import buildaspell.datagen.entries.delivery.TrapDeliveryEntry;
import net.minecraft.world.item.Items;

public class DeliveryMethodsCategory extends IndexModeCategoryProvider {

    public DeliveryMethodsCategory(ModonomiconProviderBase parent) {
        super(parent);
    }

    @Override
    protected void generateEntries() {
        add(new SelfDeliveryEntry(this).generate());
        add(new SightEntry(this).generate());
        add(new RuneDeliveryEntry(this).generate());
        add(new CastDeliveryEntry(this).generate());
        add(new TrackingEntry(this).generate());
        add(new TouchDeliveryEntry(this).generate());
        add(new TrapDeliveryEntry(this).generate());
    }

    @Override
    protected String categoryName() {
        return "Delivery Methods";
    }

    @Override
    protected BookIconModel categoryIcon() {
        return BookIconModel.create(Items.BLAZE_ROD);
    }

    @Override
    public String categoryId() {
        return "delivery_methods";
    }
}
