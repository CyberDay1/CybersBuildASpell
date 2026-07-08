package buildaspell.item;

import buildaspell.config.ModConfig;
import buildaspell.registry.ModAttachments;
import buildaspell.spell.PlayerSpellSlots;
import buildaspell.spell.Spell;
import buildaspell.spell.execution.SpellExecutor;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A spell-casting wand. Right-clicking casts the player's currently selected spell (the active
 * slot of {@link PlayerSpellSlots}) through the normal {@link SpellExecutor} path, so casting with
 * a wand behaves identically to the cast keybind.
 *
 * <p>While a wand is held it also discounts the mana cost of every spell cast and grants bonus
 * Spell Power. Both bonuses are read live from {@link ModConfig} (the {@code wands.toml} server
 * spec), so server owners can tune every number; nothing is baked into the item. The discount is
 * applied in {@link SpellExecutor} and the Spell Power bonus in
 * {@link buildaspell.mana.ManaHelper#getSpellPower}. When two wands are held (main + off hand) the
 * higher tier wins — bonuses never stack.
 */
public class WandItem extends Item {
    private final WandTier tier;

    public WandItem(WandTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public WandTier tier() {
        return tier;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResultHolder.success(held);
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.pass(held);
        }

        PlayerSpellSlots slots = serverPlayer.getData(ModAttachments.PLAYER_SPELL_SLOTS.get());
        Spell active = slots.getActiveSpell();
        if (active == null) {
            serverPlayer.sendSystemMessage(
                    Component.translatable("item.buildaspell.wand.no_active_spell").withStyle(ChatFormatting.YELLOW),
                    true);
            return InteractionResultHolder.fail(held);
        }

        boolean cast = SpellExecutor.executeSpell(serverPlayer, active);
        return cast ? InteractionResultHolder.success(held) : InteractionResultHolder.fail(held);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int reductionPct = (int) Math.round(ModConfig.wandManaReduction(tier) * 100.0);
        double power = ModConfig.wandSpellPower(tier);

        tooltip.add(Component.translatable("item.buildaspell.wand.tooltip.cast").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.buildaspell.wand.tooltip.mana", reductionPct).withStyle(ChatFormatting.AQUA));
        if (power > 0) {
            String shown = (power == Math.floor(power)) ? String.valueOf((int) power) : String.valueOf(power);
            tooltip.add(Component.translatable("item.buildaspell.wand.tooltip.power", shown).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    // ---- held-wand bonus helpers (used by SpellExecutor + ManaHelper) ----

    /** The strongest wand tier the player holds in either hand, or null if no wand is held. */
    public static WandTier bestHeldTier(Player player) {
        WandTier best = null;
        for (InteractionHand hand : InteractionHand.values()) {
            if (player.getItemInHand(hand).getItem() instanceof WandItem w
                    && (best == null || w.tier.ordinal() > best.ordinal())) {
                best = w.tier;
            }
        }
        return best;
    }

    /** Multiplier to apply to a spell's mana cost given the held wand (1.0 = no wand / no discount). */
    public static float heldDiscountMultiplier(Player player) {
        WandTier best = bestHeldTier(player);
        if (best == null) {
            return 1.0f;
        }
        float mult = (float) (1.0 - ModConfig.wandManaReduction(best));
        return mult < 0f ? 0f : mult;
    }

    /** Flat Spell Power bonus granted by the held wand (0 if no wand is held). */
    public static float heldSpellPowerBonus(Player player) {
        WandTier best = bestHeldTier(player);
        return best == null ? 0f : (float) ModConfig.wandSpellPower(best);
    }
}
