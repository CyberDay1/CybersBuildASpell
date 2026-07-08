package buildaspell.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import buildaspell.client.model.BlackHoleModel;
import buildaspell.entity.BlackHoleEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class BlackHoleRenderer extends EntityRenderer<BlackHoleEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("buildaspell", "textures/entity/black_hole.png");
    private static final RenderType RENDER_TYPE = RenderType.entityTranslucentEmissive(TEXTURE);

    // Animation: 360 degrees over 2 seconds = 180 deg/sec = 9 deg/tick
    private static final float SPIN_SPEED = 180.0f;

    // Visual scale: model radius maps to this fraction of the AoE range
    private static final float VISUAL_FRACTION = 0.195f;

    private static BlackHoleModel model;

    public BlackHoleRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(BlackHoleEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(BlackHoleEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (model == null) {
            model = new BlackHoleModel();
        }
        if (!model.isLoaded()) {
            super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
            return;
        }

        poseStack.pushPose();

        // Scale based on AoE range
        float scale = entity.getRange() * VISUAL_FRACTION / model.getModelRadius();
        poseStack.scale(scale, scale, scale);

        // Spin animation: continuous Y rotation
        float time = entity.tickCount + partialTick;
        poseStack.mulPose(Axis.YP.rotationDegrees(time * SPIN_SPEED));

        VertexConsumer consumer = buffer.getBuffer(RENDER_TYPE);
        model.render(consumer, poseStack.last());

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }
}
