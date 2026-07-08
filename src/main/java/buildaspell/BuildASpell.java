package buildaspell;

import buildaspell.config.ModConfig;
// Modonomicon guidebook datagen disabled on 26.2 until modonomicon-26.2-neoforge ships.
// import buildaspell.datagen.ModDataGenerators;
import buildaspell.events.ServerEvents;
import buildaspell.network.ModPackets;
import buildaspell.registry.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(BuildASpell.MOD_ID)
public class BuildASpell {
    public static final String MOD_ID = "buildaspell";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public BuildASpell(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("BuildASpell initializing for 1.21.11");

        // Config — split across a buildaspell/ folder; SERVER specs auto-sync to clients on login.
        ModConfig.init();
        modContainer.registerConfig(Type.SERVER, ModConfig.getGeneralSpec(), "buildaspell/general.toml");
        modContainer.registerConfig(Type.SERVER, ModConfig.getDeliveriesSpec(), "buildaspell/deliveries.toml");
        modContainer.registerConfig(Type.SERVER, ModConfig.getEffectsSpec(), "buildaspell/effects.toml");
        modContainer.registerConfig(Type.SERVER, ModConfig.getModifiersSpec(), "buildaspell/modifiers.toml");
        modContainer.registerConfig(Type.SERVER, ModConfig.getWandsSpec(), "buildaspell/wands.toml");
        modContainer.registerConfig(Type.CLIENT, ModConfig.getClientSpec(), "buildaspell/client.toml");

        // Spell behaviour primitive types (must be registered before any datapack JSON loads)
        buildaspell.spell.data.SpellPrimitives.bootstrap();

        // Registries
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModEntities.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModAttachments.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModLootModifiers.register(modEventBus);
        ModAttributes.register(modEventBus);

        // Mod bus event handlers (these events fire on the mod bus, not the game bus)
        modEventBus.addListener(ModPackets::registerPackets);
        modEventBus.addListener(ModAttributes::onAttributeModification);
        // Retires defaults that an older release already baked into an existing world's config file.
        modEventBus.addListener(ModConfig::onConfigLoad);
        // modEventBus.addListener(ModDataGenerators::onGatherData); // Modonomicon datagen disabled on 26.2
        modEventBus.addListener(buildaspell.portal.PortalChunkLoader::register);

        // Game bus event handlers (these fire on the NeoForge game event bus)
        NeoForge.EVENT_BUS.addListener(ServerEvents::onAddReloadListeners);

        // Client-side mod bus event handlers
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            registerClientModBusEvents(modEventBus);
        }
    }

    private static void registerClientModBusEvents(IEventBus modEventBus) {
        modEventBus.addListener(buildaspell.client.ClientSetup::onRegisterRenderers);
        modEventBus.addListener(buildaspell.client.ClientSetup::onRegisterSpecialModelRenderers);
        modEventBus.addListener(buildaspell.client.ClientSetup::onRegisterKeyMappings);
        modEventBus.addListener(buildaspell.client.ClientSetup::onRegisterMenuScreens);
        modEventBus.addListener(buildaspell.client.ClientSetup::onRegisterGuiLayers);
    }
}
