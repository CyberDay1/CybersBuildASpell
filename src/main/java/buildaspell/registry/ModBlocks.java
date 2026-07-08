package buildaspell.registry;

import buildaspell.BuildASpell;
import buildaspell.block.ArcaneAltarBlock;
import buildaspell.block.SpellLightBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
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

    /**
     * The expiring light placed by the Light effect. Mirrors the vanilla light block's properties so
     * it behaves identically in world, and carries no loot table because it is never dropped.
     */
    public static final DeferredHolder<Block, SpellLightBlock> SPELL_LIGHT = BLOCKS.register("spell_light",
            () -> new SpellLightBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(BuildASpell.MOD_ID, "spell_light")))
                    .replaceable()
                    .strength(-1.0f, 3600000.8f)
                    .mapColor(MapColor.NONE)
                    .noLootTable()
                    .noOcclusion()
                    .lightLevel(LightBlock.LIGHT_EMISSION)));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
