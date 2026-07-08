package buildaspell.client;

import buildaspell.BuildASpell;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ConfigureMainRenderTargetEvent;

/**
 * Client-only event handlers for mod bus events like render target configuration.
 */
@EventBusSubscriber(modid = BuildASpell.MOD_ID, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void onConfigureMainRenderTarget(ConfigureMainRenderTargetEvent event) {
        // Enable stencil buffer on the main framebuffer for portal rendering
        event.enableStencil();
    }
}
