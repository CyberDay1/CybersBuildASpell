package buildaspell.spell.data;

import buildaspell.BuildASpell;
import buildaspell.spell.SpellEffect;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.Nullable;

import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads spell effect definitions from {@code data/<namespace>/spell_effects/*.json}.
 * Datapacks override a built-in by shipping the same id (e.g. {@code buildaspell:damage})
 * or add new effects under their own namespace. Behaviour is resolved at cast time via
 * {@link #get(SpellEffect)} (built-ins) and, later, {@link #get(Identifier)}.
 */
public class EffectRegistry extends SimplePreparableReloadListener<Map<Identifier, JsonElement>> {

    private static final String DIRECTORY = "spell_effects";
    private static final Map<Identifier, EffectDefinition> DEFS = new HashMap<>();

    @Override
    protected Map<Identifier, JsonElement> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<Identifier, JsonElement> result = new HashMap<>();
        Map<Identifier, Resource> resources = resourceManager.listResources(DIRECTORY,
                id -> id.getPath().endsWith(".json"));

        for (Map.Entry<Identifier, Resource> entry : resources.entrySet()) {
            Identifier fullId = entry.getKey();
            String path = fullId.getPath();
            String stripped = path.substring(DIRECTORY.length() + 1, path.length() - 5);
            Identifier resourceId = Identifier.fromNamespaceAndPath(fullId.getNamespace(), stripped);

            try (var reader = new InputStreamReader(entry.getValue().open())) {
                result.put(resourceId, JsonParser.parseReader(reader));
            } catch (Exception e) {
                BuildASpell.LOGGER.error("Failed to read spell effect: {}", fullId, e);
            }
        }
        return result;
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> objects, ResourceManager resourceManager, ProfilerFiller profiler) {
        DEFS.clear();
        for (Map.Entry<Identifier, JsonElement> entry : objects.entrySet()) {
            Identifier id = entry.getKey();
            EffectDefinition.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                    .resultOrPartial(err -> BuildASpell.LOGGER.error("Invalid spell effect {}: {}", id, err))
                    .ifPresent(def -> DEFS.put(id, def));
        }
        BuildASpell.LOGGER.info("Loaded {} spell effect definitions", DEFS.size());
    }

    /** Built-in lookup: maps an enum effect to its {@code buildaspell:<id>} definition. */
    @Nullable
    public static EffectDefinition get(SpellEffect effect) {
        return DEFS.get(Identifier.fromNamespaceAndPath(BuildASpell.MOD_ID, effect.getSerializedName()));
    }

    @Nullable
    public static EffectDefinition get(Identifier id) {
        return DEFS.get(id);
    }

    /** All loaded effect definitions by id (built-ins + datapack), for sync/enumeration. */
    public static Map<Identifier, EffectDefinition> all() {
        return Map.copyOf(DEFS);
    }
}
