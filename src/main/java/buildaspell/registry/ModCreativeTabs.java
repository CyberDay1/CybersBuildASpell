package buildaspell.registry;

import buildaspell.BuildASpell;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

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
}
