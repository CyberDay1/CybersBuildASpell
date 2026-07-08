package buildaspell.block.entity;

import buildaspell.menu.ArcaneAltarMenu;
import buildaspell.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
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
import java.util.Optional;

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

    // --- Saved form --------------------------------------------------------------------------
    // Vanilla's item codec refuses any count above 99 (ItemStack.CODEC uses
    // ExtraCodecs.intRange(1, 99) for "count", and Codec.validate fires on encode as well as
    // decode), so ContainerHelper.saveAllItems cannot write an ingredient slot that holds more
    // than 99: the count field is silently dropped on the way out and comes back as 1. The altar
    // keeps its 320-per-slot capacity and writes its own list instead. The item and its components
    // go through the vanilla codec at a count of 1, which always encodes cleanly, and the real
    // count rides alongside as a plain int.
    private static final String TAG_ALTAR_ITEMS = "AltarItems";
    private static final String TAG_SLOT = "Slot";
    private static final String TAG_ITEM = "Item";
    private static final String TAG_COUNT = "Count";

    // --- Enchantment targeting ---------------------------------------------------------------
    public static final Identifier MANA_POOL_ENCHANTMENT =
            Identifier.fromNamespaceAndPath("buildaspell", "mana_pool");
    public static final Identifier MANA_REGENERATION_ENCHANTMENT =
            Identifier.fromNamespaceAndPath("buildaspell", "mana_regeneration");
    public static final Identifier SPELL_POWER_ENCHANTMENT =
            Identifier.fromNamespaceAndPath("buildaspell", "spell_power");

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
        if (isMainHandTarget(stack)) return true;
        return stack.is(ItemTags.ARMOR_ENCHANTABLE);
    }

    /** A wand or a weapon: something ManaHelper will read out of the main hand. */
    private static boolean isMainHandTarget(ItemStack stack) {
        return stack.getItem() instanceof buildaspell.item.WandItem || stack.is(ItemTags.WEAPON_ENCHANTABLE);
    }

    /**
     * Whether a specific altar enchantment will actually do anything on a specific item. The
     * altar's three enchantments are each read from one place only (see
     * {@link buildaspell.mana.ManaHelper}): Spell Power off the main hand, Mana Pool and Mana
     * Regeneration off worn armor. Putting one on the wrong kind of item costs XP and materials
     * and grants nothing, so both the screen and the enchant packet gate on this.
     *
     * <p>This is the single source of truth for the pairing: do not re-derive it anywhere else.
     */
    public static boolean isValidEnchantTarget(@Nullable Identifier enchantmentId, ItemStack stack) {
        if (enchantmentId == null || stack.isEmpty()) return false;
        if (SPELL_POWER_ENCHANTMENT.equals(enchantmentId)) return isMainHandTarget(stack);
        if (MANA_POOL_ENCHANTMENT.equals(enchantmentId) || MANA_REGENERATION_ENCHANTMENT.equals(enchantmentId)) {
            return stack.is(ItemTags.ARMOR_ENCHANTABLE);
        }
        // The altar only sells its own three enchantments; anything else is not a valid request.
        return false;
    }

    /** The player-facing reason an enchantment was refused, used by both the screen and the packet. */
    public static MutableComponent enchantTargetRequirement(@Nullable Identifier enchantmentId) {
        if (SPELL_POWER_ENCHANTMENT.equals(enchantmentId)) {
            return Component.translatable("gui.buildaspell.arcane_altar.requires_main_hand");
        }
        if (MANA_POOL_ENCHANTMENT.equals(enchantmentId) || MANA_REGENERATION_ENCHANTMENT.equals(enchantmentId)) {
            return Component.translatable("gui.buildaspell.arcane_altar.requires_armor");
        }
        return Component.translatable("gui.buildaspell.arcane_altar.unknown_enchantment");
    }

    public ArcaneAltarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ARCANE_ALTAR.get(), pos, state);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items.clear();
        Optional<ValueInput.ValueInputList> saved = input.childrenList(TAG_ALTAR_ITEMS);
        if (saved.isEmpty()) {
            // Altars saved before this format used vanilla's "Items" list. Counts there are all
            // 99 or less (anything higher never survived the write), so the vanilla reader is
            // safe; the next save rewrites the altar under TAG_ALTAR_ITEMS.
            ContainerHelper.loadAllItems(input, this.items);
            return;
        }

        for (ValueInput entry : saved.get()) {
            int slot = entry.getIntOr(TAG_SLOT, -1);
            if (slot < 0 || slot >= this.items.size()) continue;
            int count = entry.getIntOr(TAG_COUNT, 1);
            entry.read(TAG_ITEM, ItemStack.CODEC).ifPresent(stack -> {
                // The item is always written at a count of 1; the real count is stored beside it
                // precisely because it can exceed what the item codec accepts.
                stack.setCount(Mth.clamp(count, 1, MAX_STACK_PER_SLOT));
                this.items.set(slot, stack);
            });
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ValueOutput.ValueOutputList list = output.childrenList(TAG_ALTAR_ITEMS);
        for (int i = 0; i < this.items.size(); i++) {
            ItemStack stack = this.items.get(i);
            if (stack.isEmpty()) continue;
            ValueOutput entry = list.addChild();
            entry.putInt(TAG_SLOT, i);
            entry.store(TAG_ITEM, ItemStack.CODEC, stack.copyWithCount(1));
            entry.putInt(TAG_COUNT, stack.getCount());
        }
    }

    @Override
    public int getContainerSize() { return items.size(); }

    @Override
    public int getMaxStackSize() { return MAX_STACK_PER_SLOT; }

    /**
     * Automation's per-insert limit, kept in step with what the menu's slots allow: an ingredient
     * material may pile up to {@link #MAX_STACK_PER_SLOT} (only the four ingredient slots ever
     * hold those items), anything else stays at the item's own cap. Vanilla's default here is
     * {@code min(getMaxStackSize(), stack.getMaxStackSize())}, which would have silently pinned
     * every slot to 64 regardless of the container limit.
     */
    @Override
    public int getMaxStackSize(ItemStack stack) {
        return INGREDIENT_MATERIALS.contains(stack.getItem())
                ? MAX_STACK_PER_SLOT
                : Math.min(getMaxStackSize(), stack.getMaxStackSize());
    }

    @Override
    public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }

    @Override
    public ItemStack getItem(int slot) { return items.get(slot); }

    /**
     * Extraction chokepoint. A slot may hold more than a normal stack, so no single withdrawal is
     * allowed to hand out an oversized {@link ItemStack} instance: one of those in a player
     * inventory, in the carried slot or on the ground is an unsaveable world. Everything that
     * pulls items out lands here: {@code Slot#remove} (so GUI pickup, drop-stack and double-click
     * collect), hoppers, and NeoForge's {@code IItemHandler#extractItem}.
     */
    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot < 0 || slot >= items.size()) return ItemStack.EMPTY;
        return ContainerHelper.removeItem(items, slot, Math.min(amount, items.get(slot).getMaxStackSize()));
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }

    @Override
    public void setItem(int slot, ItemStack stack) {
        // Clamp before storing, and clamp against the per-item limit rather than the raw container
        // limit: the old order stored the stack first and then trimmed it, so the ceiling depended
        // on the caller reading the stack back afterwards.
        stack.limitSize(getMaxStackSize(stack));
        items.set(slot, stack);
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
