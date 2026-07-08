package buildaspell.registry;

import buildaspell.BuildASpell;
import buildaspell.item.BlankRuneItem;
import buildaspell.item.SpellRuneItem;
import buildaspell.item.WandItem;
import buildaspell.item.WandTier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, BuildASpell.MOD_ID);

    private static ResourceKey<Item> key(String name) {
        return ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(BuildASpell.MOD_ID, name));
    }

    public static final DeferredHolder<Item, BlockItem> ARCANE_ALTAR = ITEMS.register("arcane_altar",
            () -> new BlockItem(ModBlocks.ARCANE_ALTAR.get(), new Item.Properties().setId(key("arcane_altar"))));

    public static final DeferredHolder<Item, SpellRuneItem> SPELL_RUNE = ITEMS.register("spell_rune",
            () -> new SpellRuneItem(new Item.Properties().setId(key("spell_rune")).rarity(Rarity.UNCOMMON)));

    public static final DeferredHolder<Item, BlankRuneItem> BLANK_RUNE = ITEMS.register("blank_rune",
            () -> new BlankRuneItem(new Item.Properties().setId(key("blank_rune")).rarity(Rarity.UNCOMMON)));

    public static final DeferredHolder<Item, WandItem> WORN_WAND = ITEMS.register("worn_wand",
            () -> new WandItem(WandTier.WORN, new Item.Properties().setId(key("worn_wand")).stacksTo(1)));

    public static final DeferredHolder<Item, WandItem> CARVED_WAND = ITEMS.register("carved_wand",
            () -> new WandItem(WandTier.CARVED, new Item.Properties().setId(key("carved_wand")).stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static final DeferredHolder<Item, WandItem> RUNIC_WAND = ITEMS.register("runic_wand",
            () -> new WandItem(WandTier.RUNIC, new Item.Properties().setId(key("runic_wand")).stacksTo(1).rarity(Rarity.EPIC)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
