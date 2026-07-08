package buildaspell.datagen.entries.advanced;

import com.klikli_dev.modonomicon.api.datagen.CategoryProviderBase;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class CommandsConfigEntry extends EntryProvider {

    public CommandsConfigEntry(CategoryProviderBase parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {
        this.page("mana_commands", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Mana Commands");
        this.pageText("Server operators manage player mana with /buildaspell mana (alias /cbas mana): mana set <amount> [target] sets mana to a value, mana add <amount> [target] adds mana, mana get [target] shows current mana, and mana stats [target] shows max mana, regen, and spell power.\\\nThe target is optional and defaults to you. These require operator permissions.");

        this.page("spell_commands", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Spell Commands");
        this.pageText("The /buildaspell spell suite (alias /cbas spell) manages spells: spell export <slot> prints the spell in a slot as a shareable code, spell copy <slot> copies that code to your clipboard, and spell import <slot> <code> loads a code into a slot.\\\nspell list [target] shows a player's unlocked components, and spell combo <id> <slot> builds a known combo into a slot. These require operator permissions.");

        this.page("config", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Server Configuration");
        this.pageText("The server config lives in a 'buildaspell' folder, split across general.toml, deliveries.toml, effects.toml, modifiers.toml, and wands.toml.\\\nBetween them they control global settings: the mana cost multiplier, default spell power, per-delivery/effect/modifier toggles and cost multipliers, damage multipliers for offensive effects, the maximum number of portals per player, and the list of blocks allowed for Conjure. These server values sync to clients automatically on login.");
    }

    @Override
    protected String entryName() {
        return "Commands & Config";
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
        return BookIconModel.create(Items.COMMAND_BLOCK);
    }

    @Override
    protected String entryId() {
        return "commands_config";
    }
}
