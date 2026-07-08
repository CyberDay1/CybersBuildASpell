package buildaspell.menu;

import buildaspell.block.entity.ArcaneAltarBlockEntity;
import buildaspell.registry.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ArcaneAltarMenu extends AbstractContainerMenu {
    // Container index map: 0 = input item, 1-5 = ingredient slots.
    private static final int CONTAINER_SLOTS = 1 + ArcaneAltarBlockEntity.INGREDIENT_COUNT;
    // First player-inventory slot index in this.slots (after the container slots).
    private static final int INV_START = CONTAINER_SLOTS;
    private static final int INV_END = CONTAINER_SLOTS + 36; // 27 inv + 9 hotbar

    // Ingredient grid geometry (local coords), shared with ArcaneAltarScreen so the slot
    // hit-boxes and the drawn slot backgrounds stay aligned. The 2x2 grid is centred inside
    // the side panel (panel X=190, W=78 in the screen): 190 + (78 - 2*18)/2 + 1 = 212.
    public static final int INGREDIENT_GRID_X = 212;
    public static final int INGREDIENT_GRID_Y = 40;
    public static final int INGREDIENT_SLOT_PITCH = 18;

    private final Container container;

    // Client-controlled: the ingredient slots are hidden (inactive) until the tab is opened.
    // Defaults open on the server so shift-click / move logic always works there.
    public boolean ingredientTabOpen;

    // Client constructor
    public ArcaneAltarMenu(int containerId, Inventory playerInv, FriendlyByteBuf data) {
        this(containerId, playerInv, new SimpleContainer(CONTAINER_SLOTS));
        this.ingredientTabOpen = false;
    }

    // Server constructor
    public ArcaneAltarMenu(int containerId, Inventory playerInv, Container container) {
        super(ModMenuTypes.ARCANE_ALTAR.get(), containerId);
        this.container = container;
        this.ingredientTabOpen = true;

        // Altar input slot (centred in the right column, above the cost + Enchant button)
        this.addSlot(new Slot(container, ArcaneAltarBlockEntity.INPUT_SLOT, 128, 24) {
            @Override
            public int getMaxStackSize(ItemStack stack) {
                return stack.getMaxStackSize();
            }
            // Only accept items an altar enchant can actually read (weapon / wand / armour) — the
            // base Slot#mayPlace returns true, so without this the input takes anything.
            @Override
            public boolean mayPlace(ItemStack stack) {
                return container.canPlaceItem(ArcaneAltarBlockEntity.INPUT_SLOT, stack);
            }
        });

        // Ingredient slots — a 2x2 grid (one slot per material tier) living in the side tab
        // (local coords sit to the right of the main GUI). Hidden until the tab opens.
        for (int i = 0; i < ArcaneAltarBlockEntity.INGREDIENT_COUNT; i++) {
            int col = i % 2, row = i / 2;
            final int ingredientIndex = i;
            this.addSlot(new Slot(container, ArcaneAltarBlockEntity.INGREDIENT_START + i,
                    INGREDIENT_GRID_X + col * INGREDIENT_SLOT_PITCH,
                    INGREDIENT_GRID_Y + row * INGREDIENT_SLOT_PITCH) {
                @Override
                public boolean isActive() {
                    return ingredientTabOpen;
                }
                @Override
                public int getMaxStackSize(ItemStack stack) {
                    return ArcaneAltarBlockEntity.MAX_STACK_PER_SLOT;
                }
                // Each slot only accepts its assigned material (iron / gold / diamond / netherite).
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return container.canPlaceItem(ArcaneAltarBlockEntity.INGREDIENT_START + ingredientIndex, stack);
                }
            });
        }

        // Player inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 130 + row * 18));
            }
        }

        // Player hotbar
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 188));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < CONTAINER_SLOTS) {
                // From the altar (input or an ingredient slot) back to the player.
                if (!this.moveItemStackTo(stack, INV_START, INV_END, true)) return ItemStack.EMPTY;
            } else {
                // From the player: materials fill the ingredient slots first, else the input.
                if (!this.moveItemStackTo(stack, ArcaneAltarBlockEntity.INGREDIENT_START, CONTAINER_SLOTS, false)
                        && !this.moveItemStackTo(stack, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
