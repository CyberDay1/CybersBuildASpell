package buildaspell.entity;

import buildaspell.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class BlackHoleEntity extends Entity {
    private static final EntityDataAccessor<Float> DATA_RANGE =
            SynchedEntityData.defineId(BlackHoleEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_MAX_LIFETIME =
            SynchedEntityData.defineId(BlackHoleEntity.class, EntityDataSerializers.INT);

    @Nullable
    private UUID casterUUID;
    private float range;
    private float pullStrength;
    private float damagePerTick;
    private boolean nullify;
    private int fortuneLevel;
    private int lifetime;
    private int maxLifetime;
    private boolean hasPlayedInitialSound;

    public BlackHoleEntity(EntityType<? extends BlackHoleEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public BlackHoleEntity(Level level, Player caster, Vec3 position,
                           float range, float pullStrength, float damagePerTick,
                           int duration, boolean nullify, int fortuneLevel) {
        super(ModEntities.BLACK_HOLE.get(), level);
        this.casterUUID = caster.getUUID();
        this.setPos(position);
        this.range = range;
        this.pullStrength = pullStrength;
        this.damagePerTick = damagePerTick;
        this.maxLifetime = duration;
        this.nullify = nullify;
        this.fortuneLevel = fortuneLevel;
        this.noPhysics = true;
        this.entityData.set(DATA_RANGE, range);
        this.entityData.set(DATA_MAX_LIFETIME, duration);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_RANGE, 5.0f);
        builder.define(DATA_MAX_LIFETIME, 100);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) return;

        if (!hasPlayedInitialSound) {
            level().playSound(null, blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 1.0f, 0.5f);
            hasPlayedInitialSound = true;
        }

        lifetime++;
        if (lifetime >= maxLifetime) {
            discard();
            return;
        }

        if (!(level() instanceof ServerLevel serverLevel)) return;

        // Pull entities in range. The caster is exempt: your own black hole never drags you in.
        for (Entity entity : serverLevel.getEntities(this, getBoundingBox().inflate(range))) {
            if (entity == this || (casterUUID != null && entity.getUUID().equals(casterUUID))) continue;
            Vec3 direction = position().subtract(entity.position());
            double distance = direction.length();
            if (distance < range && distance > 0.5) {
                double t = 1.0 - distance / range; // 0 at the edge → 1 at the core
                double scale = buildaspell.config.ModConfig.comboDouble("black_hole", "pullTickScale", 0.1);
                // Set velocity directly toward the core instead of nudging it. The old
                // additive pull (~0.05/tick) was always weaker than gravity (~0.08), so
                // entities below the core never rose (the "y-axis dependent" failure) and
                // ground mobs only crept in. A direct inward velocity with a floor reliably
                // overrides gravity/friction in every direction; it ramps up toward the core.
                double speed = Math.max(0.15, pullStrength * scale * (1.0 + 4.0 * t));
                Vec3 inward = direction.normalize().scale(speed);
                entity.setDeltaMovement(inward);
                entity.hurtMarked = true;
                entity.fallDistance = 0;
            }
        }

        // Damage entities in inner radius on an interval
        if (!nullify && lifetime % buildaspell.config.ModConfig.comboInt("black_hole", "damageIntervalTicks", 10) == 0) {
            float damageRadius = range * (float) buildaspell.config.ModConfig.comboDouble("black_hole", "damageRadiusFraction", 0.3);
            for (Entity entity : serverLevel.getEntities(this, getBoundingBox().inflate(damageRadius))) {
                if (entity == this || (casterUUID != null && entity.getUUID().equals(casterUUID))) continue;
                if (fortuneLevel > 0) {
                    // Fortunate Son: kills by the singularity drop bonus loot, same as direct spell damage.
                    buildaspell.spell.SpellLootingTracker.setLootingLevel(entity.getUUID(), fortuneLevel);
                }
                entity.hurt(damageSources().magic(), damagePerTick);
            }
        }

        // Ambient sound
        if (lifetime % 20 == 0) {
            level().playSound(null, blockPosition(), SoundEvents.PORTAL_AMBIENT, SoundSource.PLAYERS, 0.5f, 0.5f);
        }

        // Particles
        int bhParticles = buildaspell.config.ModConfig.scaledParticleCount(5);
        if (lifetime % 2 == 0 && bhParticles > 0) {
            serverLevel.sendParticles(ParticleTypes.PORTAL, getX(), getY(), getZ(),
                    bhParticles, range * 0.5, range * 0.5, range * 0.5, 0.05);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag input) {
        this.range = input.contains("Range") ? input.getFloat("Range") : 5.0f;
        this.pullStrength = input.contains("PullStrength") ? input.getFloat("PullStrength") : 1.0f;
        this.damagePerTick = input.contains("DamagePerTick") ? input.getFloat("DamagePerTick") : 2.0f;
        this.lifetime = input.contains("Lifetime") ? input.getInt("Lifetime") : 0;
        this.maxLifetime = input.contains("MaxLifetime") ? input.getInt("MaxLifetime") : 100;
        this.nullify = input.contains("Nullify") ? input.getBoolean("Nullify") : false;
        this.fortuneLevel = input.contains("FortuneLevel") ? input.getInt("FortuneLevel") : 0;
        this.hasPlayedInitialSound = input.contains("HasPlayedInitialSound") ? input.getBoolean("HasPlayedInitialSound") : false;
        this.entityData.set(DATA_RANGE, range);
        this.entityData.set(DATA_MAX_LIFETIME, maxLifetime);
        if (input.contains("CasterUUID")) {
            String s = input.getString("CasterUUID");
            try { this.casterUUID = UUID.fromString(s); } catch (IllegalArgumentException ignored) {}
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag output) {
        output.putFloat("Range", range);
        if (casterUUID != null) output.putString("CasterUUID", casterUUID.toString());
        output.putFloat("PullStrength", pullStrength);
        output.putFloat("DamagePerTick", damagePerTick);
        output.putInt("Lifetime", lifetime);
        output.putInt("MaxLifetime", maxLifetime);
        output.putBoolean("Nullify", nullify);
        output.putInt("FortuneLevel", fortuneLevel);
        output.putBoolean("HasPlayedInitialSound", hasPlayedInitialSound);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    public float getRange() { return entityData.get(DATA_RANGE); }
    public int getLifetime() { return lifetime; }
    public int getMaxLifetime() { return entityData.get(DATA_MAX_LIFETIME); }
}
