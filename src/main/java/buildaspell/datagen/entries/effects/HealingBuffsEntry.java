package buildaspell.datagen.entries.effects;

import com.klikli_dev.modonomicon.api.datagen.CategoryProviderBase;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class HealingBuffsEntry extends EntryProvider {

    public HealingBuffsEntry(CategoryProviderBase parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {
        this.page("heal_shield", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Heal & Shield");
        this.pageText("Heal (35 mana) restores health to the target, with the amount scaling with Spell Power.\\\n\\\nShield (45 mana) grants temporary absorption hearts that take damage before your real health.\\\n\\\nShield is more expensive but proactive: apply it before entering combat.");

        this.page("saturation_speed_haste", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Sustain & Speed");
        this.pageText("Saturation (20 mana) restores food points, keeping you fed during long adventures.\\\n\\\nSpeed (25 mana) applies the Speed effect for faster movement.\\\n\\\nHaste (25 mana) applies the Haste effect for faster mining and attack speed.\\\n\\\nAll durations scale with the Prolonged modifier.");

        this.page("invis_cleanse", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Invisibility & Cleanse");
        this.pageText("Invisibility (30 mana) makes the target invisible, hiding their nameplate and model.\\\n\\\nCleanse (20 mana) removes all negative status effects from the target: a cheap and essential defensive tool.\\\n\\\nUse Cleanse on Self delivery for a quick emergency purge.");

        this.page("strengthen_resist", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Strengthen & Resist");
        this.pageText("Strengthen (30 mana) grants the Strength effect for increased melee damage.\\\n\\\nResist (35 mana) grants Resistance, reducing all incoming damage.\\\n\\\nBoth potency and duration scale with Increased Power and Prolonged.\\\n\\\nCast on Self before a fight, or on an ally to bolster them.");

        this.page("regenerate", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Regenerate");
        this.pageText("Regenerate (35 mana) grants the Regeneration effect, healing the target steadily over time rather than all at once like Heal.\\\n\\\nIt is ideal for sustained fights and recovers more total health for the mana when the buff is allowed to run its full duration.");
    }

    @Override
    protected String entryName() {
        return "Healing & Buffs";
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
        return BookIconModel.create(Items.GOLDEN_APPLE);
    }

    @Override
    protected String entryId() {
        return "healing_buffs";
    }
}
