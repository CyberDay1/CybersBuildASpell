package buildaspell.client.gui;

import buildaspell.spell.DeliveryMethod;
import buildaspell.spell.SpellEffect;
import buildaspell.spell.SpellModifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Maps each spell component to a vanilla item icon, rendered in the builder via
 * {@code GuiGraphicsExtractor#item}. This is the single seam for component art:
 * to swap any component to a bespoke sprite later, only this class changes.
 */
public final class ComponentIcons {
    private ComponentIcons() {}

    private static final ItemStack FALLBACK = new ItemStack(Items.PAPER);

    public static ItemStack forComponent(Object component) {
        if (component instanceof DeliveryMethod dm) return forDelivery(dm);
        if (component instanceof SpellEffect se) return forEffect(se);
        if (component instanceof SpellModifier sm) return forModifier(sm);
        if (component instanceof DatapackEffect de) return forDatapackEffect(de);
        return FALLBACK;
    }

    /** Resolve a datapack effect's display icon (an item id) to a stack, falling back to paper. */
    private static ItemStack forDatapackEffect(DatapackEffect de) {
        return de.display().icon()
                .map(BuiltInRegistries.ITEM::getValue)
                .map(ItemStack::new)
                .filter(s -> !s.isEmpty())
                .orElse(FALLBACK);
    }

    private static ItemStack stack(Item item) {
        return new ItemStack(item);
    }

    public static ItemStack forDelivery(DeliveryMethod d) {
        return stack(switch (d) {
            case RUNE -> Items.AMETHYST_SHARD;
            case SIGHT -> Items.ENDER_EYE;
            case SELF -> Items.TOTEM_OF_UNDYING;
            case CAST -> Items.BLAZE_ROD;
            case TRACKING -> Items.COMPASS;
            case TOUCH -> Items.GOLDEN_SWORD;
            case TRAP -> Items.TRIPWIRE_HOOK;
        });
    }

    public static ItemStack forEffect(SpellEffect e) {
        return stack(switch (e) {
            case DAMAGE -> Items.IRON_SWORD;
            case IGNITE -> Items.FIRE_CHARGE;
            case FREEZE -> Items.PACKED_ICE;
            case TELEPORT -> Items.ENDER_PEARL;
            case PULL -> Items.FISHING_ROD;
            case PUSH -> Items.PISTON;
            case YEET -> Items.SLIME_BLOCK;
            case REAP -> Items.NETHERITE_HOE;
            case EXPLOSION -> Items.TNT;
            case HEAL -> Items.GOLDEN_APPLE;
            case LIGHTNING -> Items.LIGHTNING_ROD;
            case POISON -> Items.SPIDER_EYE;
            case WITHER -> Items.WITHER_SKELETON_SKULL;
            case SATURATION -> Items.COOKED_BEEF;
            case LAUNCH -> Items.FIREWORK_ROCKET;
            case LIGHT -> Items.GLOWSTONE;
            case SLAM -> Items.MACE;
            case LEVITATION -> Items.SHULKER_SHELL;
            case SLOW_FALL -> Items.PHANTOM_MEMBRANE;
            case BREAK -> Items.IRON_PICKAXE;
            case INVISIBILITY -> Items.FERMENTED_SPIDER_EYE;
            case SPEED -> Items.SUGAR;
            case HASTE -> Items.GOLDEN_PICKAXE;
            case BLINK -> Items.CHORUS_FRUIT;
            case SWAP -> Items.PLAYER_HEAD;
            case SUMMON -> Items.ZOMBIE_HEAD;
            case CREATE_WATER -> Items.WATER_BUCKET;
            case EVAPORATE_WATER -> Items.SPONGE;
            case MARK -> Items.TARGET;
            case RECALL -> Items.RECOVERY_COMPASS;
            case PICKUP -> Items.HOPPER;
            case SHIELD -> Items.SHIELD;
            case CONJURE -> Items.SCAFFOLDING;
            case GROWTH -> Items.BONE_MEAL;
            case CLEANSE -> Items.MILK_BUCKET;
            case CHARM -> Items.GOLD_INGOT;
            case BLIND -> Items.INK_SAC;
            case SLOW -> Items.SOUL_SAND;
            case WEAKEN -> Items.WOODEN_SWORD;
            case STRENGTHEN -> Items.BLAZE_POWDER;
            case REGENERATE -> Items.GHAST_TEAR;
            case RESIST -> Items.NETHERITE_CHESTPLATE;
            case NIGHT_VISION -> Items.GOLDEN_CARROT;
            case WATER_BREATHING -> Items.PUFFERFISH;
            case ROOT -> Items.COBWEB;
            case GRAPPLE -> Items.TRIPWIRE_HOOK;
            case GUST -> Items.WIND_CHARGE;
        });
    }

    public static ItemStack forModifier(SpellModifier m) {
        return stack(switch (m) {
            case INCREASED_AREA -> Items.FIREWORK_STAR;
            case INCREASED_POWER -> Items.BLAZE_POWDER;
            case NULLIFY -> Items.BARRIER;
            case GENTLENESS -> Items.FEATHER;
            case FORTUNATE_SON -> Items.RABBIT_FOOT;
            case DOUBLE -> Items.AMETHYST_CLUSTER;
            case ECHO -> Items.ECHO_SHARD;
            case PROLONGED -> Items.CLOCK;
            case DELAY -> Items.REPEATER;
            case CHAIN -> Items.LEAD;
            case PIERCE -> Items.ARROW;
            case BOUNCE -> Items.SLIME_BALL;
            case ACCELERATE -> Items.GLOWSTONE_DUST;
            case DURATION -> Items.REDSTONE;
            case LINGER -> Items.DRAGON_BREATH;
            case SPLIT -> Items.SHEARS;
            case WALL -> Items.BRICKS;
            case FLOOR -> Items.SMOOTH_STONE;
            case FILL -> Items.COBBLESTONE;
            case LEECH -> Items.GHAST_TEAR;
            case SUNDER -> Items.NETHERITE_AXE;
            case RETURN -> Items.ENDER_PEARL;
        });
    }
}
