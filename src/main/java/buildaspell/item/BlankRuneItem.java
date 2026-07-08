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

import java.util.List;

public class BlankRuneItem extends Item {
    // Legacy NBT tag name kept stable so in-progress runes survive the souls->essence rename.
    private static final String ESSENCE_KEY = "SoulCount";

    public BlankRuneItem(Properties properties) {
        super(properties);
    }

    public static int getEssence(ItemStack stack) {
        return stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY)
                .copyTag().getInt(ESSENCE_KEY);
    }

    public static void addEssence(ItemStack stack, int amount) {
        var customData = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY);
        var tag = customData.copyTag();
        int current = tag.getInt(ESSENCE_KEY);
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

    /** First non-full Blank Rune in the player's inventory, or {@link ItemStack#EMPTY}. */
    public static ItemStack findFirstBlankRune(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof BlankRuneItem && !isFull(stack)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /** Adds essence to the player's first non-full Blank Rune, converting it if it fills up. */
    public static void depositEssence(Player player, int amount) {
        if (amount <= 0) {
            return;
        }
        ItemStack blankRune = findFirstBlankRune(player);
        if (blankRune.isEmpty()) {
            return;
        }
        addEssence(blankRune, amount);
        if (isFull(blankRune)) {
            convertToSpellRune(player, blankRune);
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
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        int essence = getEssence(stack);
        int required = ModConfig.getEssenceRequired();
        tooltipComponents.add(Component.literal("Essence: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(essence)).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal("/").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(String.valueOf(required)).withStyle(ChatFormatting.YELLOW)));
        if (essence < required) {
            tooltipComponents.add(Component.literal("Kill mobs or cast spells to gather essence").withStyle(ChatFormatting.GRAY));
            tooltipComponents.add(Component.literal("Bosses grant ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(String.valueOf(ModConfig.getBossEssence())).withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(" essence").withStyle(ChatFormatting.GRAY)));
        } else {
            tooltipComponents.add(Component.literal("Ready to transform!").withStyle(ChatFormatting.GREEN));
        }
    }
}
