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
        this.pageText("Nullify removes all damage to creatures from the spell: Damage, Explosion, Lightning and the like hurt nothing, while everything else still happens.\\\n\\\nStatus effects, knockback, ignition and block changes are untouched.\\\n\\\nNot stackable.\\\n\\\nAt only 5 mana, it is the cheapest modifier.\\\n\\\nUse it to apply status effects without killing the target.\\\n\\\nIt does not protect terrain: a Nullified Explosion still breaks blocks once its Increased Power climbs high enough, so keep the power low when you want a fully harmless blast.");

        this.page("gentleness", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Gentleness (10 mana)");
        this.pageText("Gentleness makes harvesting spells careful instead of greedy.\\\n\\\nNot stackable.\\\n\\\nOn Break, blocks are collected intact as if mined with Silk Touch.\\\n\\\nOn Reap, crops are gathered plainly, without Fortunate Son's bonus multiplication.\\\n\\\nAt 10 mana it is very cheap: standard kit for any mining or farming spell.");

        this.page("double", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Double (50 mana)");
        this.pageText("Double adds an extra projectile to Cast and Tracking spells.\\\n\\\nStackable: each stack fires another projectile alongside the first.\\\n\\\nAt 50 mana per stack, it is the most expensive modifier in the game.\\\n\\\nUnlike Echo, which re-casts the spell after a delay, Double delivers all its projectiles in the same instant.");

        this.page("leech", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Leech (30 mana)");
        this.pageText("Leech heals you for a fraction of the damage your spell deals to enemies.\\\n\\\nStackable: each stack returns more of the damage as health.\\\n\\\nAttach it to a Damage spell to sustain yourself through a fight, turning offense directly into staying power.");

        this.page("sunder", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Sunder (25 mana)");
        this.pageText("Sunder adds bonus damage that scales with how much armor the target is wearing.\\\n\\\nStackable.\\\n\\\nBecause spell damage already ignores armor, Sunder is your anti-tank tool: the more heavily armored the foe, the harder it bites.\\\n\\\nIt does little against unarmored targets.");

        this.page("return", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Return (35 mana)");
        this.pageText("Return makes a projectile spell boomerang back toward you after it hits or reaches the end of its flight, re-applying its effects to anything it passes on the way home.\\\n\\\nNot stackable.\\\n\\\nOnly useful on projectile deliveries such as Cast and Tracking.");
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
