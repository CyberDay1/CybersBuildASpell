package buildaspell.block.entity;

import buildaspell.menu.ArcaneAltarMenu;
import buildaspell.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ArcaneAltarBlockEntity extends BlockEntity implements Container, MenuProvider {
    // Slot 0 = the item being enchanted; slots 1-4 = ingredient slots (one per material type:
    // iron / gold / diamond / netherite — the four tiers a cumulative bill ever charges).
    public static final int INPUT_SLOT = 0;
    public static final int INGREDIENT_START = 1;
    public static final int INGREDIENT_COUNT = 4;
    // Ingredient slots accept up to five vanilla stacks so a large enchant bill fits without
    // needing every slot filled. The input slot keeps its item's natural cap (see the menu).
    public static final int MAX_STACK_PER_SLOT = 320;
    // Fixed material per ingredient slot (index 0..INGREDIENT_COUNT-1), matching the cumulative
    // bill's tiers in EnchantmentCostManager (iron 1-5 / gold 6-10 / diamond 11-15 / netherite
    // 16-20). Each ingredient slot only accepts its own material, and shows a ghost of it.
    public static final List<Item> INGREDIENT_MATERIALS =
            List.of(Items.IRON_INGOT, Items.GOLD_INGOT, Items.DIAMOND, Items.NETHERITE_INGOT);
    private final NonNullList<ItemStack> items = NonNullList.withSize(1 + INGREDIENT_COUNT, ItemStack.EMPTY);

    /** The material an ingredient slot accepts (by 0-based ingredient index), or null if out of range. */
    @Nullable
    public static Item ingredientMaterial(int ingredientIndex) {
        return ingredientIndex >= 0 && ingredientIndex < INGREDIENT_MATERIALS.size()
                ? INGREDIENT_MATERIALS.get(ingredientIndex) : null;
    }

    /**
     * Whether an item is a legal enchant target for the altar's input slot. Only items the altar's
     * enchantments can actually read are allowed: a weapon or a wand (Spell Power lives on the main
     * hand) or a piece of armour (the mana enchantments live on worn armour). Everything else is
     * rejected so players can't sink an enchant into an item that would never read it.
     */
    public static boolean isEnchantableTarget(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof buildaspell.item.WandItem) return true;
        return stack.is(ItemTags.WEAPON_ENCHANTABLE) || stack.is(ItemTags.ARMOR_ENCHANTABLE);
    }

    public ArcaneAltarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ARCANE_ALTAR.get(), pos, state);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ContainerHelper.loadAllItems(input, this.items);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
    }

    @Override
    public int getContainerSize() { return items.size(); }

    @Override
    public int getMaxStackSize() { return MAX_STACK_PER_SLOT; }

    @Override
    public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }

    @Override
    public ItemStack getItem(int slot) { return items.get(slot); }

    @Override
    public ItemStack removeItem(int slot, int amount) { return ContainerHelper.removeItem(items, slot, amount); }

    @Override
    public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
        // Push the new contents to nearby clients so the floating item above the
        // altar reflects what's actually in the slot (and its enchant glint).
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    /**
     * Server-authoritative slot filter: the input slot only accepts a valid enchant target; each
     * ingredient slot only accepts its assigned material. Also gates hopper / automation insertion.
     */
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == INPUT_SLOT) return isEnchantableTarget(stack);
        Item mat = ingredientMaterial(slot - INGREDIENT_START);
        return mat != null && stack.is(mat);
    }

    @Override
    public void clearContent() { items.clear(); }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.buildaspell.arcane_altar");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInv, Player player) {
        return new ArcaneAltarMenu(containerId, playerInv, this);
    }
}
