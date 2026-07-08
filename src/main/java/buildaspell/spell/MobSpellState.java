package buildaspell.spell;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

/**
 * Spell-applied state carried on a mob's persistent NBT: which player summoned it (so it never
 * turns on its own summoner), when a summoned mob expires and despawns, and how long a Charmed mob
 * stays pacified. Stored in persistent data so it survives chunk unloads and world restarts; the
 * event handlers in {@link buildaspell.events.ServerEvents} enforce it every tick.
 */
public final class MobSpellState {

    private static final String SUMMONER_KEY = "buildaspell:summoner";
    private static final String SUMMON_EXPIRY_KEY = "buildaspell:summon_expiry";
    private static final String PACIFIED_UNTIL_KEY = "buildaspell:pacified_until";

    private MobSpellState() {}

    /**
     * Marks {@code mob} as a spell summon of {@code summoner}. A positive {@code lifetimeTicks}
     * schedules the mob to despawn that many ticks from now; pass 0 for mobs that manage their own
     * lifetime (Vexes use vanilla limited life).
     */
    public static void tagSummon(Mob mob, Player summoner, int lifetimeTicks) {
        CompoundTag data = mob.getPersistentData();
        data.putString(SUMMONER_KEY, summoner.getUUID().toString());
        if (lifetimeTicks > 0) {
            data.putLong(SUMMON_EXPIRY_KEY, mob.level().getGameTime() + lifetimeTicks);
        }
    }

    /** @return true if this mob is a spell summon whose lifetime has run out. */
    public static boolean isExpiredSummon(Mob mob) {
        CompoundTag data = mob.getPersistentData();
        long expiry = data.getLongOr(SUMMON_EXPIRY_KEY, 0L);
        return expiry > 0L && mob.level().getGameTime() > expiry;
    }

    /** @return true if {@code target} is the player who summoned this mob. */
    public static boolean isSummonerOf(Mob mob, LivingEntity target) {
        CompoundTag data = mob.getPersistentData();
        return data.getString(SUMMONER_KEY)
                .map(id -> id.equals(target.getUUID().toString()))
                .orElse(false);
    }

    /** Pacifies the mob for {@code durationTicks}: it drops its current target and won't pick a new one. */
    public static void pacify(Mob mob, int durationTicks) {
        mob.getPersistentData().putLong(PACIFIED_UNTIL_KEY, mob.level().getGameTime() + durationTicks);
        mob.setTarget(null);
        mob.setLastHurtByMob(null);
    }

    /** @return true while a Charm-applied pacify window is still active on this mob. */
    public static boolean isPacified(Mob mob) {
        CompoundTag data = mob.getPersistentData();
        long until = data.getLongOr(PACIFIED_UNTIL_KEY, 0L);
        return until > 0L && mob.level().getGameTime() < until;
    }
}
