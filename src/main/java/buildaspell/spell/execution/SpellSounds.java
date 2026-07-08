package buildaspell.spell.execution;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class SpellSounds {
    private static final RandomSource RANDOM = RandomSource.create();

    public static void play(Level level, Vec3 pos, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, pos.x, pos.y, pos.z, sound, SoundSource.PLAYERS, volume, pitch);
    }

    public static void playWithVariation(Level level, Vec3 pos, SoundEvent sound, float volume, float basePitch, float variation) {
        float pitch = basePitch + (RANDOM.nextFloat() - 0.5f) * variation * 2;
        play(level, pos, sound, volume, pitch);
    }

    public static void playLayered(Level level, Vec3 pos, SoundEvent primary, SoundEvent secondary, float volume) {
        playWithVariation(level, pos, primary, volume, 1.0f, 0.1f);
        playWithVariation(level, pos, secondary, volume * 0.6f, 1.2f, 0.15f);
    }

    public static void damage(Level level, Vec3 pos) {
        playWithVariation(level, pos, SoundEvents.PLAYER_ATTACK_SWEEP, 0.6f, 1.2f, 0.2f);
        playWithVariation(level, pos, SoundEvents.PLAYER_ATTACK_CRIT, 0.4f, 1.4f, 0.15f);
    }

    public static void heal(Level level, Vec3 pos) {
        playLayered(level, pos, SoundEvents.PLAYER_LEVELUP, SoundEvents.EXPERIENCE_ORB_PICKUP, 0.5f);
    }

    public static void teleport(Level level, Vec3 pos) {
        playWithVariation(level, pos, SoundEvents.ENDERMAN_TELEPORT, 1.0f, 1.0f, 0.1f);
    }

    public static void teleportArrival(Level level, Vec3 pos) {
        playWithVariation(level, pos, SoundEvents.ENDERMAN_TELEPORT, 0.8f, 1.1f, 0.1f);
        playWithVariation(level, pos, SoundEvents.CHORUS_FRUIT_TELEPORT, 0.5f, 1.0f, 0.15f);
    }

    public static void freeze(Level level, Vec3 pos) {
        playLayered(level, pos, SoundEvents.GLASS_BREAK, SoundEvents.POWDER_SNOW_STEP, 0.6f);
    }

    public static void ignite(Level level, Vec3 pos) {
        playLayered(level, pos, SoundEvents.FIRECHARGE_USE, SoundEvents.BLAZE_SHOOT, 0.7f);
    }

    public static void lightning(Level level, Vec3 pos) {
        playWithVariation(level, pos, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.8f, 1.0f, 0.2f);
    }

    public static void lightningCharge(Level level, Vec3 pos) {
        playWithVariation(level, pos, SoundEvents.BEACON_ACTIVATE, 0.4f, 1.8f, 0.1f);
    }

    public static void pull(Level level, Vec3 pos) {
        playWithVariation(level, pos, SoundEvents.ENDER_EYE_LAUNCH, 0.5f, 0.6f, 0.15f);
    }

    public static void push(Level level, Vec3 pos) {
        playWithVariation(level, pos, SoundEvents.ENDER_EYE_LAUNCH, 0.5f, 1.4f, 0.15f);
    }

    public static void explosion(Level level, Vec3 pos, float power) {
        float pitch = 1.0f - (power * 0.05f);
        playWithVariation(level, pos, SoundEvents.GENERIC_EXPLODE.value(), Math.min(power * 0.3f, 1.0f), pitch, 0.1f);
    }

    public static void poison(Level level, Vec3 pos) {
        playWithVariation(level, pos, SoundEvents.BREWING_STAND_BREW, 0.5f, 1.3f, 0.2f);
    }

    public static void wither(Level level, Vec3 pos) {
        playLayered(level, pos, SoundEvents.WITHER_AMBIENT, SoundEvents.SOUL_ESCAPE.value(), 0.4f);
    }

    public static void summon(Level level, Vec3 pos) {
        playLayered(level, pos, SoundEvents.EVOKER_PREPARE_SUMMON, SoundEvents.EVOKER_CAST_SPELL, 0.7f);
    }

    public static void runeCharge(Level level, Vec3 pos, float progress) {
        float pitch = 0.5f + progress * 1.0f;
        playWithVariation(level, pos, SoundEvents.BEACON_AMBIENT, 0.3f * progress, pitch, 0.05f);
    }

    public static void runeActivate(Level level, Vec3 pos) {
        playLayered(level, pos, SoundEvents.BEACON_ACTIVATE, SoundEvents.ENCHANTMENT_TABLE_USE, 0.8f);
    }

    public static void projectileImpact(Level level, Vec3 pos) {
        playLayered(level, pos, SoundEvents.PLAYER_ATTACK_CRIT, SoundEvents.AMETHYST_BLOCK_BREAK, 0.6f);
    }

    public static void chain(Level level, Vec3 pos) {
        playWithVariation(level, pos, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.4f, 1.5f, 0.2f);
    }

    public static void blink(Level level, Vec3 pos) {
        playWithVariation(level, pos, SoundEvents.SHULKER_TELEPORT, 0.6f, 1.2f, 0.15f);
    }

    public static void shield(Level level, Vec3 pos) {
        playLayered(level, pos, SoundEvents.SHIELD_BLOCK, SoundEvents.ENCHANTMENT_TABLE_USE, 0.6f);
    }

    public static void launch(Level level, Vec3 pos) {
        playLayered(level, pos, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundEvents.ENDER_DRAGON_FLAP, 0.6f);
    }

    public static void yeet(Level level, Vec3 pos) {
        playLayered(level, pos, SoundEvents.WITHER_SHOOT, SoundEvents.PHANTOM_FLAP, 0.6f);
    }

    public static void slam(Level level, Vec3 pos) {
        playLayered(level, pos, SoundEvents.ANVIL_LAND, SoundEvents.GENERIC_EXPLODE.value(), 0.5f);
    }
}
