package buildaspell.spell.data;

import buildaspell.BuildASpell;
import buildaspell.spell.Spell;
import buildaspell.spell.SpellCombo;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads spell combo definitions from {@code data/<namespace>/spell_combos/*.json}.
 * Once loaded this is authoritative on the server (built-ins ship as bundled JSON, so
 * datapack combos sit alongside them). Before load — e.g. on a client that hasn't been
 * sent the registry yet — {@link #detect} falls back to the built-in {@link SpellCombo}
 * enum so cost preview and combo gating still work for shipped combos.
 */
public class ComboRegistry extends SimplePreparableReloadListener<Map<ResourceLocation, JsonElement>> {

    private static final String DIRECTORY = "spell_combos";
    // Iteration order is sorted by id for deterministic detection between overlapping combos.
    private static final List<Map.Entry<ResourceLocation, ComboDefinition>> ORDERED = new ArrayList<>();

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
                BuildASpell.LOGGER.error("Failed to read spell combo: {}", fullId, e);
            }
        }
        return result;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, ComboDefinition> parsed = new HashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
            ResourceLocation id = entry.getKey();
            ComboDefinition.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                    .resultOrPartial(err -> BuildASpell.LOGGER.error("Invalid spell combo {}: {}", id, err))
                    .ifPresent(def -> parsed.put(id, def));
        }
        ORDERED.clear();
        parsed.entrySet().stream()
                .sorted(Map.Entry.comparingByKey((a, b) -> a.toString().compareTo(b.toString())))
                .forEach(ORDERED::add);
        BuildASpell.LOGGER.info("Loaded {} spell combo definitions", ORDERED.size());
    }

    /** First matching combo, or null. Falls back to the built-in enum when the registry is empty (pre-sync). */
    @Nullable
    public static ComboDefinition detect(Spell spell) {
        if (!ORDERED.isEmpty()) {
            for (Map.Entry<ResourceLocation, ComboDefinition> entry : ORDERED) {
                if (entry.getValue().matches(spell)) return entry.getValue();
            }
            return null;
        }
        SpellCombo builtin = SpellCombo.detect(spell);
        return builtin != null ? ComboDefinition.fromBuiltin(builtin) : null;
    }
}
