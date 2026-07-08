package buildaspell.mana;

import buildaspell.config.ModConfig;
import buildaspell.registry.ModAttributes;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.EnumSet;
import java.util.Optional;

public class ManaHelper {
    private static final Identifier MANA_POOL_ENCH_ID = Identifier.parse("buildaspell:mana_pool");
    private static final Identifier MANA_REGEN_ENCH_ID = Identifier.parse("buildaspell:mana_regeneration");
    private static final Identifier SPELL_POWER_ENCH_ID = Identifier.parse("buildaspell:spell_power");

    // Which equipment slots each enchantment is read from. Spell Power counts only on the main
    // hand — a weapon or a wand — so an off-hand copy or a second held wand never stacks. The mana
    // enchantments count only on worn armour. Nothing is ever read from the off hand.
    private static final EnumSet<EquipmentSlot> MAINHAND_ONLY = EnumSet.of(EquipmentSlot.MAINHAND);
    private static final EnumSet<EquipmentSlot> ARMOR_SLOTS =
            EnumSet.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

    public static float getMaxMana(Player player) {
        double base = player.getAttributeValue(ModAttributes.MANA_POOL);
        int enchantBonus = getTotalEnchantmentLevel(player, MANA_POOL_ENCH_ID, ARMOR_SLOTS, ModConfig.getManaPoolMaxLevel());
        return (float) base + (enchantBonus * ManaConstants.MANA_POOL_PER_LEVEL);
    }

    public static float getManaRegen(Player player) {
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
    public static int getTotalEnchantmentLevel(Player player, Identifier enchantmentId,
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

    private static int getEnchantmentLevel(Player player, ItemStack stack, Identifier enchantmentId) {
        Optional<Holder.Reference<Enchantment>> enchHolder = player.level()
                .registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .get(net.minecraft.resources.ResourceKey.create(Registries.ENCHANTMENT, enchantmentId));

        return enchHolder.map(ref -> EnchantmentHelper.getItemEnchantmentLevel(ref, stack)).orElse(0);
    }
}
