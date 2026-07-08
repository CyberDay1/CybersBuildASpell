package buildaspell.enchanting;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import buildaspell.BuildASpell;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class EnchantmentCostManager extends SimplePreparableReloadListener<Map<Identifier, JsonElement>> {
    private static final Map<String, Map<Integer, EnchantmentCost>> COSTS = new HashMap<>();
    private static final String DIRECTORY = "enchantment_costs";

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
                JsonElement json = JsonParser.parseReader(reader);
                result.put(resourceId, json);
            } catch (Exception e) {
                BuildASpell.LOGGER.error("Failed to read enchantment cost: {}", fullId, e);
            }
        }

        return result;
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> objects, ResourceManager resourceManager, ProfilerFiller profiler) {
        COSTS.clear();

        for (Map.Entry<Identifier, JsonElement> entry : objects.entrySet()) {
            try {
                Identifier id = entry.getKey();
                JsonElement json = entry.getValue();

                Map<Integer, EnchantmentCost> levelCosts = parseCostData(json);
                COSTS.put(id.toString(), levelCosts);

                BuildASpell.LOGGER.info("Loaded enchantment costs for: {}", id);
            } catch (Exception e) {
                BuildASpell.LOGGER.error("Failed to load enchantment cost: {}", entry.getKey(), e);
            }
        }

        BuildASpell.LOGGER.info("Loaded {} enchantment cost definitions", COSTS.size());
    }

    private Map<Integer, EnchantmentCost> parseCostData(JsonElement json) {
        Map<Integer, EnchantmentCost> costs = new HashMap<>();

        if (!json.isJsonObject()) {
            return costs;
        }

        var obj = json.getAsJsonObject();
        if (!obj.has("costs") || !obj.get("costs").isJsonArray()) {
            return costs;
        }

        var costsArray = obj.getAsJsonArray("costs");
        for (JsonElement costElement : costsArray) {
            if (!costElement.isJsonObject()) continue;

            var costObj = costElement.getAsJsonObject();
            int level = costObj.get("level").getAsInt();
            int xpLevels = costObj.get("xp_levels").getAsInt();
            String itemId = costObj.get("item").getAsString();
            int itemCount = costObj.get("item_count").getAsInt();

            Identifier itemLocation = Identifier.parse(itemId);
            Item item = BuiltInRegistries.ITEM.getValue(itemLocation);

            if (item == Items.AIR) {
                BuildASpell.LOGGER.warn("Unknown item in cost: {}", itemId);
                continue;
            }

            costs.put(level, new EnchantmentCost(xpLevels, item, itemCount));
        }

        return costs;
    }

    /** XP points charged for a single enchant level (scales linearly with the level). */
    public static final int XP_POINTS_PER_LEVEL = 10;

    /** The full bill for climbing from {@code fromLevel} (exclusive) to {@code toLevel} (inclusive). */
    public record CumulativeCost(int xpPoints, Map<Item, Integer> items) {
        public boolean isEmpty() {
            return xpPoints == 0 && items.isEmpty();
        }
    }

    /**
     * Sums the per-level material + XP cost for every level from {@code fromLevel + 1} up to
     * {@code toLevel}, so a player can't cheaply skip straight to max — reaching level N costs
     * everything along the way. Materials are tallied by item type.
     */
    public static CumulativeCost getCumulativeCost(String enchantmentId, int fromLevel, int toLevel) {
        Map<Item, Integer> items = new LinkedHashMap<>();
        int xpPoints = 0;
        for (int level = fromLevel + 1; level <= toLevel; level++) {
            EnchantmentCost cost = getCost(enchantmentId, level);
            if (cost.itemCount() > 0) {
                items.merge(cost.item(), cost.itemCount(), Integer::sum);
            }
            xpPoints += level * XP_POINTS_PER_LEVEL;
        }
        return new CumulativeCost(xpPoints, items);
    }

    /** A player's current total experience in points (levels + partial progress). */
    public static int totalXpPoints(Player player) {
        return xpAtLevelStart(player.experienceLevel)
                + Math.round(player.experienceProgress * player.getXpNeededForNextLevel());
    }

    /** Total XP points required to have reached the start of the given level (vanilla curve). */
    public static int xpAtLevelStart(int level) {
        if (level <= 16) {
            return level * level + 6 * level;
        } else if (level <= 31) {
            return (int) (2.5 * level * level - 40.5 * level + 360.0);
        } else {
            return (int) (4.5 * level * level - 162.5 * level + 2220.0);
        }
    }

    public static EnchantmentCost getCost(String enchantmentId, int level) {
        Map<Integer, EnchantmentCost> levelCosts = COSTS.get(enchantmentId);
        if (levelCosts == null) {
            return getDefaultCost(level);
        }

        EnchantmentCost cost = levelCosts.get(level);
        return cost != null ? cost : getDefaultCost(level);
    }

    private static EnchantmentCost getDefaultCost(int level) {
        Item item;
        int baseCount;
        int xpLevels = level * 5;

        if (level <= 5) {
            item = Items.IRON_INGOT;
            baseCount = level;
        } else if (level <= 10) {
            item = Items.GOLD_INGOT;
            baseCount = (level - 5) * 2;
        } else if (level <= 15) {
            item = Items.DIAMOND;
            baseCount = (level - 10) * 3;
        } else {
            item = Items.NETHERITE_INGOT;
            baseCount = (level - 15);
        }

        return new EnchantmentCost(xpLevels, item, baseCount);
    }
}
