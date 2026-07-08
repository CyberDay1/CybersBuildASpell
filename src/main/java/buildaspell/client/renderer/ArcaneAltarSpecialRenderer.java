package buildaspell.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;
import buildaspell.client.model.AltarModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.function.Consumer;

/**
 * Renders the Arcane Altar item (inventory icon, in-hand, dropped, item frame) using the
 * same 3D {@link AltarModel} as the in-world block, via the 26.x special-model-renderer
 * pipeline. Positioning per display context is driven by the {@code display} block in the
 * base model {@code models/item/arcane_altar.json}; this renderer only draws the geometry.
 */
public class ArcaneAltarSpecialRenderer implements NoDataSpecialModelRenderer {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("buildaspell", "textures/entity/arcane_altar.png");
    private static final RenderType RENDER_TYPE = RenderTypes.entityTranslucentEmissive(TEXTURE);

    // Bedrock units to blocks
    private static final float SCALE = 1.0f / 16.0f;

    private static AltarModel model;

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector,
                       int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
        if (model == null) {
            model = new AltarModel();
        }
        if (!model.isLoaded()) {
            return;
        }

        // Wall-clock time so the runes still spin/bob while shown in the inventory.
        float timeSeconds = System.nanoTime() / 1_000_000_000.0f;

        poseStack.pushPose();
        // Centre the model in the unit cube and scale from Bedrock units to blocks,
        // matching ArcaneAltarBlockEntityRenderer.
        poseStack.translate(0.5f, 0.0f, 0.5f);
        poseStack.scale(SCALE, SCALE, SCALE);

        for (int i = 0; i < model.getBoneCount(); i++) {
            final int boneIdx = i;
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
                collector.submitCustomGeometry(poseStack, RENDER_TYPE,
                        (pose, consumer) -> model.renderBone(consumer, pose, boneIdx));
                poseStack.popPose();
            } else {
                collector.submitCustomGeometry(poseStack, RENDER_TYPE,
                        (pose, consumer) -> model.renderBone(consumer, pose, boneIdx));
            }
        }

        poseStack.popPose();
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        // Rough model bounds (block space) for the item bounding box / GUI culling.
        // Display sizing is handled by the base model's display transforms.
        output.accept(new Vector3f(0.0f, 0.0f, 0.0f));
        output.accept(new Vector3f(1.0f, 0.0f, 0.0f));
        output.accept(new Vector3f(0.0f, 0.0f, 1.0f));
        output.accept(new Vector3f(1.0f, 0.0f, 1.0f));
        output.accept(new Vector3f(0.0f, 1.6f, 0.0f));
        output.accept(new Vector3f(1.0f, 1.6f, 0.0f));
        output.accept(new Vector3f(0.0f, 1.6f, 1.0f));
        output.accept(new Vector3f(1.0f, 1.6f, 1.0f));
    }

    /** Codec wiring so the {@code buildaspell:arcane_altar} special model resolves to this renderer. */
    public record Unbaked() implements NoDataSpecialModelRenderer.Unbaked {
        public static final Unbaked INSTANCE = new Unbaked();
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(INSTANCE);

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public SpecialModelRenderer<Void> bake(SpecialModelRenderer.BakingContext context) {
            return new ArcaneAltarSpecialRenderer();
        }
    }
}
