package buildaspell.item;

import buildaspell.config.ModConfig;
import buildaspell.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public class BlankRuneItem extends Item {
    // Legacy NBT tag name kept stable so in-progress runes survive the souls->essence rename.
    private static final String ESSENCE_KEY = "SoulCount";

    public BlankRuneItem(Properties properties) {
        super(properties);
    }

    public static int getEssence(ItemStack stack) {
        return stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY)
                .copyTag().getIntOr(ESSENCE_KEY, 0);
    }

    public static void addEssence(ItemStack stack, int amount) {
        var customData = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY);
        var tag = customData.copyTag();
        int current = tag.getIntOr(ESSENCE_KEY, 0);
        int updated = Math.min(current + amount, ModConfig.getEssenceRequired());
        tag.putInt(ESSENCE_KEY, updated);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
    }

    public static boolean isFull(ItemStack stack) {
        return getEssence(stack) >= ModConfig.getEssenceRequired();
    }

    public static boolean isBoss(LivingEntity entity) {
        return entity instanceof WitherBoss ||
                entity instanceof EnderDragon ||
                (entity.getType().getCategory() == net.minecraft.world.entity.MobCategory.MONSTER &&
                        !entity.getType().canSpawnFarFromPlayer());
    }

    public static int getEssenceValue(LivingEntity entity) {
        return isBoss(entity) ? ModConfig.getBossEssence() : ModConfig.getKillEssence();
    }

    /** Drops the essence tag, and the custom data with it once nothing else is stored there, so a
     * rune left behind by a split goes back to stacking with untouched ones. */
    private static void clearEssence(ItemStack stack) {
        var customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return;
        }
        var tag = customData.copyTag();
        tag.remove(ESSENCE_KEY);
        if (tag.isEmpty()) {
            stack.remove(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        } else {
            stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
        }
    }

    /**
     * Inventory slot holding the Blank Rune that should take the next essence, or -1 for none.
     *
     * <p>A rune already part way there wins outright. Essence is stored on the item stack, so a
     * slot holding several runes shares one counter between all of them — the player watches every
     * rune in the stack climb, then one transforms and the rest are left sitting at full with
     * nowhere to go. Filling them one at a time is the fix, and that means always coming back to
     * the rune in progress rather than splitting a fresh one off the pile on the next kill.
     *
     * <p>Failing that, a slot whose counter already reads full is taken ahead of an untouched one,
     * whether it holds one rune or a pile. Those are the leftovers from an older version of this,
     * stuck at full with nowhere for their essence to go, and they convert the moment they are
     * picked: clearing the backlog first is what gets a player out of that state, one rune per
     * essence gathered. A lone full rune is the commonest leftover of all — a stack of two under
     * the old code transformed one and stranded exactly one — so it must not be skipped.
     */
    private static int findChargingSlot(Player player) {
        int stranded = -1;
        int available = -1;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!(stack.getItem() instanceof BlankRuneItem)) {
                continue;
            }
            if (stack.getCount() == 1 && !isFull(stack) && getEssence(stack) > 0) {
                return i;
            }
            if (isFull(stack)) {
                if (stranded < 0) {
                    stranded = i;
                }
            } else if (available < 0) {
                available = i;
            }
        }
        return stranded >= 0 ? stranded : available;
    }

    /** First Blank Rune in the player's inventory able to take essence, or {@link ItemStack#EMPTY}. */
    public static ItemStack findFirstBlankRune(Player player) {
        int slot = findChargingSlot(player);
        return slot < 0 ? ItemStack.EMPTY : player.getInventory().getItem(slot);
    }

    /**
     * Adds essence to the one Blank Rune the player is currently filling, converting it if that
     * tops it up. A rune is peeled off its stack before it takes anything, so only ever one of them
     * is part charged and the rest stay untouched until their turn.
     */
    public static void depositEssence(Player player, int amount) {
        if (amount <= 0) {
            return;
        }
        int slot = findChargingSlot(player);
        if (slot < 0) {
            return;
        }
        ItemStack inSlot = player.getInventory().getItem(slot);
        ItemStack charging = inSlot;
        if (inSlot.getCount() > 1) {
            charging = inSlot.split(1);
            clearEssence(inSlot);
        }
        addEssence(charging, amount);
        if (isFull(charging)) {
            convertToSpellRune(player, charging);
        }
        if (charging != inSlot && !charging.isEmpty() && !player.getInventory().add(charging)) {
            player.drop(charging, false);
        }
    }

    public static ItemStack convertToSpellRune(Player player, ItemStack blankRune) {
        if (!isFull(blankRune)) {
            return blankRune;
        }

        ItemStack spellRune = new ItemStack(ModItems.SPELL_RUNE.get());
        blankRune.shrink(1);

        if (!player.getInventory().add(spellRune)) {
            player.drop(spellRune, false);
        }

        player.sendSystemMessage(Component.literal("Blank Rune gathered enough essence and transformed into a Spell Rune!").withStyle(ChatFormatting.GOLD));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);

        return blankRune;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltipAdder, TooltipFlag tooltipFlag) {
        int essence = getEssence(stack);
        int required = ModConfig.getEssenceRequired();
        tooltipAdder.accept(Component.literal("Essence: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(essence)).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal("/").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(String.valueOf(required)).withStyle(ChatFormatting.YELLOW)));
        if (essence < required) {
            tooltipAdder.accept(Component.literal("Kill mobs or cast spells to gather essence").withStyle(ChatFormatting.GRAY));
            tooltipAdder.accept(Component.literal("Bosses grant ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(String.valueOf(ModConfig.getBossEssence())).withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(" essence").withStyle(ChatFormatting.GRAY)));
        } else {
            tooltipAdder.accept(Component.literal("Ready to transform!").withStyle(ChatFormatting.GREEN));
        }
    }
}
