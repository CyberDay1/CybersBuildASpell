package buildaspell.spell.data;

import buildaspell.BuildASpell;
import buildaspell.spell.DeliveryMethod;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.Nullable;

import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads spell delivery definitions from {@code data/<namespace>/spell_deliveries/*.json}.
 * Server-authoritative; the numeric scalars feed delivery behaviour. Falls back to the
 * existing config values when a definition is absent (e.g. client-side before registry sync),
 * preserving current behavior exactly.
 */
public class DeliveryRegistry extends SimplePreparableReloadListener<Map<ResourceLocation, JsonElement>> {

    private static final String DIRECTORY = "spell_deliveries";
    private static final Map<ResourceLocation, DeliveryDefinition> DEFS = new HashMap<>();

    @Override
    protected Map<ResourceLocation, JsonElement> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, JsonElement> result = new HashMap<>();
        Map<ResourceLocation, Resource> resources = resourceManager.listResources(DIRECTORY,
                id -> id.getPath().endsWith(".json"));
        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            ResourceLocation fullId = entry.getKey();
            String path = fullId.getPath();
            String stripped = path.substring(DIRECTORY.length() + 1, path.length() - 5);
            ResourceLocation resourceId = ResourceLocation.fromNamespaceAndPath(fullId.getNamespace(), stripped);
            try (var reader = new InputStreamReader(entry.getValue().open())) {
                result.put(resourceId, JsonParser.parseReader(reader));
            } catch (Exception e) {
                BuildASpell.LOGGER.error("Failed to read spell delivery: {}", fullId, e);
            }
        }
        return result;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager resourceManager, ProfilerFiller profiler) {
        DEFS.clear();
        for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
            ResourceLocation id = entry.getKey();
            DeliveryDefinition.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                    .resultOrPartial(err -> BuildASpell.LOGGER.error("Invalid spell delivery {}: {}", id, err))
                    .ifPresent(def -> DEFS.put(id, def));
        }
        BuildASpell.LOGGER.info("Loaded {} spell delivery definitions", DEFS.size());
    }

    @Nullable
    public static DeliveryDefinition get(DeliveryMethod delivery) {
        return DEFS.get(ResourceLocation.fromNamespaceAndPath(BuildASpell.MOD_ID, delivery.getSerializedName()));
    }

    /** Registry scalar for a delivery, or {@code fallback} when no definition supplies it. */
    public static double param(DeliveryMethod delivery, String name, double fallback) {
        DeliveryDefinition def = get(delivery);
        return def != null ? def.param(name, fallback) : fallback;
    }
}
