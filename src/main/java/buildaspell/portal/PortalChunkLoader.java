package buildaspell.portal;

import buildaspell.BuildASpell;
import buildaspell.compat.NeoPortalsCompat;
import buildaspell.entity.PortalEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;

/**
 * Owns the NeoForge force-loading tickets that keep portal chunks loaded.
 *
 * Rules (per user spec):
 *  - Undialed portal: only the chunk it sits in, so it can be discovered/edited.
 *  - Dialed portal with NeoPortals present: only its own chunk (NeoPortals force-loads the rest).
 *  - Dialed portal without NeoPortals: a 3x3 chunk area so the destination side stays reachable.
 *
 * Tickets are persistent and ref-counted per owner BlockPos, so they survive a server restart
 * (which is what reloads the portal's own chunk and lets it re-affirm its tickets on tick).
 */
public class PortalChunkLoader {
    public static final Identifier ID =
            Identifier.fromNamespaceAndPath(BuildASpell.MOD_ID, "portal");
    public static final TicketController CONTROLLER = new TicketController(ID);

    public static void register(RegisterTicketControllersEvent event) {
        event.register(CONTROLLER);
    }

    /**
     * (Re)applies force-load tickets for a portal based on its current dial/NeoPortals state.
     * Releases the full 3x3 first so shrinking from dialed-no-neoportals back down works cleanly.
     */
    public static void updateTickets(PortalEntity portal) {
        if (!(portal.level() instanceof ServerLevel level)) return;
        BlockPos owner = portal.blockPosition();
        int cx = SectionPos.blockToSectionCoord(owner.getX());
        int cz = SectionPos.blockToSectionCoord(owner.getZ());

        // Drop any existing ring first (idempotent / safe if absent).
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                CONTROLLER.forceChunk(level, owner, cx + dx, cz + dz, false, true);
            }
        }

        int radius;
        if (!portal.isDialed()) {
            radius = 0;
        } else if (NeoPortalsCompat.isLoaded()) {
            radius = 0;
        } else {
            radius = 1;
        }

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                CONTROLLER.forceChunk(level, owner, cx + dx, cz + dz, true, true);
            }
        }
    }

    /** Releases all tickets this portal could be holding (full 3x3 around it). */
    public static void releaseTickets(PortalEntity portal) {
        if (!(portal.level() instanceof ServerLevel level)) return;
        BlockPos owner = portal.blockPosition();
        int cx = SectionPos.blockToSectionCoord(owner.getX());
        int cz = SectionPos.blockToSectionCoord(owner.getZ());
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                CONTROLLER.forceChunk(level, owner, cx + dx, cz + dz, false, true);
            }
        }
    }
}
