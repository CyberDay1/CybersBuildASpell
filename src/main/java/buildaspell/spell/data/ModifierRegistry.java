package buildaspell.spell.data;

import buildaspell.BuildASpell;
import buildaspell.spell.SpellModifier;
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
 * Loads spell modifier definitions from {@code data/<namespace>/spell_modifiers/*.json}.
 * Server-authoritative; the numeric scalars feed {@link buildaspell.spell.Spell} stat
 * resolution. Falls back to the existing config values when a definition is absent
 * (e.g. client-side before registry sync), preserving current behavior exactly.
 */
public class ModifierRegistry extends SimplePreparableReloadListener<Map<Identifier, JsonElement>> {

    private static final String DIRECTORY = "spell_modifiers";
    private static final Map<Identifier, ModifierDefinition> DEFS = new HashMap<>();

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
                BuildASpell.LOGGER.error("Failed to read spell modifier: {}", fullId, e);
            }
        }
        return result;
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> objects, ResourceManager resourceManager, ProfilerFiller profiler) {
        DEFS.clear();
        for (Map.Entry<Identifier, JsonElement> entry : objects.entrySet()) {
            Identifier id = entry.getKey();
            ModifierDefinition.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                    .resultOrPartial(err -> BuildASpell.LOGGER.error("Invalid spell modifier {}: {}", id, err))
                    .ifPresent(def -> DEFS.put(id, def));
        }
        BuildASpell.LOGGER.info("Loaded {} spell modifier definitions", DEFS.size());
    }

    @Nullable
    public static ModifierDefinition get(SpellModifier modifier) {
        return DEFS.get(Identifier.fromNamespaceAndPath(BuildASpell.MOD_ID, modifier.getSerializedName()));
    }

    /** Registry scalar for a modifier, or {@code fallback} when no definition supplies it. */
    public static double param(SpellModifier modifier, String name, double fallback) {
        ModifierDefinition def = get(modifier);
        return def != null ? def.param(name, fallback) : fallback;
    }
}
