package buildaspell.client.renderer.state;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;

import java.util.ArrayList;
import java.util.List;

public class ArcaneAltarRenderState extends BlockEntityRenderState {
    public ItemStackRenderState itemState = new ItemStackRenderState();
    public boolean hasItem;
    public float time;
    // Filled ingredient slots, spread evenly around the orbiting ring beneath the focal item.
    public final List<ItemStackRenderState> orbiting = new ArrayList<>();
}
