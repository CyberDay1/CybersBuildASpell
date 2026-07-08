package buildaspell.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import buildaspell.entity.DurationAreaEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class DurationAreaRenderer extends EntityRenderer<DurationAreaEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/misc/white.png");

    public DurationAreaRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(DurationAreaEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(DurationAreaEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // Visual is handled entirely by server-side particle spawning in DurationAreaEntity.
    }
}
