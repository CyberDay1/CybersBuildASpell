package buildaspell.compat;

import dev.cyberday.neoportals.api.PortalAPI;
import dev.cyberday.neoportals.portal.Portal;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Optional integration with the NeoPortals mod (mod id: "neoportals").
 *
 * <p>All methods that reference NeoPortals classes are only called after
 * {@link #isLoaded()} returns true, so the JVM never resolves those classes
 * unless the mod is present on the classpath at runtime.
 */
public final class NeoPortalsCompat {

    private NeoPortalsCompat() {}

    /** Returns true when the NeoPortals mod is loaded in this game session. */
    public static boolean isLoaded() {
        return ModList.get().isLoaded("neoportals");
    }

    /**
     * Creates a linked NeoPortals portal pair between two positions and returns
     * the UUIDs of the spawned portals: {@code [sourceUUID, destUUID]}.
     *
     * <p>Only call this when {@link #isLoaded()} is true.
     *
     * @param levelA    server level for the source portal
     * @param centerA   world-space center of the source portal
     * @param axisWA    horizontal axis of source portal
     * @param axisHA    vertical axis of source portal
     * @param widthA    source portal width in world units
     * @param heightA   source portal height in world units
     * @param levelB    server level for the destination portal
     * @param centerB   world-space center of the destination portal
     * @param axisWB    horizontal axis of destination portal
     * @param axisHB    vertical axis of destination portal
     * @param widthB    destination portal width
     * @param heightB   destination portal height
     * @return UUIDs {@code [sourcePortalUUID, destPortalUUID]}, or {@code null} on failure
     */
    @Nullable
    public static UUID[] createPortalPair(
            ServerLevel levelA, Vec3 centerA, Vec3 axisWA, Vec3 axisHA, float widthA, float heightA,
            ServerLevel levelB, Vec3 centerB, Vec3 axisWB, Vec3 axisHB, float widthB, float heightB) {
        try {
            Portal[] pair = PortalAPI.createPortalPair(
                    levelA, centerA, axisWA, axisHA, widthA, heightA,
                    levelB, centerB, axisWB, axisHB);
            // The API creates the dest portal with side A's size — correct it if different
            if (widthB != widthA || heightB != heightA) {
                PortalAPI.setOrientationAndSize(pair[1], axisWB, axisHB, widthB, heightB);
            }
            return new UUID[]{pair[0].getUUID(), pair[1].getUUID()};
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Removes a NeoPortals portal entity by UUID from the given level.
     * Safe to call if the portal no longer exists.
     *
     * <p>Only call this when {@link #isLoaded()} is true.
     *
     * @param level the server level that contains the portal
     * @param uuid  UUID of the NeoPortals Portal entity to remove
     */
    public static void removePortal(ServerLevel level, UUID uuid) {
        try {
            Portal p = PortalAPI.getPortalByUUID(level, uuid);
            if (p != null) {
                p.discard();
            }
        } catch (Exception e) {
            // Portal already gone; ignore
        }
    }

    /**
     * Looks up a NeoPortals portal entity by UUID. Returns {@code null} if not found.
     *
     * <p>Only call this when {@link #isLoaded()} is true.
     *
     * @param level the server level to search in
     * @param uuid  UUID of the NeoPortals Portal entity
     * @return the Portal, or {@code null} if it no longer exists
     */
    @Nullable
    public static Portal getPortal(ServerLevel level, UUID uuid) {
        try {
            return PortalAPI.getPortalByUUID(level, uuid);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns the server level for a given dimension key.
     * Convenience helper to avoid direct MinecraftServer access in PortalEntity.
     *
     * @param fromLevel any ServerLevel — used to reach the MinecraftServer
     * @param dimKey    target dimension key
     * @return the ServerLevel, or {@code null} if not loaded
     */
    @Nullable
    public static ServerLevel getLevel(ServerLevel fromLevel, ResourceKey<Level> dimKey) {
        return fromLevel.getServer().getLevel(dimKey);
    }
}
