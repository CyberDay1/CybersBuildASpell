package buildaspell.registry;

import buildaspell.BuildASpell;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/** Tags buildaspell defines for its own content, overridable by datapacks. */
public final class ModTags {

    private ModTags() {
    }

    /**
     * What Conjure is allowed to build with.
     *
     * <p>
     * Conjure makes blocks out of mana, so anything in here is effectively free and infinite. That
     * is the whole reason the tag exists and the only rule for what belongs in it: common ground
     * material with no economy behind it. Stone and its variants, dirt, sand, gravel, clay,
     * sandstone, the nether and end equivalents. Nothing smelted, mined for, or dyed &mdash; no ores
     * or metals, no gems, no obsidian, and no bedrock.
     *
     * <p>
     * A tag rather than a config list so packs can extend it to their own stone types without
     * touching a config file, and so modded stone comes along by tag inheritance.
     */
    public static final TagKey<Block> CONJURABLE = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(BuildASpell.MOD_ID, "conjurable"));
}
