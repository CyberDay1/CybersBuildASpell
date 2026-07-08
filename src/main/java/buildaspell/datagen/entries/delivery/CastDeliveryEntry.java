package buildaspell.datagen.entries.delivery;

import com.klikli_dev.modonomicon.api.datagen.CategoryProviderBase;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class CastDeliveryEntry extends EntryProvider {

    public CastDeliveryEntry(CategoryProviderBase parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {
        this.page("overview", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Cast Delivery (25 mana)");
        this.pageText("The Cast delivery method fires a magical projectile in the direction the caster is looking. At 25 base mana, it offers a good balance between cost and versatility.\\\nThe projectile travels in a straight line and triggers on the first entity or block it hits.");

        this.page("modifiers", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Projectile Modifiers");
        this.pageText("Cast projectiles can be enhanced with several modifiers: Accelerate increases travel speed, Pierce allows the projectile to pass through entities and hit multiple targets, Bounce makes it ricochet off surfaces, and Split divides it into multiple projectiles.");
    }

    @Override
    protected String entryName() {
        return "Cast";
    }

    @Override
    protected String entryDescription() {
        return "Fire a magical projectile";
    }

    @Override
    protected Pair<Integer, Integer> entryBackground() {
        return EntryBackground.DEFAULT;
    }

    @Override
    protected BookIconModel entryIcon() {
        return BookIconModel.create(Items.BLAZE_ROD);
    }

    @Override
    protected String entryId() {
        return "cast_delivery";
    }
}
