package buildaspell.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import buildaspell.client.model.TornadoModel;
import buildaspell.entity.TornadoEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class TornadoEntityRenderer extends EntityRenderer<TornadoEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("buildaspell", "textures/entity/tornado.png");
    // Depth-writing translucent (NOT the emissive variant): entityTranslucentEmissive writes color only,
    // so the vanilla water pass — which renders after entities — blends its blue over the funnel and washes
    // it out anywhere the tornado overlaps an ocean. Writing depth makes water depth-fail behind the funnel
    // instead, so it stays visible over water.
    private static final RenderType RENDER_TYPE = RenderType.entityTranslucent(TEXTURE);

    // Visual scale: model radius maps to this fraction of the AoE range
    private static final float VISUAL_FRACTION = 0.45f;

    // Slow the funnel spin down from the raw animation rate (testers found it too fast/dizzying).
    private static final float SPIN_SPEED = 0.5f;

    private static TornadoModel model;

    public TornadoEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(TornadoEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(TornadoEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (model == null) {
            model = new TornadoModel();
        }
        if (!model.isLoaded()) {
            super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
            return;
        }

        float timeSeconds = (entity.getRenderAge() + partialTick) / 20.0f * SPIN_SPEED;
        float range = entity.getRange();
        float scale = range * VISUAL_FRACTION / model.getModelRadius();

        VertexConsumer consumer = buffer.getBuffer(RENDER_TYPE);

        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);

        // Render each bone with its animation transforms
        for (int i = 0; i < model.getBoneCount(); i++) {
            float rotation = model.getBoneRotation(i, timeSeconds);
            float bobY = model.getBoneBobOffset(i, timeSeconds);

            poseStack.pushPose();
            if (bobY != 0) {
                poseStack.translate(0.0f, bobY, 0.0f);
            }
            if (rotation != 0) {
                poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
            }

            model.renderBone(consumer, poseStack.last(), i);

            poseStack.popPose();
        }

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }
}
