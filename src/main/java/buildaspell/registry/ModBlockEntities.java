package buildaspell.registry;

import buildaspell.BuildASpell;
import buildaspell.block.entity.ArcaneAltarBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, BuildASpell.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ArcaneAltarBlockEntity>> ARCANE_ALTAR =
            BLOCK_ENTITY_TYPES.register("arcane_altar",
                    () -> new BlockEntityType<>(ArcaneAltarBlockEntity::new, ModBlocks.ARCANE_ALTAR.get()));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITY_TYPES.register(eventBus);
    }
}
