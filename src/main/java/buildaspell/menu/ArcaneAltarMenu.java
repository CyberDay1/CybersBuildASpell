package buildaspell.menu;

import buildaspell.block.entity.ArcaneAltarBlockEntity;
import buildaspell.registry.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ArcaneAltarMenu extends AbstractContainerMenu {
    // Container index map: 0 = input item, 1-4 = ingredient slots.
    private static final int CONTAINER_SLOTS = 1 + ArcaneAltarBlockEntity.INGREDIENT_COUNT;
    // First player-inventory slot index in this.slots (after the container slots).
    private static final int INV_START = CONTAINER_SLOTS;
    private static final int INV_END = CONTAINER_SLOTS + 36; // 27 inv + 9 hotbar

    public static final int INGREDIENT_GRID_X = 212; // 190 + (78 - 2*18)/2 + 1, centred in the panel
    public static final int INGREDIENT_GRID_Y = 40;
    public static final int INGREDIENT_SLOT_PITCH = 18;

    private final Container container;

    // Client-controlled: the ingredient slots are hidden (inactive) until the tab is opened.
    // Defaults open on the server so shift-click / move logic always works there.
    public boolean ingredientTabOpen;

    /**
     * The client-side mirror of the altar's contents. It has to carry the altar's raised limit:
     * {@link SimpleContainer#setItem} trims every stack it is handed to its own maximum, so a plain
     * one would clamp the synced ingredient counts to 64 and the have/need bill would under-report
     * what the server is actually holding.
     */
    private static Container clientMirrorContainer() {
        return new SimpleContainer(CONTAINER_SLOTS) {
            @Override
            public int getMaxStackSize() {
                return ArcaneAltarBlockEntity.MAX_STACK_PER_SLOT;
            }

            @Override
            public int getMaxStackSize(ItemStack stack) {
                return ArcaneAltarBlockEntity.MAX_STACK_PER_SLOT;
            }
        };
    }

    // Client constructor
    public ArcaneAltarMenu(int containerId, Inventory playerInv, FriendlyByteBuf data) {
        this(containerId, playerInv, clientMirrorContainer());
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
            public int getMaxStackSize(ItemStack stack) { return stack.getMaxStackSize(); }
            // Only accept items an altar enchant can read (weapon / wand / armour); base mayPlace is true.
            @Override
            public boolean mayPlace(ItemStack stack) {
                return container.canPlaceItem(ArcaneAltarBlockEntity.INPUT_SLOT, stack);
            }
        });

        // Ingredient slots — a 2-wide grid (4 slots: two rows of two) living in the
        // side tab (local coords sit to the right of the main GUI). Hidden until the tab opens.
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
                public int getMaxStackSize(ItemStack stack) { return ArcaneAltarBlockEntity.MAX_STACK_PER_SLOT; }

                // Each slot only accepts its assigned material (iron / gold / diamond / netherite),
                // and once occupied it only accepts more of exactly what is already there. Without
                // the second half, clicking a full slot while carrying a same-item-different-
                // components stack (a renamed diamond, say) makes vanilla swap the two, handing the
                // player the slot's whole oversized pile as the carried stack.
                @Override
                public boolean mayPlace(ItemStack stack) {
                    if (!container.canPlaceItem(ArcaneAltarBlockEntity.INGREDIENT_START + ingredientIndex, stack)) {
                        return false;
                    }
                    ItemStack current = getItem();
                    return current.isEmpty() || ItemStack.isSameItemSameComponents(current, stack);
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

    /**
     * An ingredient slot can hold more than a vanilla stack, and no oversized {@link ItemStack} may
     * ever leave the altar: vanilla's item codec caps a count at 99, so one sitting in a player
     * inventory or on the ground makes the world unsaveable. Every other way out of the container
     * funnels through {@code Container#removeItem}, which clamps. The hotbar swap does not: it
     * hands {@code Slot#getItem()} straight to {@code Inventory#setItem}. Intercept that one case
     * and move a single stack instead.
     */
    @Override
    public void clicked(int slotId, int button, ContainerInput containerInput, Player player) {
        if (containerInput == ContainerInput.SWAP && slotId >= 0 && slotId < CONTAINER_SLOTS) {
            Slot slot = this.slots.get(slotId);
            ItemStack inSlot = slot.getItem();
            if (inSlot.getCount() > inSlot.getMaxStackSize()) {
                boolean hotbarTarget = button >= 0 && button < 9 || button == 40;
                if (hotbarTarget && player.getInventory().getItem(button).isEmpty() && slot.mayPickup(player)) {
                    ItemStack taken = slot.remove(inSlot.getMaxStackSize());
                    if (!taken.isEmpty()) {
                        player.getInventory().setItem(button, taken);
                        slot.onTake(player, taken);
                    }
                }
                return;
            }
        }
        super.clicked(slotId, button, containerInput, player);
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
