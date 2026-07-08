package buildaspell.portal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Disk-persisted backing store for the portal registry. Attached to the overworld's
 * DimensionDataStorage so that dialed links, names and per-player discovery survive a
 * server restart. {@link PortalManager} is the public API; this class is just storage.
 */
public class PortalSavedData extends SavedData {
    public static final String DATA_NAME = "buildaspell_portals";

    private static final Codec<Map<UUID, Set<UUID>>> DISCOVERED_CODEC =
            Codec.unboundedMap(
                    Codec.STRING.xmap(UUID::fromString, UUID::toString),
                    Codec.STRING.xmap(UUID::fromString, UUID::toString).listOf()
                            .xmap(list -> (Set<UUID>) new HashSet<>(list), set -> new java.util.ArrayList<>(set))
            );

    final Map<UUID, PortalInfo> registry = new ConcurrentHashMap<>();
    final Map<UUID, Set<UUID>> discovered = new ConcurrentHashMap<>();

    public PortalSavedData() {
    }

    public static PortalSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        PortalSavedData data = new PortalSavedData();

        ListTag portals = tag.getList("portals", Tag.TAG_COMPOUND);
        for (int i = 0; i < portals.size(); i++) {
            Tag entry = portals.get(i);
            PortalInfo.CODEC.parse(NbtOps.INSTANCE, entry).result()
                    .ifPresent(info -> data.registry.put(info.getPortalUUID(), info));
        }

        if (tag.contains("discovered")) {
            DataResult<Map<UUID, Set<UUID>>> result =
                    DISCOVERED_CODEC.parse(NbtOps.INSTANCE, tag.get("discovered"));
            result.result().ifPresent(map -> map.forEach((player, set) -> {
                Set<UUID> concurrent = ConcurrentHashMap.newKeySet();
                concurrent.addAll(set);
                data.discovered.put(player, concurrent);
            }));
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag portals = new ListTag();
        for (PortalInfo info : registry.values()) {
            PortalInfo.CODEC.encodeStart(NbtOps.INSTANCE, info).result().ifPresent(portals::add);
        }
        tag.put("portals", portals);

        DISCOVERED_CODEC.encodeStart(NbtOps.INSTANCE, discovered).result()
                .ifPresent(t -> tag.put("discovered", t));

        return tag;
    }
}
