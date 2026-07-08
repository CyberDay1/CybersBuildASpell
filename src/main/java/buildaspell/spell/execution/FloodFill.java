package buildaspell.spell.execution;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

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
     * then rises one layer at a time, and stops at the height where the space would spill out.
     *
     * <p>So a hole in the ground fills to ground level and no further, and a sealed room fills to
     * its ceiling. Casting somewhere genuinely open returns nothing — there is no container to
     * fill, and the alternative is burying the landscape.
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

            boolean spilled = false;
            while (!queue.isEmpty()) {
                BlockPos pos = queue.poll();
                layer.add(pos);

                for (Direction dir : HORIZONTAL) {
                    BlockPos next = pos.relative(dir);
                    if (!level.isLoaded(next) || !passable.test(next)) {
                        continue;
                    }
                    if (!withinRadius(next, anchor, maxRadius)) {
                        // The layer runs past everything we can see, so it is not enclosed at this
                        // height. Whatever we have already placed below stays; this layer does not.
                        spilled = true;
                        break;
                    }
                    if (seen.add(next)) {
                        queue.add(next);
                    }
                }
                if (spilled) {
                    break;
                }
            }

            if (spilled) {
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

    private static boolean withinRadius(BlockPos pos, BlockPos anchor, int maxRadius) {
        return Math.abs(pos.getX() - anchor.getX()) <= maxRadius
                && Math.abs(pos.getY() - anchor.getY()) <= maxRadius
                && Math.abs(pos.getZ() - anchor.getZ()) <= maxRadius;
    }
}
