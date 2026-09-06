package buildaspell.registry;

import buildaspell.BuildASpell;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BuildASpell.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CYBERS_BUILD_A_SPELL_TAB =
            CREATIVE_TABS.register("buildaspell_tab",
                    () -> CreativeModeTab.builder()
                            .title(Component.translatable("itemGroup." + BuildASpell.MOD_ID))
                            .icon(() -> new ItemStack(ModItems.SPELL_RUNE.get()))
                            .displayItems((params, output) -> {
                                output.accept(ModItems.SPELL_RUNE.get());
                                output.accept(ModItems.BLANK_RUNE.get());
                                output.accept(ModItems.ARCANE_ALTAR.get());
                                output.accept(ModItems.WORN_WAND.get());
                                output.accept(ModItems.CARVED_WAND.get());
                                output.accept(ModItems.RUNIC_WAND.get());
                            })
                            .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_TABS.register(eventBus);
    }

    /**
     * Trims our enchanted books out of the Ingredients tab's search listing.
     *
     * <p>Vanilla files one book per level of every enchantment under the search tab, and one book at
     * the top level under the tab proper. That is four extra entries for Sharpness. Ours are capped
     * at 255 so the Arcane Altar always has somewhere left to climb, which turned into 255 books
     * apiece — a wall of them in the creative search and in any recipe viewer reading it. Keep the
     * top-level book vanilla already shows in the tab and drop the rest, so each of our enchantments
     * appears exactly once, the same as every other enchantment does in the tab itself.
     */
    public static void onBuildCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (!CreativeModeTabs.INGREDIENTS.equals(event.getTabKey())) {
            return;
        }
        List<ItemStack> redundant = new ArrayList<>();
        for (ItemStack entry : event.getSearchEntries()) {
            if (isBelowTopLevelSpellBook(entry)) {
                redundant.add(entry);
            }
        }
        for (ItemStack book : redundant) {
            event.remove(book, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        }
    }

    /**
     * True for a book holding one of our enchantments at anything less than its top level. Judged by
     * namespace rather than by a list of ids, so an enchantment added later is covered without
     * anyone having to remember this. A book carrying anything that isn't ours is left alone.
     */
    private static boolean isBelowTopLevelSpellBook(ItemStack stack) {
        ItemEnchantments stored = stack.get(DataComponents.STORED_ENCHANTMENTS);
        if (stored == null || stored.isEmpty()) {
            return false;
        }
        boolean below = false;
        for (Holder<Enchantment> enchantment : stored.keySet()) {
            boolean ours = enchantment.unwrapKey()
                    .map(key -> key.location().getNamespace().equals(BuildASpell.MOD_ID))
                    .orElse(false);
            if (!ours) {
                return false;
            }
            if (stored.getLevel(enchantment) < enchantment.value().getMaxLevel()) {
                below = true;
            }
        }
        return below;
    }
}
