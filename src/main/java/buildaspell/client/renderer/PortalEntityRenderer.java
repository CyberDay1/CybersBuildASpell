package buildaspell.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import buildaspell.client.renderer.state.PortalRenderState;
import buildaspell.entity.PortalEntity;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;

public class PortalEntityRenderer extends EntityRenderer<PortalEntity, PortalRenderState> {

    // Reuse the vanilla nether portal texture — it tiles seamlessly and reads as "portal"
    private static final Identifier TEXTURE_PORTAL =
            Identifier.fromNamespaceAndPath("minecraft", "textures/block/nether_portal.png");
    private static final RenderType RENDER_TYPE_PORTAL =
            RenderTypes.entityTranslucentEmissive(TEXTURE_PORTAL);

    public PortalEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public PortalRenderState createRenderState() {
        return new PortalRenderState();
    }

    @Override
    public void extractRenderState(PortalEntity entity, PortalRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.portalWidth = entity.getPortalWidth();
        state.portalHeight = entity.getPortalHeight();
        state.isDialed = entity.isDialed();
        state.ticksExisted = entity.tickCount;
        state.destDimensionId = entity.getDestDimensionId();
        state.axisW = entity.getAxisW();
        state.axisH = entity.getAxisH();
        state.normal = entity.getNormal();
        state.portalOrigin = entity.position();
        state.portalUUID = entity.getUUID();
        if (entity.getDestinationPos() != null) {
            state.destinationPos = entity.getDestinationPos();
        }
        state.neoPortalsHandlingRender = state.isDialed && entity.hasNeoPortals();
    }

    @Override
    public void submit(PortalRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        // NeoPortals is present and this portal is dialed — NeoPortals renders the visual.
        if (state.neoPortalsHandlingRender) {
            super.submit(state, poseStack, collector, camera);
            return;
        }

        poseStack.pushPose();

        float halfWidth  = state.portalWidth  * 0.5f;
        float halfHeight = state.portalHeight * 0.5f;
        float time = state.ticksExisted + state.partialTick;

        // Anchor the portal's base at the entity position (its feet) rather than centering the
        // quad on it. Centering buried the bottom half underground, so only ~half the height
        // showed — the rift looked short and stubby. This lifts it to stand on the ground.
        poseStack.translate(0, halfHeight, 0);

        Vec3 axisW  = state.axisW;
        Vec3 axisH  = state.axisH;
        Vec3 normal = state.normal;

        // Arcane teal when dialed, pink when undialed
        int r = state.isDialed ? 60  : 255;
        int g = state.isDialed ? 210 : 80;
        int b = state.isDialed ? 255 : 180;

        // UV window scrolls across the tileable texture.
        // Window size = 0.5 so the panning range stays within [0, 1].
        // Layer 1 — forward scroll
        float u1 = (time * 0.006f) % 0.5f;
        float v1 = (time * 0.003f) % 0.5f;
        collector.submitCustomGeometry(poseStack, RENDER_TYPE_PORTAL,
                (pose, consumer) -> renderPortalLayer(
                        consumer, pose, axisW, axisH, normal,
                        halfWidth, halfHeight, u1, v1, r, g, b, 200));

        // Layer 2 — counter-scroll, pushed 0.01 along normal to avoid z-fighting
        float u2 = 0.5f - (time * 0.004f) % 0.5f;
        float v2 = (time * 0.007f) % 0.5f;
        poseStack.translate(normal.x * 0.01, normal.y * 0.01, normal.z * 0.01);
        collector.submitCustomGeometry(poseStack, RENDER_TYPE_PORTAL,
                (pose, consumer) -> renderPortalLayer(
                        consumer, pose, axisW, axisH, normal,
                        halfWidth, halfHeight, u2, v2, r, g, b, 110));

        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    /**
     * Renders a single translucent quad in the portal's plane.
     * UV window: [uOff, uOff+0.5] × [vOff, vOff+0.5] — always within [0,1].
     */
    private static void renderPortalLayer(VertexConsumer consumer, PoseStack.Pose pose,
                                          Vec3 axisW, Vec3 axisH, Vec3 normal,
                                          float halfWidth, float halfHeight,
                                          float uOff, float vOff,
                                          int r, int g, int b, int a) {
        float nx = (float) normal.x, ny = (float) normal.y, nz = (float) normal.z;

        // Four corners of the portal quad in entity-local space
        float blX = (float)(-halfWidth * axisW.x - halfHeight * axisH.x);
        float blY = (float)(-halfWidth * axisW.y - halfHeight * axisH.y);
        float blZ = (float)(-halfWidth * axisW.z - halfHeight * axisH.z);

        float brX = (float)( halfWidth * axisW.x - halfHeight * axisH.x);
        float brY = (float)( halfWidth * axisW.y - halfHeight * axisH.y);
        float brZ = (float)( halfWidth * axisW.z - halfHeight * axisH.z);

        float trX = (float)( halfWidth * axisW.x + halfHeight * axisH.x);
        float trY = (float)( halfWidth * axisW.y + halfHeight * axisH.y);
        float trZ = (float)( halfWidth * axisW.z + halfHeight * axisH.z);

        float tlX = (float)(-halfWidth * axisW.x + halfHeight * axisH.x);
        float tlY = (float)(-halfWidth * axisW.y + halfHeight * axisH.y);
        float tlZ = (float)(-halfWidth * axisW.z + halfHeight * axisH.z);

        float uMax = uOff + 0.5f;
        float vMax = vOff + 0.5f;

        consumer.addVertex(pose, blX, blY, blZ).setColor(r, g, b, a)
                .setUv(uOff, vMax).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0xF000F0).setNormal(pose, nx, ny, nz);
        consumer.addVertex(pose, brX, brY, brZ).setColor(r, g, b, a)
                .setUv(uMax, vMax).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0xF000F0).setNormal(pose, nx, ny, nz);
        consumer.addVertex(pose, trX, trY, trZ).setColor(r, g, b, a)
                .setUv(uMax, vOff).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0xF000F0).setNormal(pose, nx, ny, nz);
        consumer.addVertex(pose, tlX, tlY, tlZ).setColor(r, g, b, a)
                .setUv(uOff, vOff).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0xF000F0).setNormal(pose, nx, ny, nz);
    }
}
