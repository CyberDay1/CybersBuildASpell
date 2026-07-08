package buildaspell.entity;

import buildaspell.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Lightning Storm combo: an invisible storm cloud that drifts horizontally and periodically drops a
 * real {@link LightningBolt} on the ground beneath it for the length of its lifetime. The cloud itself
 * is drawn entirely from server-sent smoke particles, so it uses a no-op renderer. Several of these are
 * spawned per cast (count scales with Increased Area), and their lifetime scales with Duration.
 */
public class StormCloudEntity extends Entity {
    @Nullable
    private UUID casterUUID;
    private double driftX;
    private double driftZ;
    private int lifetime;
    private int maxLifetime = 200;
    private int strikeInterval = 30;
    private double strikeRadius = 3.0;
    private boolean nullify;

    public StormCloudEntity(EntityType<? extends StormCloudEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public StormCloudEntity(Level level, @Nullable Player caster, Vec3 position,
                            double driftX, double driftZ, int duration,
                            int strikeInterval, double strikeRadius, boolean nullify) {
        super(ModEntities.STORM_CLOUD.get(), level);
        this.casterUUID = caster != null ? caster.getUUID() : null;
        this.setPos(position);
        this.driftX = driftX;
        this.driftZ = driftZ;
        this.maxLifetime = duration;
        this.strikeInterval = Math.max(1, strikeInterval);
        this.strikeRadius = strikeRadius;
        this.nullify = nullify;
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel serverLevel)) return;

        lifetime++;
        if (lifetime >= maxLifetime) {
            discard();
            return;
        }

        // Drift the cloud across the sky.
        setPos(getX() + driftX, getY(), getZ() + driftZ);

        spawnCloudParticles(serverLevel);

        // Drop a bolt on the ground under a random point beneath the cloud.
        if (lifetime % strikeInterval == 0) {
            var rand = serverLevel.getRandom();
            double ang = rand.nextDouble() * Math.PI * 2;
            double r = Math.sqrt(rand.nextDouble()) * strikeRadius;
            int sx = (int) Math.floor(getX() + Math.cos(ang) * r);
            int sz = (int) Math.floor(getZ() + Math.sin(ang) * r);
            int sy = serverLevel.getHeight(Heightmap.Types.MOTION_BLOCKING, sx, sz);
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(serverLevel, EntitySpawnReason.TRIGGERED);
            if (bolt != null) {
                bolt.snapTo(sx + 0.5, sy, sz + 0.5);
                bolt.setCause(getCaster() instanceof ServerPlayer sp ? sp : null);
                // Nullify keeps the storm as pure spectacle: full bolts, zero damage/fire.
                bolt.setVisualOnly(nullify);
                serverLevel.addFreshEntity(bolt);
                buildaspell.spell.execution.LightningInteractions.onStrike(serverLevel, sx + 0.5, sz + 0.5, nullify);
            }
        }

        if (lifetime % 40 == 0) {
            level().playSound(null, getX(), getY(), getZ(),
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 1.2f, 0.9f);
        }
    }

    @Nullable
    private Player getCaster() {
        return casterUUID != null ? level().getPlayerByUUID(casterUUID) : null;
    }

    private void spawnCloudParticles(ServerLevel level) {
        var rand = level.getRandom();
        int puffs = buildaspell.config.ModConfig.scaledParticleCount(24);
        for (int i = 0; i < puffs; i++) {
            double ox = (rand.nextDouble() * 2 - 1) * strikeRadius;
            double oz = (rand.nextDouble() * 2 - 1) * strikeRadius;
            double oy = rand.nextDouble() * 1.2;
            level.sendParticles(ParticleTypes.SMOKE,
                    getX() + ox, getY() + oy, getZ() + oz,
                    0, driftX * 4, -0.01, driftZ * 4, 0.4);
        }
        // Occasional electric crackle glints in the cloud body.
        if (rand.nextInt(3) == 0) {
            double ox = (rand.nextDouble() * 2 - 1) * strikeRadius;
            double oz = (rand.nextDouble() * 2 - 1) * strikeRadius;
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    getX() + ox, getY(), getZ() + oz,
                    2, 0.3, 0.1, 0.3, 0.0);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.driftX = input.getDoubleOr("DriftX", 0.0);
        this.driftZ = input.getDoubleOr("DriftZ", 0.0);
        this.lifetime = input.getIntOr("Lifetime", 0);
        this.maxLifetime = input.getIntOr("MaxLifetime", 200);
        this.strikeInterval = Math.max(1, input.getIntOr("StrikeInterval", 30));
        this.strikeRadius = input.getDoubleOr("StrikeRadius", 3.0);
        this.nullify = input.getBooleanOr("Nullify", false);
        input.getString("CasterUUID").ifPresent(s -> {
            try { this.casterUUID = UUID.fromString(s); } catch (IllegalArgumentException ignored) {}
        });
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putDouble("DriftX", driftX);
        output.putDouble("DriftZ", driftZ);
        output.putInt("Lifetime", lifetime);
        output.putInt("MaxLifetime", maxLifetime);
        output.putInt("StrikeInterval", strikeInterval);
        output.putDouble("StrikeRadius", strikeRadius);
        output.putBoolean("Nullify", nullify);
        if (casterUUID != null) output.putString("CasterUUID", casterUUID.toString());
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }
}
