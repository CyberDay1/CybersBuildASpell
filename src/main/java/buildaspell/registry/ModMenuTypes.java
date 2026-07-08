package buildaspell.registry;

import buildaspell.BuildASpell;
import buildaspell.menu.ArcaneAltarMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, BuildASpell.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<ArcaneAltarMenu>> ARCANE_ALTAR =
            MENU_TYPES.register("arcane_altar",
                    () -> IMenuTypeExtension.create(ArcaneAltarMenu::new));

    public static void register(IEventBus eventBus) {
        MENU_TYPES.register(eventBus);
    }
}
