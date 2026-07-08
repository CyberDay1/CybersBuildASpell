package buildaspell.datagen.entries.modifiers;

import com.klikli_dev.modonomicon.api.datagen.CategoryProviderBase;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class ProjectileModsEntry extends EntryProvider {

    public ProjectileModsEntry(CategoryProviderBase parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {
        this.page("accelerate_pierce", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Accelerate & Pierce");
        this.pageText("Accelerate (15 mana) increases projectile travel speed, making it harder for targets to dodge. Not stackable.\\\nPierce (20 mana) allows the projectile to pass through entities, hitting every target in its path. Stackable: each stack allows one additional pass-through.");

        this.page("bounce_chain", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Bounce & Chain");
        this.pageText("Bounce (25 mana) causes projectiles to ricochet off surfaces, potentially hitting targets around corners. Not stackable.\\\nChain (30 mana) makes the spell jump to a nearby target after hitting the first, like chain lightning. Stackable for additional chain jumps.");

        this.page("split", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Split (45 mana)");
        this.pageText("Split divides a single projectile into multiple projectiles, each carrying the full spell effect. Stackable: each stack adds one more projectile to the volley.\\\nAt 45 mana per stack, it is the most expensive projectile modifier but incredibly powerful for area saturation.");
    }

    @Override
    protected String entryName() {
        return "Projectile Modifiers";
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
        return BookIconModel.create(Items.ARROW);
    }

    @Override
    protected String entryId() {
        return "projectile_mods";
    }
}
