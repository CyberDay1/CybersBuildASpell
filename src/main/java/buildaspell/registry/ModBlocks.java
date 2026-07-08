package buildaspell.registry;

import buildaspell.BuildASpell;
import buildaspell.block.ArcaneAltarBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, BuildASpell.MOD_ID);

    public static final DeferredHolder<Block, ArcaneAltarBlock> ARCANE_ALTAR = BLOCKS.register("arcane_altar",
            () -> new ArcaneAltarBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(BuildASpell.MOD_ID, "arcane_altar")))
                    .strength(5.0f, 1200.0f)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
