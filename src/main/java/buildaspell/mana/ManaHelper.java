package buildaspell.mana;

import buildaspell.compat.IronsManaCompat;
import buildaspell.config.ModConfig;
import buildaspell.registry.ModAttachments;
import buildaspell.registry.ModAttributes;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.EnumSet;
import java.util.Optional;

public class ManaHelper {
    private static final ResourceLocation MANA_POOL_ENCH_ID = ResourceLocation.parse("buildaspell:mana_pool");
    private static final ResourceLocation MANA_REGEN_ENCH_ID = ResourceLocation.parse("buildaspell:mana_regeneration");
    private static final ResourceLocation SPELL_POWER_ENCH_ID = ResourceLocation.parse("buildaspell:spell_power");

    // Which equipment slots each enchantment is read from. Spell Power counts only on the main
    // hand — a weapon or a wand — so an off-hand copy or a second held wand never stacks. The mana
    // enchantments count only on worn armour. Nothing is ever read from the off hand.
    private static final EnumSet<EquipmentSlot> MAINHAND_ONLY = EnumSet.of(EquipmentSlot.MAINHAND);
    private static final EnumSet<EquipmentSlot> ARMOR_SLOTS =
            EnumSet.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

    /**
     * This player's current mana. There is only ever one backing store: Iron's Spellbooks' pool
     * while {@link IronsManaCompat#isDeferring()}, and buildaspell's own attachment otherwise.
     * Every read of current mana goes through here rather than the attachment directly.
     */
    public static float getCurrentMana(Player player) {
        if (IronsManaCompat.isDeferring()) {
            return IronsManaCompat.getMana(player);
        }
        return player.getData(ModAttachments.PLAYER_MANA.get()).getCurrentMana();
    }

    /**
     * Sets current mana, floored at zero. Writes are server-side while deferring — Iron's treats
     * its pool as server-authoritative and sends the figure back down itself.
     */
    public static void setCurrentMana(Player player, float mana) {
        if (IronsManaCompat.isDeferring()) {
            IronsManaCompat.setMana(player, mana);
            return;
        }
        player.getData(ModAttachments.PLAYER_MANA.get()).setCurrentMana(mana);
    }

    /** Adds (or, with a negative amount, removes) mana without checking affordability. */
    public static void addMana(Player player, float amount) {
        if (IronsManaCompat.isDeferring()) {
            IronsManaCompat.setMana(player, IronsManaCompat.getMana(player) + amount);
            return;
        }
        player.getData(ModAttachments.PLAYER_MANA.get()).addMana(amount);
    }

    /** Spends {@code amount} and returns true, or returns false and leaves the pool untouched. */
    public static boolean consumeMana(Player player, float amount) {
        if (IronsManaCompat.isDeferring()) {
            float current = IronsManaCompat.getMana(player);
            if (current < amount) {
                return false;
            }
            IronsManaCompat.setMana(player, current - amount);
            return true;
        }
        return player.getData(ModAttachments.PLAYER_MANA.get()).consumeMana(amount);
    }

    /**
     * Maximum mana. While deferring this is Iron's pool alone, without buildaspell's Mana Pool
     * enchantment on top: Iron's clamps every write to its own attribute, so a bigger figure here
     * would only widen a bar that can never fill.
     */
    public static float getMaxMana(Player player) {
        if (IronsManaCompat.isDeferring()) {
            return IronsManaCompat.getMaxMana(player);
        }
        double base = player.getAttributeValue(ModAttributes.MANA_POOL);
        int enchantBonus = getTotalEnchantmentLevel(player, MANA_POOL_ENCH_ID, ARMOR_SLOTS, ModConfig.getManaPoolMaxLevel());
        return (float) base + (enchantBonus * ManaConstants.MANA_POOL_PER_LEVEL);
    }

    /**
     * Regeneration in flat mana per second, on both paths. Iron's states its own regen as a
     * multiplier on a proportional rate, so while deferring the figure is converted through the
     * pool size to keep this contract — it will grow as the pool grows, where buildaspell's own
     * figure does not.
     */
    public static float getManaRegen(Player player) {
        if (IronsManaCompat.isDeferring()) {
            return IronsManaCompat.getManaRegenPerSecond(player);
        }
        double base = player.getAttributeValue(ModAttributes.MANA_REGEN);
        int enchantBonus = getTotalEnchantmentLevel(player, MANA_REGEN_ENCH_ID, ARMOR_SLOTS, ModConfig.getManaRegenMaxLevel());
        return (float) base + (enchantBonus * ManaConstants.MANA_REGEN_PER_LEVEL);
    }

    public static float getSpellPower(Player player) {
        double base = player.getAttributeValue(ModAttributes.SPELL_POWER);
        int enchantBonus = getTotalEnchantmentLevel(player, SPELL_POWER_ENCH_ID, MAINHAND_ONLY, ModConfig.getSpellPowerMaxLevel());
        float wandBonus = buildaspell.item.WandItem.heldSpellPowerBonus(player);
        return (float) base + (enchantBonus * (float) ModConfig.getSpellPowerPerLevel()) + wandBonus;
    }

    /**
     * Sums the enchantment level across the given equipment slots, clamping each item's level to
     * {@code maxPerItem} first. The cap lets pack devs rein in the buildaspell enchantments via
     * config: levels above the cap still display but stop scaling the bonus.
     */
    public static int getTotalEnchantmentLevel(Player player, ResourceLocation enchantmentId,
                                               EnumSet<EquipmentSlot> slots, int maxPerItem) {
        int total = 0;
        for (EquipmentSlot slot : slots) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                total += Math.min(getEnchantmentLevel(player, stack, enchantmentId), maxPerItem);
            }
        }
        return total;
    }

    private static int getEnchantmentLevel(Player player, ItemStack stack, ResourceLocation enchantmentId) {
        Optional<Holder.Reference<Enchantment>> enchHolder = player.level()
                .registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .get(net.minecraft.resources.ResourceKey.create(Registries.ENCHANTMENT, enchantmentId));

        return enchHolder.map(ref -> EnchantmentHelper.getItemEnchantmentLevel(ref, stack)).orElse(0);
    }
}
