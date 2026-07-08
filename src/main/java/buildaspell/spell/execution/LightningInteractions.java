package buildaspell.spell.execution;

import buildaspell.config.ModConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Custom "what happens when a bolt lands here" behaviour, shared by the Lightning effect and the
 * Lightning Storm combo. Real bolts already handle vanilla side effects (fire, mob conversions,
 * copper de-oxidising); this adds buildaspell-specific block transmutations on top.
 *
 * Nullified strikes are pure spectacle and skip all of this. Everything here is server-side and
 * gated behind the {@code lightningTransmutesBlocks} general config toggle so packs can disable it.
 */
public final class LightningInteractions {
    private LightningInteractions() {}

    /**
     * Resolve the top ground block under (x, z) and apply any custom transmutation to it. Callers
     * pass the horizontal strike position; the ground is found via the motion-blocking heightmap so
     * both aerial effect strikes and ground storm strikes land on the same block the bolt visually hits.
     */
    public static void onStrike(ServerLevel level, double x, double z, boolean nullify) {
        if (nullify) return;
        if (!ModConfig.lightningTransmutesBlocks()) return;

        int bx = (int) Math.floor(x);
        int bz = (int) Math.floor(z);
        int topY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, bx, bz);
        BlockPos pos = new BlockPos(bx, topY - 1, bz);
        transmute(level, pos);
    }

    private static void transmute(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        // Fulgurite: lightning fuses sand into glass, and can reach one block deeper into a sand column.
        if (state.is(Blocks.SAND) || state.is(Blocks.RED_SAND)) {
            fuseToGlass(level, pos);
            BlockPos below = pos.below();
            BlockState belowState = level.getBlockState(below);
            if (belowState.is(Blocks.SAND) || belowState.is(Blocks.RED_SAND)) {
                fuseToGlass(level, below);
            }
        }
    }

    private static void fuseToGlass(ServerLevel level, BlockPos pos) {
        level.setBlockAndUpdate(pos, Blocks.GLASS.defaultBlockState());
        level.sendParticles(ParticleTypes.FLAME,
                pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                8, 0.3, 0.2, 0.3, 0.01);
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6f, 1.4f);
    }
}
