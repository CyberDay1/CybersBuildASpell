package buildaspell.events;

import buildaspell.BuildASpell;
import buildaspell.config.ModConfig;
import buildaspell.spell.SpellLootingTracker;
import buildaspell.spell.SpellModifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.enchanting.GetEnchantmentLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Fortunate Son's creature half: it lends the killer a Looting level for the duration of the
 * victim's death loot roll.
 *
 * <p>It used to grow the drops after the fact, which is not what Looting does. Vanilla Looting only
 * feeds the {@code enchanted_count_increase} entries a loot table chooses to declare, so guaranteed
 * drops — a Wither's nether star, a boss's trophy, a mob's own equipment — are untouched by it.
 * Growing the finished pile multiplied those too, which is where the duplicated boss drops came
 * from. Now the level is injected into the roll itself and vanilla decides what it applies to.
 */
@EventBusSubscriber(modid = BuildASpell.MOD_ID)
public class SpellLootingEvents {

    /**
     * The Looting level in force for the death currently being resolved, or 0 when no spell-killed
     * loot roll is running. Opened by {@link #onLivingDeath} and closed by {@link #onLivingDrops};
     * vanilla runs both inside one {@code die()} call, so the window never spans a tick.
     */
    private static int activeLootingLevel = 0;

    /**
     * The thread that opened the window. {@link GetEnchantmentLevelEvent} is a general-purpose hook
     * that any thread may fire, and this pins our answer to the server thread that is mid-roll.
     */
    private static Thread activeLootingThread;

    /** The caster we lent a hand to, and the stack we put there, so both can be taken back. */
    @Nullable private static Player lentHandTo;
    @Nullable private static ItemStack lentHandStack;

    /**
     * Runs last so a death another mod cancels never opens a window: a cancelled event stops
     * reaching handlers, and no cancelled death rolls any loot.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        // A previous death that never reached its drops would otherwise leave the window open, so
        // every death starts by shutting it.
        closeLootingWindow();

        LivingEntity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel)) {
            return;
        }

        int lootingLevel = SpellLootingTracker.getLootingLevel(entity.getUUID());
        SpellLootingTracker.clearLootingLevel(entity.getUUID());
        if (lootingLevel <= 0) {
            return;
        }

        // A player's death drops are their inventory rather than a rolled loot table, so there is
        // nothing here for Looting to act on.
        if (entity instanceof Player) {
            return;
        }

        if (event.getSource() == null || event.getSource().getEntity() == null) {
            return;
        }

        int maxLevel = ModConfig.modifierInt(SpellModifier.FORTUNATE_SON, "maxLevel", 25);
        int effectiveLevel = Math.min(lootingLevel, maxLevel);
        if (effectiveLevel <= 0) {
            return;
        }

        activeLootingLevel = effectiveLevel;
        activeLootingThread = Thread.currentThread();

        // Looting is read from the killer's main hand and nowhere else, and vanilla walks only the
        // slots that actually hold something — so a caster with an empty hand is never asked about
        // the enchantment at all and the spell would quietly do nothing. Lend them a hand for the
        // length of the death and take it straight back below. The stack itself carries no
        // enchantment: it exists so vanilla's walk has something to ask about, and the level comes
        // from the same place it does for a caster holding a real weapon.
        if (event.getSource().getEntity() instanceof Player killer && killer.getMainHandItem().isEmpty()) {
            ItemStack lent = new ItemStack(Items.STICK);
            // Safe to write directly: a player emits the equip game event for armour only, and a
            // stick is not equippable, so this makes no sound, shakes no sculk and syncs nothing.
            // The hand is restored inside the same tick, before any of it could be observed.
            killer.setItemSlot(EquipmentSlot.MAINHAND, lent);
            lentHandTo = killer;
            lentHandStack = lent;
        }
    }

    @SubscribeEvent
    public static void onGetEnchantmentLevel(GetEnchantmentLevelEvent event) {
        if (activeLootingLevel <= 0 || Thread.currentThread() != activeLootingThread) {
            return;
        }
        if (!event.isTargetting(Enchantments.LOOTING)) {
            return;
        }
        // upgrade() takes the higher of the two, so a killer already carrying a better Looting
        // weapon keeps it and Fortunate Son can never push the level past its own cap.
        event.getHolder(Enchantments.LOOTING)
                .ifPresent(holder -> event.getEnchantments().upgrade(holder, activeLootingLevel));
    }

    /** Runs last so the level is still in force for anything else reading this entity's drops. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDrops(LivingDropsEvent event) {
        closeLootingWindow();
    }

    /** Safety net called from the end of each server tick: the window is never meant to outlive one. */
    public static void closeLootingWindow() {
        if (lentHandTo != null) {
            // Only take back the stack we actually lent. If anything else has since filled the hand,
            // that is the player's item and clearing it would destroy it.
            if (lentHandTo.getMainHandItem() == lentHandStack) {
                lentHandTo.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            }
            lentHandTo = null;
            lentHandStack = null;
        }
        activeLootingLevel = 0;
        activeLootingThread = null;
    }
}
