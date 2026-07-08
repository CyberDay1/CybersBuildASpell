package buildaspell.portal;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Public API for the portal registry. State lives in {@link PortalSavedData} attached to the
 * overworld's DimensionDataStorage so it persists across restarts. The server reference is
 * captured on ServerStartingEvent; before that (or on a dedicated-client) calls degrade to no-ops
 * / empty results, which is safe because portals only exist server-side.
 */
public class PortalManager {
    @Nullable
    private static MinecraftServer server;

    public static void setServer(@Nullable MinecraftServer minecraftServer) {
        server = minecraftServer;
    }

    @Nullable
    private static PortalSavedData data() {
        if (server == null) return null;
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) return null;
        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(PortalSavedData::new, PortalSavedData::load),
                PortalSavedData.DATA_NAME);
    }

    public static void registerPortal(UUID portalUUID, String name, ResourceKey<Level> dimension, Vec3 position, @Nullable UUID ownerUUID) {
        PortalSavedData data = data();
        if (data == null) return;
        PortalInfo info = new PortalInfo(portalUUID, name, dimension, position, ownerUUID);
        data.registry.put(portalUUID, info);
        if (ownerUUID != null) {
            data.discovered.computeIfAbsent(ownerUUID, k -> java.util.concurrent.ConcurrentHashMap.newKeySet()).add(portalUUID);
        }
        data.setDirty();
    }

    public static void unregisterPortal(UUID portalUUID) {
        PortalSavedData data = data();
        if (data == null) return;
        data.registry.remove(portalUUID);
        for (Set<UUID> discovered : data.discovered.values()) {
            discovered.remove(portalUUID);
        }
        data.setDirty();
    }

    public static void discoverPortal(UUID playerUUID, UUID portalUUID) {
        PortalSavedData data = data();
        if (data == null) return;
        data.discovered.computeIfAbsent(playerUUID, k -> java.util.concurrent.ConcurrentHashMap.newKeySet()).add(portalUUID);
        data.setDirty();
    }

    public static boolean hasDiscovered(UUID playerUUID, UUID portalUUID) {
        PortalSavedData data = data();
        if (data == null) return false;
        Set<UUID> discovered = data.discovered.get(playerUUID);
        return discovered != null && discovered.contains(portalUUID);
    }

    @Nullable
    public static PortalInfo getPortalInfo(UUID portalUUID) {
        PortalSavedData data = data();
        if (data == null) return null;
        return data.registry.get(portalUUID);
    }

    public static List<PortalInfo> getDiscoveredPortals(UUID playerUUID) {
        PortalSavedData data = data();
        if (data == null) return Collections.emptyList();
        Set<UUID> discovered = data.discovered.get(playerUUID);
        if (discovered == null) return Collections.emptyList();
        return discovered.stream()
                .map(data.registry::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public static void updatePortalName(UUID portalUUID, String newName) {
        PortalSavedData data = data();
        if (data == null) return;
        PortalInfo info = data.registry.get(portalUUID);
        if (info != null) {
            info.setName(newName);
            data.setDirty();
        }
    }
}
