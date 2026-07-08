package buildaspell.client;

import buildaspell.BuildASpell;
import buildaspell.client.gui.ArcaneAltarScreen;
import buildaspell.client.gui.ManaBarOverlay;
import buildaspell.client.renderer.*;
import buildaspell.registry.ModBlockEntities;
import buildaspell.registry.ModEntities;
import buildaspell.registry.ModItems;
import buildaspell.registry.ModMenuTypes;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

public class ClientSetup {

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // Entity renderers with visual representation
        event.registerEntityRenderer(ModEntities.SPELL_PROJECTILE.get(), SpellProjectileRenderer::new);
        event.registerEntityRenderer(ModEntities.BLACK_HOLE.get(), BlackHoleRenderer::new);
        event.registerEntityRenderer(ModEntities.TORNADO.get(), TornadoEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.PORTAL.get(), PortalEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.DURATION_AREA.get(), DurationAreaRenderer::new);
        event.registerEntityRenderer(ModEntities.RUNE_MARKER.get(), RuneMarkerRenderer::new);

        // Invisible entities (no-op renderers)
        event.registerEntityRenderer(ModEntities.DELAYED_SPELL.get(), EmptyEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.FLIGHT_DURATION.get(), EmptyEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.FORTRESS_BARRIER.get(), EmptyEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.BLIZZARD.get(), EmptyEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.STORM_CLOUD.get(), EmptyEntityRenderer::new);

        // Block entity renderers
        event.registerBlockEntityRenderer(ModBlockEntities.ARCANE_ALTAR.get(), ArcaneAltarBlockEntityRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        // Render the Arcane Altar item with the 3D geo model (icon, in-hand, dropped, frames).
        event.registerItem(new IClientItemExtensions() {
            private ArcaneAltarItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new ArcaneAltarItemRenderer();
                }
                return renderer;
            }
        }, ModItems.ARCANE_ALTAR.get());
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        for (net.minecraft.client.KeyMapping key : ModKeyBinds.ALL_KEYS) {
            event.register(key);
        }
    }

    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.ARCANE_ALTAR.get(), ArcaneAltarScreen::new);
    }

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.EXPERIENCE_LEVEL,
                ResourceLocation.fromNamespaceAndPath(BuildASpell.MOD_ID, "mana_bar"),
                new ManaBarOverlay());
    }

}
