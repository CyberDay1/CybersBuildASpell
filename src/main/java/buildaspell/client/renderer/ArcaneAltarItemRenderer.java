package buildaspell.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import buildaspell.client.model.AltarModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Renders the Arcane Altar item (inventory icon, in-hand, dropped, item frame) using the
 * same 3D {@link AltarModel} as the in-world block, so the item matches the placed block
 * instead of the old static JSON model. Positioning per display context is driven by the
 * {@code display} block in {@code models/item/arcane_altar.json} (builtin/entity).
 */
public class ArcaneAltarItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("buildaspell", "textures/entity/arcane_altar.png");
    private static final RenderType RENDER_TYPE = RenderType.entityTranslucentEmissive(TEXTURE);

    // Bedrock units to blocks
    private static final float SCALE = 1.0f / 16.0f;

    private static AltarModel model;

    public ArcaneAltarItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (model == null) {
            model = new AltarModel();
        }
        if (!model.isLoaded()) {
            return;
        }

        // Wall-clock time so the runes still spin/bob while shown in the inventory.
        float timeSeconds = System.nanoTime() / 1_000_000_000.0f;

        VertexConsumer consumer = bufferSource.getBuffer(RENDER_TYPE);

        poseStack.pushPose();
        // Centre the model in the unit cube and scale from Bedrock units to blocks,
        // matching ArcaneAltarBlockEntityRenderer.
        poseStack.translate(0.5f, 0.0f, 0.5f);
        poseStack.scale(SCALE, SCALE, SCALE);

        for (int i = 0; i < model.getBoneCount(); i++) {
            if (model.isBoneAnimated(i)) {
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
            } else {
                model.renderBone(consumer, poseStack.last(), i);
            }
        }

        poseStack.popPose();
    }
}
