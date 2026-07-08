package buildaspell.datagen.entries.effects;

import com.klikli_dev.modonomicon.api.datagen.CategoryProviderBase;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class MovementForceEntry extends EntryProvider {

    public MovementForceEntry(CategoryProviderBase parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {
        this.page("push_pull", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Push & Pull");
        this.pageText("Push (25 mana) knocks the target away from the impact point with considerable force.\\\n\\\nPull (25 mana) does the opposite, dragging the target toward the impact point.\\\n\\\nBoth are useful for battlefield control and can be combined with other effects for devastating combos.");

        this.page("yeet_launch_slam", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Yeet, Launch & Slam");
        this.pageText("Yeet (30 mana) sends the target flying in the direction the target itself is facing.\\\n\\\nLaunch (35 mana) propels the target straight up into the air.\\\n\\\nSlam (30 mana) smashes the target into the ground.\\\n\\\nThese effects scale with Spell Power for greater force.");

        this.page("levitation_slowfall", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Levitation & Slow Fall");
        this.pageText("Levitation (25 mana) applies the Levitation effect, causing the target to float upward uncontrollably.\\\n\\\nSlow Fall (15 mana) applies Slow Falling, allowing a gentle descent.\\\n\\\nCasters who master the interplay of rising and falling are said to have touched true flight.\\\n\\\nExperiment.");

        this.page("grapple", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Grapple (35 mana)");
        this.pageText("Grapple yanks you toward the point you are aiming at, with a slight upward lift to help you clear ledges.\\\n\\\nIt is a mobility spell for the caster rather than a force applied to enemies: aim at a distant block or rooftop and pull yourself across the gap.\\\n\\\nStrength scales with Increased Power.");

        this.page("gust", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Gust (30 mana)");
        this.pageText("Gust unleashes a cone of wind in the direction you face, shoving every entity in front of you outward.\\\n\\\nUnlike Push, which radiates from an impact point, Gust is directional: sweep groups off ledges or create breathing room.\\\n\\\nForce scales with Increased Power and the cone reaches further with Increased Area.");
    }

    @Override
    protected String entryName() {
        return "Movement & Force";
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
        return BookIconModel.create(Items.FEATHER);
    }

    @Override
    protected String entryId() {
        return "movement_force";
    }
}
