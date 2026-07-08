package buildaspell.spell;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MarkManager {
    private static final ConcurrentHashMap<UUID, Vec3> marksByPlayer = new ConcurrentHashMap<>();

    public static void setMark(UUID playerUUID, Vec3 position) {
        marksByPlayer.put(playerUUID, position);
    }

    @Nullable
    public static Vec3 getMark(UUID playerUUID) {
        return marksByPlayer.get(playerUUID);
    }

    public static boolean hasMark(UUID playerUUID) {
        return marksByPlayer.containsKey(playerUUID);
    }

    public static void removeMark(UUID playerUUID) {
        marksByPlayer.remove(playerUUID);
    }

    public static void clearAll() {
        marksByPlayer.clear();
    }
}
