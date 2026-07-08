package buildaspell.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import buildaspell.client.model.BlackHoleModel;
import buildaspell.client.renderer.state.BlackHoleRenderState;
import buildaspell.entity.BlackHoleEntity;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;

public class BlackHoleRenderer extends EntityRenderer<BlackHoleEntity, BlackHoleRenderState> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("buildaspell", "textures/entity/black_hole.png");
    private static final RenderType RENDER_TYPE = RenderTypes.entityTranslucentEmissive(TEXTURE);

    // Animation: 360 degrees over 2 seconds = 180 deg/sec = 9 deg/tick
    private static final float SPIN_SPEED = 180.0f;

    // Visual scale: model radius maps to this fraction of the AoE range
    private static final float VISUAL_FRACTION = 0.195f;

    private static BlackHoleModel model;

    public BlackHoleRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public BlackHoleRenderState createRenderState() {
        return new BlackHoleRenderState();
    }

    @Override
    public void extractRenderState(BlackHoleEntity entity, BlackHoleRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.range = entity.getRange();
        state.lifetime = entity.tickCount;
    }

    @Override
    public void submit(BlackHoleRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (model == null) {
            model = new BlackHoleModel();
        }
        if (!model.isLoaded()) {
            super.submit(state, poseStack, collector, camera);
            return;
        }

        poseStack.pushPose();

        // Scale based on AoE range
        float scale = state.range * VISUAL_FRACTION / model.getModelRadius();
        poseStack.scale(scale, scale, scale);

        // Spin animation: continuous Y rotation
        float time = state.lifetime + state.partialTick;
        poseStack.mulPose(Axis.YP.rotationDegrees(time * SPIN_SPEED));

        collector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, consumer) -> {
            model.render(consumer, pose);
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }
}
