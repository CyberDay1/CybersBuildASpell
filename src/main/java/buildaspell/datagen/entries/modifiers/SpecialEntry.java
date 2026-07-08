package buildaspell.datagen.entries.modifiers;

import com.klikli_dev.modonomicon.api.datagen.CategoryProviderBase;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class SpecialEntry extends EntryProvider {

    public SpecialEntry(CategoryProviderBase parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {
        this.page("nullify", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Nullify (5 mana)");
        this.pageText("Nullify prevents the spell from dealing any damage, even if damage-dealing effects are present. Not stackable. At only 5 mana, it is the cheapest modifier.\\\nUse it to apply status effects without killing the target, or to safely test explosive spells without destroying terrain.");

        this.page("gentleness", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Gentleness (10 mana)");
        this.pageText("Gentleness prevents the spell from destroying or modifying blocks, while still allowing entity effects. Not stackable.\\\nEssential for using Explosion in built-up areas without grief. At 10 mana it is very cheap and should be standard in most combat spell loadouts.");

        this.page("double", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Double (50 mana)");
        this.pageText("Double causes every effect in the spell to trigger twice on the same target. Stackable: each stack adds another trigger.\\\nAt 50 mana per stack, it is the most expensive modifier in the game. Unlike Echo which re-casts with a delay, Double applies all repetitions instantly.");

        this.page("leech", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Leech (30 mana)");
        this.pageText("Leech heals you for a fraction of the damage your spell deals to enemies. Stackable: each stack returns more of the damage as health.\\\nAttach it to a Damage spell to sustain yourself through a fight, turning offense directly into staying power.");

        this.page("sunder", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Sunder (25 mana)");
        this.pageText("Sunder adds bonus damage that scales with how much armor the target is wearing. Stackable.\\\nBecause spell damage already ignores armor, Sunder is your anti-tank tool: the more heavily armored the foe, the harder it bites. It does little against unarmored targets.");

        this.page("return", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Return (35 mana)");
        this.pageText("Return makes a projectile spell boomerang back toward you after it hits or reaches the end of its flight, re-applying its effects to anything it passes on the way home.\\\nNot stackable. Only useful on projectile deliveries such as Cast and Tracking.");
    }

    @Override
    protected String entryName() {
        return "Special Modifiers";
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
        return BookIconModel.create(Items.AMETHYST_SHARD);
    }

    @Override
    protected String entryId() {
        return "special";
    }
}
