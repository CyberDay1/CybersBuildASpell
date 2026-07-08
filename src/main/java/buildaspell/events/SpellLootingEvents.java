package buildaspell.events;

import buildaspell.BuildASpell;
import buildaspell.config.ModConfig;
import buildaspell.spell.SpellLootingTracker;
import buildaspell.spell.SpellModifier;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

@EventBusSubscriber(modid = BuildASpell.MOD_ID)
public class SpellLootingEvents {

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        int lootingLevel = SpellLootingTracker.getLootingLevel(entity.getUUID());

        if (lootingLevel <= 0) {
            return;
        }

        // Consume the tracked level up front: every path below is a single use of it.
        SpellLootingTracker.clearLootingLevel(entity.getUUID());

        // A player's death drops are their inventory, not a loot table. Vanilla Looting only ever
        // multiplies loot-table rolls, so growing these stacks duplicated whatever the victim was
        // carrying on every kill.
        if (entity instanceof Player) {
            return;
        }

        DamageSource source = event.getSource();
        if (source == null || source.getEntity() == null) {
            return;
        }

        int maxLevel = ModConfig.modifierInt(SpellModifier.FORTUNATE_SON, "maxLevel", 3);
        int effectiveLevel = Math.min(lootingLevel, maxLevel);
        if (effectiveLevel <= 0) {
            return;
        }

        for (ItemEntity drop : event.getDrops()) {
            ItemStack stack = drop.getItem();
            if (stack.isEmpty()) {
                continue;
            }
            // Never grow past the item's own limit. Unstackable gear has a max of 1, so this also
            // stops a mob's equipment drop becoming an illegal stack of two swords.
            int maxCount = stack.getMaxStackSize();
            if (stack.getCount() >= maxCount) {
                continue;
            }
            int bonusDrops = entity.level().getRandom().nextInt(effectiveLevel + 1);
            if (bonusDrops > 0) {
                stack.setCount(Math.min(maxCount, stack.getCount() + bonusDrops));
            }
        }
    }
}
