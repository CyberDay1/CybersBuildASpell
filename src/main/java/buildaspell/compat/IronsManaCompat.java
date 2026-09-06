package buildaspell.compat;

import buildaspell.config.ModConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;

/**
 * Optional integration with Iron's Spells 'n Spellbooks (mod id: "irons_spellbooks").
 *
 * <p>Iron's keeps its own mana pool, and two spell mods each running a pool means two bars, two
 * numbers and two unrelated regen curves for the same player. Gear that grants
 * {@code irons_spellbooks:max_mana} makes the split worse than cosmetic: Iron's regen is a
 * percentage of the pool, buildaspell's is flat, so the same gear moves one caster's refill rate
 * and pins the other's. Turning on {@code deferManaToIronsSpellbooks} stops buildaspell keeping a
 * pool of its own and spends out of Iron's instead. It is off by default, so installing Iron's
 * alongside buildaspell changes nothing until a pack author asks for it.
 *
 * <p>Every method that touches an Iron's class is only reached after {@link #isLoaded()}, so the
 * JVM never resolves those classes on a standalone install.
 */
public final class IronsManaCompat {

    private IronsManaCompat() {}

    /**
     * Iron's regenerates 1% of the pool every {@code MagicManager.MANA_REGEN_TICKS} (10) ticks, so
     * the rate is 2% of maximum mana per second before its {@code manaRegenMultiplier} server
     * config, which lives outside the public API and is assumed to be its default 1.0.
     */
    private static final float IRONS_REGEN_FRACTION_PER_SECOND = 0.02f;

    /**
     * True only when Iron's is loaded AND ships the API classes this compat was compiled against.
     * A plain mod-presence check isn't enough: if the class is missing or has moved, touching it
     * throws {@link NoClassDefFoundError}, which is an {@link Error} rather than an Exception and
     * takes the server tick down with it. Probing once here keeps every deferral path off unless
     * the API is really there.
     */
    private static final boolean API_PRESENT = probeApi();

    private static boolean probeApi() {
        try {
            ClassLoader loader = IronsManaCompat.class.getClassLoader();
            Class.forName("io.redspace.ironsspellbooks.api.magic.MagicData", false, loader);
            Class.forName("io.redspace.ironsspellbooks.api.registry.AttributeRegistry", false, loader);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /** Returns true when Iron's Spellbooks is loaded and its expected API is available. */
    public static boolean isLoaded() {
        return ModList.get().isLoaded("irons_spellbooks") && API_PRESENT;
    }

    /**
     * Whether buildaspell should read and write Iron's mana instead of its own. This is the check
     * every mana path should use — {@link #isLoaded()} alone ignores the pack author's opt-out.
     */
    public static boolean isDeferring() {
        return isLoaded() && ModConfig.deferManaToIronsSpellbooks();
    }

    /**
     * Only call when {@link #isDeferring()} is true.
     */
    public static float getMana(Player player) {
        return MagicData.getPlayerMagicData(player).getMana();
    }

    /**
     * Writes Iron's mana. Server side only: Iron's {@code setMana} skips its clamp and its
     * cancellable event when the {@code MagicData} has no server player behind it, and it syncs the
     * authoritative figure back down within ten ticks anyway.
     *
     * <p>Only call when {@link #isDeferring()} is true.
     */
    public static void setMana(Player player, float mana) {
        if (player.level().isClientSide()) {
            return;
        }
        MagicData.getPlayerMagicData(player).setMana(Math.max(0f, mana));
    }

    /**
     * Iron's maximum mana. buildaspell's own Mana Pool enchantment is deliberately left out: Iron's
     * clamps every write to this attribute, so a larger figure here would only draw a bar that can
     * never fill. That is why the Arcane Altar stops offering it while deferral is on — see
     * {@code ArcaneAltarBlockEntity.isEnchantmentOffered}.
     *
     * <p>Only call when {@link #isDeferring()} is true.
     */
    public static float getMaxMana(Player player) {
        return (float) player.getAttributeValue(AttributeRegistry.MAX_MANA);
    }

    /**
     * Iron's regen expressed as flat mana per second, so it can stand in for buildaspell's own
     * figure. Iron's own attribute is a multiplier on a proportional rate rather than a rate, hence
     * the conversion through the pool size — see {@link #IRONS_REGEN_FRACTION_PER_SECOND}.
     *
     * <p>Only call when {@link #isDeferring()} is true.
     */
    public static float getManaRegenPerSecond(Player player) {
        float regenMultiplier = (float) player.getAttributeValue(AttributeRegistry.MANA_REGEN);
        return getMaxMana(player) * regenMultiplier * IRONS_REGEN_FRACTION_PER_SECOND;
    }
}
