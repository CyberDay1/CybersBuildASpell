package buildaspell.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import buildaspell.entity.RuneMarkerEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class RuneMarkerRenderer extends EntityRenderer<RuneMarkerEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("buildaspell", "textures/entity/rune_marker.png");
    private static final RenderType RENDER_TYPE = RenderType.entityTranslucentEmissive(TEXTURE);
    private static final int RING_SEGMENTS = 24;

    public RuneMarkerRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(RuneMarkerEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(RuneMarkerEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        float time = entity.tickCount + partialTick;
        float charge = entity.getChargeProgress();
        boolean trap = entity.isTrap();

        VertexConsumer consumer = buffer.getBuffer(RENDER_TYPE);

        // Align to surface direction
        alignToSurface(poseStack, entity.getSurfaceDirection());

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
        float outerRadius = radius * charge;
        renderRing(consumer, poseStack.last(), outerRadius, 0.05f, ringR, ringG, ringB, alpha);

        // Inner charge circle
        if (charge > 0.1f) {
            float innerRadius = radius * charge * 0.6f;
            int innerAlpha = (int) (charge * 120);
            int innerR = trap ? 255 : 120;
            int innerG = trap ? 40 : 220;
            int innerB = trap ? 40 : 255;
            renderFilledCircle(consumer, poseStack.last(), innerRadius, innerR, innerG, innerB, innerAlpha);
        }

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
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
