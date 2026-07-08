package buildaspell.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import buildaspell.entity.SpellProjectileEntity;
import buildaspell.spell.ProjectileShape;
import buildaspell.spell.SpellVisual;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public class SpellProjectileRenderer extends EntityRenderer<SpellProjectileEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("buildaspell", "textures/entity/spell_projectile.png");
    private static final RenderType RENDER_TYPE = RenderType.entityTranslucentEmissive(TEXTURE);

    private static final int[] DEFAULT_COLOR = {150, 200, 255};
    private static final Map<String, int[]> EFFECT_COLORS = Map.ofEntries(
            Map.entry("damage", new int[]{255, 80, 80}),
            Map.entry("ignite", new int[]{255, 160, 50}),
            Map.entry("freeze", new int[]{120, 200, 255}),
            Map.entry("heal", new int[]{100, 255, 120}),
            Map.entry("saturation", new int[]{100, 255, 120}),
            Map.entry("lightning", new int[]{255, 255, 100}),
            Map.entry("poison", new int[]{150, 255, 50}),
            Map.entry("wither", new int[]{180, 50, 220}),
            Map.entry("teleport", new int[]{50, 255, 255}),
            Map.entry("blink", new int[]{50, 255, 255}),
            Map.entry("recall", new int[]{50, 255, 255}),
            Map.entry("push", new int[]{100, 150, 255}),
            Map.entry("pull", new int[]{100, 150, 255}),
            Map.entry("yeet", new int[]{100, 150, 255}),
            Map.entry("launch", new int[]{100, 150, 255}),
            Map.entry("slam", new int[]{100, 150, 255}),
            Map.entry("explosion", new int[]{255, 120, 50}),
            Map.entry("shield", new int[]{255, 215, 80}),
            Map.entry("invisibility", new int[]{220, 220, 240}),
            Map.entry("summon", new int[]{80, 220, 200}),
            Map.entry("break", new int[]{200, 160, 80}),
            Map.entry("conjure", new int[]{200, 160, 80})
    );

    public SpellProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(SpellProjectileEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(SpellProjectileEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // Explicit per-spell color overrides the per-effect default.
        int[] effectColor;
        int packed = entity.getVisualColor();
        if (packed != SpellVisual.COLOR_DEFAULT) {
            effectColor = new int[]{(packed >> 16) & 0xFF, (packed >> 8) & 0xFF, packed & 0xFF};
        } else {
            effectColor = EFFECT_COLORS.getOrDefault(entity.getEffectType(), DEFAULT_COLOR);
        }
        ProjectileShape shape = ProjectileShape.fromId(entity.getVisualShape());
        float time = entity.tickCount + partialTick;

        VertexConsumer consumer = buffer.getBuffer(RENDER_TYPE);

        // --- Inner core layer: fast spin, near-white with slight tint ---
        int coreR = (255 * 7 + effectColor[0] * 3) / 10;
        int coreG = (255 * 7 + effectColor[1] * 3) / 10;
        int coreB = (255 * 7 + effectColor[2] * 3) / 10;

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(time * 20.0f));
        poseStack.mulPose(Axis.XP.rotationDegrees(time * 14.0f));
        poseStack.scale(0.3f, 0.3f, 0.3f);
        drawShape(shape, consumer, poseStack, coreR, coreG, coreB, 255);
        poseStack.popPose();

        // --- Outer glow layer: slower reverse spin, full color, pulsing scale ---
        float pulse = 1.0f + 0.08f * (float) Math.sin(time * 0.15);
        float glowScale = 0.7f * pulse;
        int glowAlpha = 140;

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(time * -8.0f));
        poseStack.mulPose(Axis.XP.rotationDegrees(time * -5.6f));
        poseStack.scale(glowScale, glowScale, glowScale);
        drawShape(shape, consumer, poseStack, effectColor[0], effectColor[1], effectColor[2], glowAlpha);
        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private static void drawShape(ProjectileShape shape, VertexConsumer consumer, PoseStack poseStack,
                                  int r, int g, int b, int a) {
        switch (shape) {
            case CUBE -> drawCube(consumer, poseStack, r, g, b, a);
            case RING -> drawRing(consumer, poseStack, r, g, b, a);
            case SPHERE -> drawSphere(consumer, poseStack, r, g, b, a);
            default -> drawCross(consumer, poseStack, r, g, b, a);
        }
    }

    /** Two crossed billboard quads — the historical look. */
    private static void drawCross(VertexConsumer consumer, PoseStack poseStack, int r, int g, int b, int a) {
        renderQuad(consumer, poseStack.last(), -0.5f, -0.5f, 0.5f, 0.5f, 0.0f, r, g, b, a);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0f));
        renderQuad(consumer, poseStack.last(), -0.5f, -0.5f, 0.5f, 0.5f, 0.0f, r, g, b, a);
    }

    /** Six axis-aligned faces forming a solid-looking cube. */
    private static void drawCube(VertexConsumer consumer, PoseStack poseStack, int r, int g, int b, int a) {
        float s = 0.5f;
        // front / back
        renderQuad(consumer, poseStack.last(), -s, -s, s, s, s, r, g, b, a);
        renderQuad(consumer, poseStack.last(), -s, -s, s, s, -s, r, g, b, a);
        // left / right
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0f));
        renderQuad(consumer, poseStack.last(), -s, -s, s, s, s, r, g, b, a);
        renderQuad(consumer, poseStack.last(), -s, -s, s, s, -s, r, g, b, a);
        // top / bottom
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0f));
        renderQuad(consumer, poseStack.last(), -s, -s, s, s, s, r, g, b, a);
        renderQuad(consumer, poseStack.last(), -s, -s, s, s, -s, r, g, b, a);
    }

    /** Eight thin quads arranged radially to suggest a torus ring. */
    private static void drawRing(VertexConsumer consumer, PoseStack poseStack, int r, int g, int b, int a) {
        int segments = 8;
        float radius = 0.55f;
        float seg = 0.22f;   // half-width of each segment quad
        for (int i = 0; i < segments; i++) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(360.0f / segments * i));
            // push outward along Z, then face tangentially
            renderQuad(consumer, poseStack.last(), -seg, -0.12f, seg, 0.12f, radius, r, g, b, a);
            poseStack.popPose();
        }
    }

    /** Cheap volumetric look: a cluster of crossed billboards at staggered offsets/scales. */
    private static void drawSphere(VertexConsumer consumer, PoseStack poseStack, int r, int g, int b, int a) {
        float[][] offs = {
                {0.0f, 0.0f, 0.0f, 1.0f},
                {0.28f, 0.1f, -0.2f, 0.7f},
                {-0.25f, -0.2f, 0.18f, 0.7f},
                {0.1f, -0.28f, 0.22f, 0.6f},
                {-0.15f, 0.26f, -0.15f, 0.6f},
        };
        for (float[] o : offs) {
            poseStack.pushPose();
            poseStack.translate(o[0], o[1], o[2]);
            poseStack.scale(o[3], o[3], o[3]);
            drawCross(consumer, poseStack, r, g, b, a);
            poseStack.popPose();
        }
    }

    private static void renderQuad(VertexConsumer consumer, PoseStack.Pose pose,
                                    float minX, float minY, float maxX, float maxY, float z,
                                    int r, int g, int b, int a) {
        consumer.addVertex(pose, minX, minY, z)
                .setColor(r, g, b, a)
                .setUv(0.0f, 1.0f)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0xF000F0)
                .setNormal(pose, 0.0f, 1.0f, 0.0f);
        consumer.addVertex(pose, maxX, minY, z)
                .setColor(r, g, b, a)
                .setUv(1.0f, 1.0f)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0xF000F0)
                .setNormal(pose, 0.0f, 1.0f, 0.0f);
        consumer.addVertex(pose, maxX, maxY, z)
                .setColor(r, g, b, a)
                .setUv(1.0f, 0.0f)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0xF000F0)
                .setNormal(pose, 0.0f, 1.0f, 0.0f);
        consumer.addVertex(pose, minX, maxY, z)
                .setColor(r, g, b, a)
                .setUv(0.0f, 0.0f)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0xF000F0)
                .setNormal(pose, 0.0f, 1.0f, 0.0f);
    }
}
