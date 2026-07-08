package buildaspell.spell;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Transient store for the TOUCH (Imbue) delivery: a caster charges a spell into their hands and the
 * next melee attack or interaction discharges it onto the target. Entries are short-lived and never
 * persisted — an imbue that is not used before {@code expiryGameTime} is dropped on the next lookup,
 * and the whole table is cleared on server stop. Keyed by player UUID so a caster only ever holds one
 * pending imbue at a time (re-casting Touch overwrites the previous charge).
 */
public class ImbueManager {

    private record Pending(Spell spell, long expiryGameTime) {}

    private static final ConcurrentHashMap<UUID, Pending> pendingByPlayer = new ConcurrentHashMap<>();

    public static void setImbue(UUID playerUUID, Spell spell, long expiryGameTime) {
        pendingByPlayer.put(playerUUID, new Pending(spell, expiryGameTime));
    }

    /**
     * @return the still-valid imbued spell for this player, or null if none is pending or it has
     *         expired. Expired entries are removed as a side effect so the table self-prunes.
     */
    @Nullable
    public static Spell consumeIfValid(UUID playerUUID, long currentGameTime) {
        Pending pending = pendingByPlayer.get(playerUUID);
        if (pending == null) {
            return null;
        }
        pendingByPlayer.remove(playerUUID);
        if (currentGameTime > pending.expiryGameTime()) {
            return null;
        }
        return pending.spell();
    }

    public static boolean hasImbue(UUID playerUUID) {
        return pendingByPlayer.containsKey(playerUUID);
    }

    public static void clearAll() {
        pendingByPlayer.clear();
    }
}
