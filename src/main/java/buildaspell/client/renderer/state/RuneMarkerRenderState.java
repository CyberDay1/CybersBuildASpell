package buildaspell.client.renderer.state;

import net.minecraft.core.Direction;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.neoforged.api.distmarker.Dist;

public class RuneMarkerRenderState extends EntityRenderState {
    public float chargeProgress;
    public int lifetime;
    public Direction surfaceDirection = Direction.UP;
    public boolean isTrap;
}
