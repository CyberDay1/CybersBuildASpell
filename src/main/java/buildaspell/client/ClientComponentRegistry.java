package buildaspell.client;

import buildaspell.network.SyncComponentRegistryPacket.Entry;

import java.util.List;

/**
 * Client-side cache of datapack-authored spell components synced from the server. The spell
 * builder reads this to append datapack effects to its palette alongside the built-in enum
 * components. Holds display metadata only (id + icon/cost/name); behavior lives server-side.
 *
 * <p>Plain data holder — no client-only types — so the common packet handler can write to it
 * without dragging client classes onto the dedicated server.
 */
public final class ClientComponentRegistry {
    private ClientComponentRegistry() {}

    private static volatile List<Entry> effects = List.of();

    public static void setEffects(List<Entry> entries) {
        effects = List.copyOf(entries);
    }

    /** Datapack-only effects (no enum constant), in arbitrary registry order. */
    public static List<Entry> effects() {
        return effects;
    }
}
