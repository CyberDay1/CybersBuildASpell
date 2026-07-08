package buildaspell.client.renderer.state;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;

import java.util.UUID;

public class PortalRenderState extends EntityRenderState {
    public float portalWidth = 1.5f;
    public float portalHeight = 2.5f;
    public boolean isDialed;
    public int ticksExisted;
    public String destDimensionId = "";
    public UUID portalUUID;
    public Vec3 axisW = new Vec3(1, 0, 0);
    public Vec3 axisH = new Vec3(0, 1, 0);
    public Vec3 normal = new Vec3(0, 0, 1);
    public Vec3 portalOrigin = Vec3.ZERO;
    public Vec3 destinationPos = Vec3.ZERO;
    /** True when NeoPortals is present and handling the portal's visual rendering. */
    public boolean neoPortalsHandlingRender;
}
