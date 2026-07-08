package buildaspell.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import buildaspell.block.entity.ArcaneAltarBlockEntity;
import buildaspell.client.model.AltarModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ArcaneAltarBlockEntityRenderer implements BlockEntityRenderer<ArcaneAltarBlockEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("buildaspell", "textures/entity/arcane_altar.png");
    private static final RenderType RENDER_TYPE = RenderType.entityTranslucentEmissive(TEXTURE);

    // Bedrock units to blocks
    private static final float SCALE = 1.0f / 16.0f;

    // Item floats above the model (crystal top ~Y=26 bedrock units = 1.625 blocks)
    private static final float ITEM_BASE_HEIGHT = 2.0f;

    // Ingredient ring orbits a little below the focal item.
    private static final float ORBIT_BASE_HEIGHT = 1.4f;

    private static AltarModel model;
    private final ItemRenderer itemRenderer;

    public ArcaneAltarBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(ArcaneAltarBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (model == null) {
            model = new AltarModel();
        }

        Level level = blockEntity.getLevel();
        float gameTime = level != null ? level.getGameTime() : 0;
        float time = gameTime + partialTick;

        // Render the 3D altar model
        if (model.isLoaded()) {
            float timeSeconds = time / 20.0f;
            VertexConsumer consumer = bufferSource.getBuffer(RENDER_TYPE);

            poseStack.pushPose();
            // Center model on the block and scale from Bedrock units to blocks
            poseStack.translate(0.5f, 0.0f, 0.5f);
            poseStack.scale(SCALE, SCALE, SCALE);

            for (int i = 0; i < model.getBoneCount(); i++) {
                if (model.isBoneAnimated(i)) {
                    // Animated rune bones: apply spin + bob
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
                    // Static bones: render directly
                    model.renderBone(consumer, poseStack.last(), i);
                }
            }

            poseStack.popPose();
        }

        // Render floating item above the altar
        ItemStack stack = blockEntity.getItem(0);
        if (!stack.isEmpty()) {
            poseStack.pushPose();

            float bobHeight = ITEM_BASE_HEIGHT + 0.1f * (float) Math.sin(time * 0.1f);
            poseStack.translate(0.5f, bobHeight, 0.5f);
            poseStack.mulPose(Axis.YP.rotationDegrees(time * 2.0f));
            poseStack.scale(0.6f, 0.6f, 0.6f);

            itemRenderer.renderStatic(stack, ItemDisplayContext.GROUND, packedLight, OverlayTexture.NO_OVERLAY,
                    poseStack, bufferSource, level, (int) blockEntity.getBlockPos().asLong());

            poseStack.popPose();
        }

        // Ingredient items orbit the altar in a slowly-spinning ring beneath the focal item.
        // Only the filled slots orbit, spread evenly around the circle so gaps don't show.
        java.util.List<ItemStack> orbiting = new java.util.ArrayList<>();
        for (int i = 0; i < ArcaneAltarBlockEntity.INGREDIENT_COUNT; i++) {
            ItemStack ing = blockEntity.getItem(ArcaneAltarBlockEntity.INGREDIENT_START + i);
            if (!ing.isEmpty()) orbiting.add(ing);
        }
        int n = orbiting.size();
        if (n > 0) {
            float orbitRadius = 0.55f;
            float orbitHeight = ORBIT_BASE_HEIGHT;
            for (int i = 0; i < n; i++) {
                float angle = (360.0f / n) * i + time * 1.2f;
                double rad = Math.toRadians(angle);
                float ox = (float) Math.cos(rad) * orbitRadius;
                float oz = (float) Math.sin(rad) * orbitRadius;
                float oy = orbitHeight + 0.08f * (float) Math.sin(time * 0.08f + i);

                poseStack.pushPose();
                poseStack.translate(0.5f + ox, oy, 0.5f + oz);
                poseStack.mulPose(Axis.YP.rotationDegrees(-angle));
                poseStack.scale(0.35f, 0.35f, 0.35f);

                itemRenderer.renderStatic(orbiting.get(i), ItemDisplayContext.GROUND, packedLight,
                        OverlayTexture.NO_OVERLAY, poseStack, bufferSource, level,
                        (int) blockEntity.getBlockPos().asLong() + i + 1);

                poseStack.popPose();
            }
        }
    }
}
