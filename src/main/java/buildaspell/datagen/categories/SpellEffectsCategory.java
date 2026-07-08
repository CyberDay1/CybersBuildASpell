package buildaspell.datagen.categories;

import com.klikli_dev.modonomicon.api.datagen.IndexModeCategoryProvider;
import com.klikli_dev.modonomicon.api.datagen.ModonomiconProviderBase;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import buildaspell.datagen.entries.effects.DirectDamageEntry;
import buildaspell.datagen.entries.effects.HealingBuffsEntry;
import buildaspell.datagen.entries.effects.MovementForceEntry;
import buildaspell.datagen.entries.effects.StatusOffenseEntry;
import buildaspell.datagen.entries.effects.SummoningEntry;
import buildaspell.datagen.entries.effects.TeleportationEntry;
import buildaspell.datagen.entries.effects.UtilityEffectsEntry;
import buildaspell.datagen.entries.effects.WorldManipEntry;
import net.minecraft.world.item.Items;

public class SpellEffectsCategory extends IndexModeCategoryProvider {

    public SpellEffectsCategory(ModonomiconProviderBase parent) {
        super(parent);
    }

    @Override
    protected void generateEntries() {
        add(new DirectDamageEntry(this).generate());
        add(new StatusOffenseEntry(this).generate());
        add(new MovementForceEntry(this).generate());
        add(new TeleportationEntry(this).generate());
        add(new HealingBuffsEntry(this).generate());
        add(new SummoningEntry(this).generate());
        add(new WorldManipEntry(this).generate());
        add(new UtilityEffectsEntry(this).generate());
    }

    @Override
    protected String categoryName() {
        return "Spell Effects";
    }

    @Override
    protected BookIconModel categoryIcon() {
        return BookIconModel.create(Items.BLAZE_POWDER);
    }

    @Override
    public String categoryId() {
        return "spell_effects";
    }
}
