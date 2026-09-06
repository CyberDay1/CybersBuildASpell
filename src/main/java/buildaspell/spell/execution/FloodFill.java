package buildaspell.spell.execution;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;

/**
 * Block-space searches behind the Fill modifier.
 *
 * <p>Fill used to be a plain distance test, which made it a sphere that ignored the world around it
 * and reached straight through walls. Both searches here instead walk the space they are actually
 * standing in, so a Fill stops where the room stops.
 */
public final class FloodFill {

    private static final Direction[] HORIZONTAL = {
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
    };

    private FloodFill() {
    }

    /**
     * The cells the search should start from for a cast centred on {@code center}.
     *
     * <p>A cast rarely lands inside the thing it is aimed at — break a wall and the impact point is
     * the air in front of it. So if the centre itself is not part of the medium, the faces touching
     * it are, and the search starts from those instead of finding nothing.
     */
    public static List<BlockPos> seedsAround(Level level, BlockPos center, Predicate<BlockPos> passable) {
        if (level.isLoaded(center) && passable.test(center)) {
            return List.of(center);
        }
        List<BlockPos> seeds = new ArrayList<>();
        for (Direction dir : Direction.values()) {
            BlockPos next = center.relative(dir);
            if (level.isLoaded(next) && passable.test(next)) {
                seeds.add(next);
            }
        }
        return seeds;
    }

    /**
     * Every cell reachable from {@code seeds} through {@code passable}, walking all six faces.
     *
     * <p>Used where the medium bounds itself: a body of water ends at its shore, a mass of stone
     * ends at the open air, so there is nothing to spill out of.
     */
    public static List<BlockPos> connected(Level level, BlockPos anchor, Collection<BlockPos> seeds,
                                           int maxRadius, int budget, Predicate<BlockPos> passable) {
        List<BlockPos> found = new ArrayList<>();
        if (budget <= 0 || seeds.isEmpty()) {
            return found;
        }

        Set<BlockPos> seen = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        for (BlockPos seed : seeds) {
            BlockPos fixed = seed.immutable();
            if (seen.add(fixed)) {
                queue.add(fixed);
            }
        }

        while (!queue.isEmpty() && found.size() < budget) {
            BlockPos pos = queue.poll();
            found.add(pos);

            for (Direction dir : Direction.values()) {
                BlockPos next = pos.relative(dir);
                if (!seen.add(next)) {
                    continue;
                }
                if (withinRadius(next, anchor, maxRadius) && level.isLoaded(next) && passable.test(next)) {
                    queue.add(next);
                }
            }
        }
        return found;
    }

    /**
     * Pours into the space around {@code anchor} the way a liquid would: it settles to the bottom,
     * then rises one layer at a time, and stops at the height where it comes up to the surface.
     *
     * <p>Each layer rises if it is held: either it closed against walls, or it ran past the reach
     * limit but still has a roof over it. It stops where the layer both escapes the reach limit and
     * has nothing overhead that this cast could ever reach, which is the point where it would stop
     * filling anything and start burying the landscape. So a hole fills to ground level, a walled
     * pen fills to the top of its wall, a room fills to its ceiling as far as {@code budget}
     * carries it, and casting on open ground places nothing.
     *
     * <p>{@code maxRadius} is a reach limit, not a wall. A layer running out to it keeps filling
     * upward — the old behaviour of treating that as a spill is what left a big room with only its
     * floor covered. It is a limit in every direction, though, not just outward: see
     * {@link #withinRadius}, which clamps Y by the same figure. And a room wide enough to clip a
     * layer is wide enough that one layer of it eats most of {@code budget}, so a hall far wider
     * than the reach fills as much of itself as one cast can carry, not the whole of it.
     */
    public static List<BlockPos> contained(Level level, BlockPos anchor, int maxRadius, int budget,
                                           Predicate<BlockPos> passable) {
        List<BlockPos> found = new ArrayList<>();
        List<BlockPos> starts = seedsAround(level, anchor, passable);
        if (budget <= 0 || starts.isEmpty()) {
            return found;
        }

        // Settle to the floor first, so a cast taken at head height still fills from the bottom up.
        BlockPos base = starts.get(0);
        while (true) {
            BlockPos below = base.below();
            if (!withinRadius(below, anchor, maxRadius) || !level.isLoaded(below) || !passable.test(below)) {
                break;
            }
            base = below;
        }

        Set<BlockPos> claimed = new HashSet<>();
        // Lowest pending layer first — that ordering is what makes this behave like a rising level
        // rather than an arbitrary flood.
        TreeMap<Integer, Set<BlockPos>> pending = new TreeMap<>();
        pending.computeIfAbsent(base.getY(), y -> new HashSet<>()).add(base);

        while (!pending.isEmpty() && found.size() < budget) {
            Set<BlockPos> seeds = pending.pollFirstEntry().getValue();

            List<BlockPos> layer = new ArrayList<>();
            Set<BlockPos> seen = new HashSet<>();
            Deque<BlockPos> queue = new ArrayDeque<>();
            for (BlockPos seed : seeds) {
                if (!claimed.contains(seed) && seen.add(seed)) {
                    queue.add(seed);
                }
            }

            // Did this layer close against walls, or did it just run out of arm's length?
            boolean clipped = false;
            // Is any part of it out from under cover?
            boolean openAbove = false;

            while (!queue.isEmpty()) {
                BlockPos pos = queue.poll();
                layer.add(pos);
                if (!openAbove && uncovered(level, pos, anchor, maxRadius)) {
                    openAbove = true;
                }

                for (Direction dir : HORIZONTAL) {
                    BlockPos next = pos.relative(dir);
                    if (!withinRadius(next, anchor, maxRadius)) {
                        // Out of reach is not out of bounds. The radius caps how much one cast can
                        // touch, so the layer stops growing here — but only note that there was
                        // more space we could not get to, and only if there really was.
                        clipped |= level.isLoaded(next) && passable.test(next);
                        continue;
                    }
                    if (level.isLoaded(next) && passable.test(next) && seen.add(next)) {
                        queue.add(next);
                    }
                }
            }

            // A layer that closed against walls is held, however wide it turned out to be, so it
            // gets filled and the level rises. One that ran out of reach might still be held just
            // out of sight, so ask what is overhead instead: a roof means we are inside something
            // and should keep going, nothing overhead means the fill has come up out of whatever it
            // was in and is about to start burying the landscape. That is where it stops.
            if (clipped && openAbove) {
                break;
            }

            for (BlockPos pos : layer) {
                if (found.size() >= budget) {
                    break;
                }
                if (claimed.add(pos)) {
                    found.add(pos);
                }
            }

            // Seed the neighbouring layers. Downward matters too: a side passage can open onto a
            // pocket lower than where we started.
            for (BlockPos pos : layer) {
                queueNeighbour(level, pending, claimed, pos.above(), anchor, maxRadius, passable);
                queueNeighbour(level, pending, claimed, pos.below(), anchor, maxRadius, passable);
            }
        }
        return found;
    }

    private static void queueNeighbour(Level level, TreeMap<Integer, Set<BlockPos>> pending,
                                       Set<BlockPos> claimed, BlockPos pos, BlockPos anchor,
                                       int maxRadius, Predicate<BlockPos> passable) {
        if (claimed.contains(pos) || !withinRadius(pos, anchor, maxRadius)
                || !level.isLoaded(pos) || !passable.test(pos)) {
            return;
        }
        pending.computeIfAbsent(pos.getY(), y -> new HashSet<>()).add(pos);
    }

    /**
     * Whether this cell has nothing over it that the cast could ever reach.
     *
     * <p>A ceiling further up than the fill can rise is not holding anything, so it does not count
     * as cover. That is the difference between an enclosed hall and an open Nether plain, whose only
     * roof is bedrock the better part of a hundred blocks up: asking the world where its surface is
     * says "covered" everywhere under that roof, and the fill would never stop.
     *
     * <p>Leaves are not a roof either, so a cast under a tree still knows it is standing outside.
     *
     * <p>On its own this cannot tell a wide open pit from flat ground — stand at the bottom of a
     * quarry and there is no reachable roof there either. It is only meaningful alongside whether
     * the layer was held by walls, which is how {@link #contained} uses it.
     */
    private static boolean uncovered(Level level, BlockPos pos, BlockPos anchor, int maxRadius) {
        // The cast cannot place above this line, so it cannot be held by anything above it either.
        int top = anchor.getY() + maxRadius;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = pos.getY() + 1; y <= top; y++) {
            cursor.set(pos.getX(), y, pos.getZ());
            if (!level.isLoaded(cursor)) {
                return false;
            }
            BlockState state = level.getBlockState(cursor);
            if (state.blocksMotion() && !state.is(BlockTags.LEAVES)) {
                return false;
            }
        }
        return true;
    }

    private static boolean withinRadius(BlockPos pos, BlockPos anchor, int maxRadius) {
        return Math.abs(pos.getX() - anchor.getX()) <= maxRadius
                && Math.abs(pos.getY() - anchor.getY()) <= maxRadius
                && Math.abs(pos.getZ() - anchor.getZ()) <= maxRadius;
    }
}
