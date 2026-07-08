package buildaspell.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import buildaspell.client.model.TornadoModel;
import buildaspell.client.renderer.state.TornadoRenderState;
import buildaspell.entity.TornadoEntity;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;

public class TornadoEntityRenderer extends EntityRenderer<TornadoEntity, TornadoRenderState> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("buildaspell", "textures/entity/tornado.png");
    // Depth-writing translucent (NOT the emissive variant): entityTranslucentEmissive writes color only,
    // so the vanilla water pass — which renders after entities — blends its blue over the funnel and washes
    // it out anywhere the tornado overlaps an ocean. Writing depth makes water depth-fail behind the funnel
    // instead, so it stays visible over water.
    private static final RenderType RENDER_TYPE = RenderTypes.entityTranslucent(TEXTURE);

    // Visual scale: model radius maps to this fraction of the AoE range
    private static final float VISUAL_FRACTION = 0.45f;

    // Slow the funnel spin down from the raw animation rate (testers found it too fast/dizzying).
    private static final float SPIN_SPEED = 0.5f;

    private static TornadoModel model;

    public TornadoEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public TornadoRenderState createRenderState() {
        return new TornadoRenderState();
    }

    /**
     * The collision box is a tiny 0.5³ marker, but the rendered funnel scales up to the full AoE
     * radius (and ~1.4× that in height). Without widening the culling box, the client frustum-culls
     * the whole model as soon as the little box leaves view — which happens instantly at large area
     * when the camera sits near or inside the funnel, making a max-size tornado appear to not spawn.
     * On 26.2 the render culling box lives on the renderer (it took the entity as a parameter),
     * not on Entity, so the override moved here from TornadoEntity.
     */
    @Override
    protected AABB getBoundingBoxForCulling(TornadoEntity entity) {
        float r = entity.getRange();
        double h = r * 1.4 + 2.0; // matches the particle column height, plus a margin
        return new AABB(entity.getX() - r, entity.getY() - 1.0, entity.getZ() - r,
                entity.getX() + r, entity.getY() + h, entity.getZ() + r);
    }

    @Override
    public void extractRenderState(TornadoEntity entity, TornadoRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.range = entity.getRange();
        // Drive the funnel animation off the client-advancing render age, not the custom lifetime
        // tickCount (which stays frozen on the client after the server-side early-return in tick()),
        // otherwise a large tornado twitches instead of spinning.
        state.tickCount = entity.getRenderAge();
    }

    @Override
    public void submit(TornadoRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (model == null) {
            model = new TornadoModel();
        }
        if (!model.isLoaded()) {
            super.submit(state, poseStack, collector, camera);
            return;
        }

        float timeSeconds = (state.tickCount + state.partialTick) / 20.0f * SPIN_SPEED;
        float range = state.range;
        float scale = range * VISUAL_FRACTION / model.getModelRadius();

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

            final int boneIdx = i;
            collector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, consumer) -> {
                model.renderBone(consumer, pose, boneIdx);
            });

            poseStack.popPose();
        }

        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }
}
