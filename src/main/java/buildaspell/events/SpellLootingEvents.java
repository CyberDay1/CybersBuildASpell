package buildaspell.events;

import buildaspell.BuildASpell;
import buildaspell.spell.SpellLootingTracker;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
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

        if (lootingLevel > 0) {
            DamageSource source = event.getSource();
            if (source != null && source.getEntity() != null) {
                for (ItemEntity drop : event.getDrops()) {
                    ItemStack stack = drop.getItem();
                    if (!stack.isEmpty() && stack.getCount() > 0) {
                        int bonusDrops = entity.level().getRandom().nextInt(lootingLevel + 1);
                        if (bonusDrops > 0) {
                            stack.grow(bonusDrops);
                        }
                    }
                }
            }

            SpellLootingTracker.clearLootingLevel(entity.getUUID());
        }
    }
}
