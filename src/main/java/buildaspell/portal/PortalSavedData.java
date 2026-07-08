package buildaspell.portal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Disk-persisted backing store for the portal registry. Attached to the overworld's
 * SavedDataStorage so that dialed links, names and per-player discovery survive a
 * server restart. {@link PortalManager} is the public API; this class is just storage.
 */
public class PortalSavedData extends SavedData {
    private static final Codec<Map<UUID, Set<UUID>>> DISCOVERED_CODEC =
            Codec.unboundedMap(
                    Codec.STRING.xmap(UUID::fromString, UUID::toString),
                    Codec.STRING.xmap(UUID::fromString, UUID::toString).listOf()
                            .xmap(list -> (Set<UUID>) new HashSet<>(list), set -> new ArrayList<>(set))
            );

    public static final Codec<PortalSavedData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            PortalInfo.CODEC.listOf().optionalFieldOf("portals", List.of())
                    .forGetter(d -> new ArrayList<>(d.registry.values())),
            DISCOVERED_CODEC.optionalFieldOf("discovered", Map.of())
                    .forGetter(d -> d.discovered)
    ).apply(inst, PortalSavedData::fromData));

    public static final SavedDataType<PortalSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("buildaspell", "portals"),
            PortalSavedData::new, CODEC);

    final Map<UUID, PortalInfo> registry = new ConcurrentHashMap<>();
    final Map<UUID, Set<UUID>> discovered = new ConcurrentHashMap<>();

    public PortalSavedData() {
    }

    private static PortalSavedData fromData(List<PortalInfo> portals, Map<UUID, Set<UUID>> discovered) {
        PortalSavedData data = new PortalSavedData();
        for (PortalInfo info : portals) {
            data.registry.put(info.getPortalUUID(), info);
        }
        discovered.forEach((player, set) -> {
            Set<UUID> concurrent = ConcurrentHashMap.newKeySet();
            concurrent.addAll(set);
            data.discovered.put(player, concurrent);
        });
        return data;
    }
}
