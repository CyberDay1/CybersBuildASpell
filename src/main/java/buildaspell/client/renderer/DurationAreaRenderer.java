package buildaspell.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import buildaspell.client.renderer.state.DurationAreaRenderState;
import buildaspell.entity.DurationAreaEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.neoforged.api.distmarker.Dist;

public class DurationAreaRenderer extends EntityRenderer<DurationAreaEntity, DurationAreaRenderState> {

    public DurationAreaRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public DurationAreaRenderState createRenderState() {
        return new DurationAreaRenderState();
    }

    @Override
    public void extractRenderState(DurationAreaEntity entity, DurationAreaRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.range = entity.getRange();
    }

    @Override
    public void submit(DurationAreaRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        // Visual is handled entirely by server-side particle spawning in DurationAreaEntity.
        super.submit(state, poseStack, collector, camera);
    }
}
