package buildaspell.client;

import buildaspell.BuildASpell;
import buildaspell.client.gui.ManaBarOverlay;
import buildaspell.client.gui.SpellBuilderScreen;
import buildaspell.entity.PortalEntity;
import buildaspell.spell.MarkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;

import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;


import java.util.List;

@EventBusSubscriber(modid = BuildASpell.MOD_ID, value = Dist.CLIENT)
public class ClientEvents {

    private static int tickCount = 0;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        KeyInputHandler.handleKeyInput();
        tickCount++;
        // Spawn portal particles at ~10Hz (every 2 ticks)
        if (tickCount % 2 == 0) {
            spawnPortalParticles();
        }
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        // Clear client-side caches on disconnect
        MarkManager.clearAll();
        SpellBuilderScreen.clearStaticState();
        ManaBarOverlay.reset();
    }

    private static void spawnPortalParticles() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused()) return;

        Level level = mc.level;
        AABB searchBox = mc.player.getBoundingBox().inflate(32.0);
        List<PortalEntity> portals = level.getEntitiesOfClass(PortalEntity.class, searchBox);

        for (PortalEntity portal : portals) {
            // Skip portals where NeoPortals handles the visual
            if (portal.hasNeoPortals()) continue;
            spawnParticlesFor(level, portal);
        }
    }

    private static void spawnParticlesFor(Level level, PortalEntity portal) {
        Vec3 pos    = portal.position();
        Vec3 axisW  = portal.getAxisW();
        Vec3 axisH  = portal.getAxisH();
        Vec3 normal = portal.getNormal();
        float halfWidth  = (float)(portal.getPortalWidth()  * 0.5);
        float halfHeight = (float)(portal.getPortalHeight() * 0.5);

        // Arcane teal when dialed, pink when undialed
        int color = portal.isDialed()
                ? 0xFF26D9FF   // teal (r=0.15, g=0.85, b=1.0)
                : 0xFFFF4DB3;  // pink (r=1.0, g=0.30, b=0.70)
        DustParticleOptions particle = new DustParticleOptions(color, 0.9f);

        var random = level.getRandom();
        int count = 1 + random.nextInt(3); // 1–3 particles per call
        for (int i = 0; i < count; i++) {
            // Random position in the portal plane
            float u = (random.nextFloat() * 2 - 1) * halfWidth;
            float v = (random.nextFloat() * 2 - 1) * halfHeight;
            double px = pos.x + u * axisW.x + v * axisH.x;
            double py = pos.y + u * axisW.y + v * axisH.y;
            double pz = pos.z + u * axisW.z + v * axisH.z;

            // Drift outward from the portal surface in both directions
            double side   = random.nextBoolean() ? 1.0 : -1.0;
            double speed  = 0.04 + random.nextDouble() * 0.06;
            double spread = 0.015;
            double vx = normal.x * speed * side + (random.nextDouble() - 0.5) * spread;
            double vy = normal.y * speed * side + (random.nextDouble() - 0.5) * spread;
            double vz = normal.z * speed * side + (random.nextDouble() - 0.5) * spread;

            level.addParticle(particle, px, py, pz, vx, vy, vz);
        }
    }
}
