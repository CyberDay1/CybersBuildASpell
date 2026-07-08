package buildaspell.spell;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SpellLootingTracker {
    private static final long EXPIRY_TICKS = 1200; // 60 seconds

    private record Entry(int level, long timestamp) {}

    private static final ConcurrentHashMap<UUID, Entry> lootingLevels = new ConcurrentHashMap<>();
    private static long currentTick = 0;

    public static void setLootingLevel(UUID entityUUID, int level) {
        if (level > 0) {
            lootingLevels.put(entityUUID, new Entry(level, currentTick));
        }
    }

    public static int getLootingLevel(UUID entityUUID) {
        Entry entry = lootingLevels.get(entityUUID);
        if (entry == null) return 0;
        if (currentTick - entry.timestamp > EXPIRY_TICKS) {
            lootingLevels.remove(entityUUID);
            return 0;
        }
        return entry.level;
    }

    public static void clearLootingLevel(UUID entityUUID) {
        lootingLevels.remove(entityUUID);
    }

    public static void clearAll() {
        lootingLevels.clear();
        currentTick = 0;
    }

    public static void tick() {
        currentTick++;
        if (currentTick % 200 == 0) { // Purge every 10 seconds
            lootingLevels.entrySet().removeIf(e -> currentTick - e.getValue().timestamp > EXPIRY_TICKS);
        }
    }
}
