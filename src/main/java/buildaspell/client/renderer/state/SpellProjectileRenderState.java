package buildaspell.client.renderer.state;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.neoforged.api.distmarker.Dist;

public class SpellProjectileRenderState extends EntityRenderState {
    public String effectType = "";
    public int lifetime;
    public int visualColor;
    public String visualShape = "";
}
