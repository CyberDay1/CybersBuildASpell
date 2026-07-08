package buildaspell.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import buildaspell.block.entity.ArcaneAltarBlockEntity;
import buildaspell.client.model.AltarModel;
import buildaspell.client.renderer.state.ArcaneAltarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;

import javax.annotation.Nullable;

public class ArcaneAltarBlockEntityRenderer implements BlockEntityRenderer<ArcaneAltarBlockEntity, ArcaneAltarRenderState> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("buildaspell", "textures/entity/arcane_altar.png");
    private static final RenderType RENDER_TYPE = RenderTypes.entityTranslucentEmissive(TEXTURE);

    // Bedrock units to blocks
    private static final float SCALE = 1.0f / 16.0f;

    // Item floats above the model (crystal top ~Y=26 bedrock units = 1.625 blocks)
    private static final float ITEM_BASE_HEIGHT = 2.0f;

    // Ingredient ring orbits a little below the focal item.
    private static final float ORBIT_BASE_HEIGHT = 1.4f;

    private static AltarModel model;
    private final ItemModelResolver itemModelResolver;

    public ArcaneAltarBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public ArcaneAltarRenderState createRenderState() {
        return new ArcaneAltarRenderState();
    }

    @Override
    public void extractRenderState(ArcaneAltarBlockEntity blockEntity, ArcaneAltarRenderState state,
                                    float partialTick, Vec3 cameraPos,
                                    @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTick, cameraPos, crumblingOverlay);
        ItemStack stack = blockEntity.getItem(0);
        state.hasItem = !stack.isEmpty();
        if (state.hasItem) {
            state.itemState = new ItemStackRenderState();
            this.itemModelResolver.updateForTopItem(
                    state.itemState, stack, ItemDisplayContext.GROUND,
                    blockEntity.getLevel(), null,
                    (int) blockEntity.getBlockPos().asLong());
        }
        // Ingredient items orbit the altar; only the filled slots orbit, spread evenly.
        state.orbiting.clear();
        for (int i = 0; i < ArcaneAltarBlockEntity.INGREDIENT_COUNT; i++) {
            ItemStack ing = blockEntity.getItem(ArcaneAltarBlockEntity.INGREDIENT_START + i);
            if (!ing.isEmpty()) {
                ItemStackRenderState ingState = new ItemStackRenderState();
                this.itemModelResolver.updateForTopItem(
                        ingState, ing, ItemDisplayContext.GROUND,
                        blockEntity.getLevel(), null,
                        (int) blockEntity.getBlockPos().asLong() + state.orbiting.size() + 1);
                state.orbiting.add(ingState);
            }
        }

        float gameTime = blockEntity.getLevel() != null ? blockEntity.getLevel().getGameTime() : 0;
        state.time = gameTime + partialTick;
    }

    @Override
    public void submit(ArcaneAltarRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (model == null) {
            model = new AltarModel();
        }

        // Render the 3D altar model
        if (model.isLoaded()) {
            float timeSeconds = state.time / 20.0f;

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

                    final int boneIdx = i;
                    collector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, consumer) -> {
                        model.renderBone(consumer, pose, boneIdx);
                    });

                    poseStack.popPose();
                } else {
                    // Static bones: render directly
                    final int boneIdx = i;
                    collector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, consumer) -> {
                        model.renderBone(consumer, pose, boneIdx);
                    });
                }
            }

            poseStack.popPose();
        }

        // Render floating item above the altar
        if (state.hasItem) {
            poseStack.pushPose();

            float bobHeight = ITEM_BASE_HEIGHT + 0.1f * (float) Math.sin(state.time * 0.1f);
            poseStack.translate(0.5f, bobHeight, 0.5f);
            poseStack.mulPose(Axis.YP.rotationDegrees(state.time * 2.0f));
            poseStack.scale(0.6f, 0.6f, 0.6f);

            state.itemState.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);

            poseStack.popPose();
        }

        // Ingredient items orbit the altar in a slowly-spinning ring beneath the focal item.
        int n = state.orbiting.size();
        if (n > 0) {
            float orbitRadius = 0.55f;
            float orbitHeight = ORBIT_BASE_HEIGHT;
            for (int i = 0; i < n; i++) {
                float angle = (360.0f / n) * i + state.time * 1.2f;
                double rad = Math.toRadians(angle);
                float ox = (float) Math.cos(rad) * orbitRadius;
                float oz = (float) Math.sin(rad) * orbitRadius;
                float oy = orbitHeight + 0.08f * (float) Math.sin(state.time * 0.08f + i);

                poseStack.pushPose();
                poseStack.translate(0.5f + ox, oy, 0.5f + oz);
                poseStack.mulPose(Axis.YP.rotationDegrees(-angle));
                poseStack.scale(0.35f, 0.35f, 0.35f);

                state.orbiting.get(i).submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);

                poseStack.popPose();
            }
        }
    }
}
