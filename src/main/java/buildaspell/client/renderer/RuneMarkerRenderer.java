package buildaspell.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import buildaspell.client.renderer.state.RuneMarkerRenderState;
import buildaspell.entity.RuneMarkerEntity;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;

public class RuneMarkerRenderer extends EntityRenderer<RuneMarkerEntity, RuneMarkerRenderState> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("buildaspell", "textures/entity/rune_marker.png");
    private static final RenderType RENDER_TYPE = RenderTypes.entityTranslucentEmissive(TEXTURE);
    private static final int RING_SEGMENTS = 24;

    public RuneMarkerRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public RuneMarkerRenderState createRenderState() {
        return new RuneMarkerRenderState();
    }

    @Override
    public void extractRenderState(RuneMarkerEntity entity, RuneMarkerRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.chargeProgress = entity.getChargeProgress();
        state.lifetime = entity.tickCount;
        state.surfaceDirection = entity.getSurfaceDirection();
        state.isTrap = entity.isTrap();
    }

    @Override
    public void submit(RuneMarkerRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();

        float time = (state.lifetime + state.partialTick);
        float charge = state.chargeProgress;
        boolean trap = state.isTrap;

        // Align to surface direction
        alignToSurface(poseStack, state.surfaceDirection);

        // Slight offset from surface
        poseStack.translate(0.0f, 0.02f, 0.0f);

        // Rotating glow ring
        poseStack.mulPose(Axis.YP.rotationDegrees(time * 5.0f));

        float radius = 0.5f;

        // Outer ring - grows with charge. Traps glow red; normal runes glow cyan/blue. The two hues are
        // deliberately on opposite sides of the wheel so a rune and a trap are never mistaken for each other.
        int alpha = (int) (100 + charge * 155);
        int ringR = trap ? 255 : 40;
        int ringG = trap ? (int) (20 + charge * 40) : (int) (180 + charge * 75);
        int ringB = trap ? 30 : 255;
        final float outerRadius = radius * charge;
        final int finalAlpha = alpha;
        final int finalRed = ringR;
        final int finalGreen = ringG;
        final int finalBlue = ringB;
        collector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, consumer) -> {
            renderRing(consumer, pose, outerRadius, 0.05f, finalRed, finalGreen, finalBlue, finalAlpha);
        });

        // Inner charge circle
        if (charge > 0.1f) {
            float innerRadius = radius * charge * 0.6f;
            final float finalInnerRadius = innerRadius;
            final int innerAlpha = (int) (charge * 120);
            final int innerR = trap ? 255 : 120;
            final int innerGreen = trap ? 40 : 220;
            final int innerBlue = trap ? 40 : 255;
            collector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, consumer) -> {
                renderFilledCircle(consumer, pose, finalInnerRadius, innerR, innerGreen, innerBlue, innerAlpha);
            });
        }

        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    private void alignToSurface(PoseStack poseStack, Direction direction) {
        switch (direction) {
            case UP -> {} // Default orientation
            case DOWN -> poseStack.mulPose(Axis.XP.rotationDegrees(180.0f));
            case NORTH -> poseStack.mulPose(Axis.XP.rotationDegrees(-90.0f));
            case SOUTH -> poseStack.mulPose(Axis.XP.rotationDegrees(90.0f));
            case EAST -> poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0f));
            case WEST -> poseStack.mulPose(Axis.ZP.rotationDegrees(90.0f));
        }
    }

    private static void renderRing(VertexConsumer consumer, PoseStack.Pose pose,
                                    float radius, float thickness, int r, int g, int b, int a) {
        if (radius < 0.01f) return;
        for (int i = 0; i < RING_SEGMENTS; i++) {
            float angle1 = 2.0f * (float) Math.PI * i / RING_SEGMENTS;
            float angle2 = 2.0f * (float) Math.PI * (i + 1) / RING_SEGMENTS;

            float x1i = (radius - thickness) * Mth.cos(angle1);
            float z1i = (radius - thickness) * Mth.sin(angle1);
            float x1o = (radius + thickness) * Mth.cos(angle1);
            float z1o = (radius + thickness) * Mth.sin(angle1);
            float x2i = (radius - thickness) * Mth.cos(angle2);
            float z2i = (radius - thickness) * Mth.sin(angle2);
            float x2o = (radius + thickness) * Mth.cos(angle2);
            float z2o = (radius + thickness) * Mth.sin(angle2);

            consumer.addVertex(pose, x1i, 0, z1i).setColor(r, g, b, a)
                    .setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(pose, 0, 1, 0);
            consumer.addVertex(pose, x1o, 0, z1o).setColor(r, g, b, a)
                    .setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(pose, 0, 1, 0);
            consumer.addVertex(pose, x2o, 0, z2o).setColor(r, g, b, a)
                    .setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(pose, 0, 1, 0);
            consumer.addVertex(pose, x2i, 0, z2i).setColor(r, g, b, a)
                    .setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(pose, 0, 1, 0);
        }
    }

    private static void renderFilledCircle(VertexConsumer consumer, PoseStack.Pose pose,
                                            float radius, int r, int g, int b, int a) {
        for (int i = 0; i < RING_SEGMENTS; i++) {
            float angle1 = 2.0f * (float) Math.PI * i / RING_SEGMENTS;
            float angle2 = 2.0f * (float) Math.PI * (i + 1) / RING_SEGMENTS;

            consumer.addVertex(pose, 0, 0, 0).setColor(r, g, b, a)
                    .setUv(0.5f, 0.5f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(pose, 0, 1, 0);
            consumer.addVertex(pose, radius * Mth.cos(angle1), 0, radius * Mth.sin(angle1)).setColor(r, g, b, a / 2)
                    .setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(pose, 0, 1, 0);
            consumer.addVertex(pose, radius * Mth.cos(angle2), 0, radius * Mth.sin(angle2)).setColor(r, g, b, a / 2)
                    .setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(pose, 0, 1, 0);
            // Fourth vertex for quad (degenerate - same as third)
            consumer.addVertex(pose, radius * Mth.cos(angle2), 0, radius * Mth.sin(angle2)).setColor(r, g, b, a / 2)
                    .setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(pose, 0, 1, 0);
        }
    }
}
