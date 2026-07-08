package buildaspell.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The light the Light effect leaves behind.
 *
 * <p>Behaves exactly like {@code minecraft:light} — invisible, no collision, emits at its own
 * level — except that it clears itself when its scheduled tick comes due. The Light effect places
 * these with a lifetime rather than vanilla light blocks, which have no expiry of their own and
 * cannot be mined in survival, so a cast lights an area instead of permanently rewriting it.
 */
public class SpellLightBlock extends LightBlock {
    public SpellLightBlock(Properties properties) {
        super(properties);
    }

    /**
     * Fires once, at the tick scheduled when the block was placed. Scheduled ticks are saved with
     * the chunk, so the lifetime survives an unload or a server restart rather than stranding the
     * block the way an in-memory timer would.
     */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.removeBlock(pos, false);
    }
}
