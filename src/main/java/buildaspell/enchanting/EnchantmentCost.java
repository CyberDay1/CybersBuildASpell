package buildaspell.enchanting;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public record EnchantmentCost(int levels, Item item, int itemCount) {

    public boolean canAfford(int playerLevels, ItemStack itemStack) {
        return playerLevels >= levels
                && itemStack.getItem() == item
                && itemStack.getCount() >= itemCount;
    }

    public void consume(Player player, ItemStack itemStack) {
        player.giveExperienceLevels(-levels);
        itemStack.shrink(itemCount);
    }
}
