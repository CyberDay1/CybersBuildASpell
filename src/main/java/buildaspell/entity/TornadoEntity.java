package buildaspell.entity;

import buildaspell.registry.ModEntities;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.util.Mth;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class TornadoEntity extends Entity {
    private static final EntityDataAccessor<Float> DATA_RANGE =
            SynchedEntityData.defineId(TornadoEntity.class, EntityDataSerializers.FLOAT);

    // Transient per-entity catch timers (not persisted; re-tracked after reload).
    private final java.util.Map<UUID, Integer> caughtTicks = new java.util.HashMap<>();

    @Nullable
    private UUID casterId;
    private float range;
    private int duration;
    private int tickCount;
    private float pullStrength;
    private float liftStrength;
    private float spinStrength;
    private float spellPower;
    private boolean nullify;
    private int fortuneLevel;
    // Horizontal heading the funnel drifts along (caster's facing at cast time), normalized.
    private double moveX;
    private double moveZ;

    public TornadoEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public TornadoEntity(Level level, Player caster, Vec3 position,
                         float range, int duration, float pullStrength,
                         float liftStrength, float spinStrength,
                         float spellPower, boolean nullify, int fortuneLevel) {
        super(ModEntities.TORNADO.get(), level);
        this.casterId = caster.getUUID();
        this.setPos(position);
        this.range = range;
        this.duration = duration;
        this.pullStrength = pullStrength;
        this.liftStrength = liftStrength;
        this.spinStrength = spinStrength;
        this.spellPower = spellPower;
        this.nullify = nullify;
        this.fortuneLevel = fortuneLevel;
        // Drift along the caster's horizontal facing; a tornado that just sits still reads as a puff.
        Vec3 look = caster.getLookAngle();
        double len = Math.sqrt(look.x * look.x + look.z * look.z);
        if (len > 1.0e-4) {
            this.moveX = look.x / len;
            this.moveZ = look.z / len;
        }
        this.entityData.set(DATA_RANGE, range);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_RANGE, 5.0f);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) return;

        tickCount++;
        if (tickCount >= duration) {
            discard();
            return;
        }

        if (!(level() instanceof ServerLevel serverLevel)) return;

        // Drift the funnel forward and let it ride the terrain — climb hills and drop into valleys
        // rather than hovering at a fixed Y. Vertical change is capped per tick so a cliff doesn't
        // teleport it; horizontal motion only commits if the destination chunk is loaded.
        if (moveX != 0 || moveZ != 0) {
            double speed = buildaspell.config.ModConfig.comboDouble("tornado", "moveSpeed", 0.15);
            double nx = getX() + moveX * speed;
            double nz = getZ() + moveZ * speed;
            BlockPos ahead = BlockPos.containing(nx, getY(), nz);
            if (serverLevel.isLoaded(ahead)) {
                double climb = buildaspell.config.ModConfig.comboDouble("tornado", "climbSpeed", 0.5);
                int groundY = serverLevel.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(nx), Mth.floor(nz));
                double ny = getY() + Mth.clamp(groundY - getY(), -climb, climb);
                setPos(nx, ny, nz);
            }
        }

        // Suck entities toward the centre, hold them, then hurl them skyward once caught ~1s.
        java.util.Set<UUID> inRange = new java.util.HashSet<>();

        for (Entity entity : serverLevel.getEntities(this, getBoundingBox().inflate(range))) {
            if (entity == this) continue;
            if (casterId != null && entity.getUUID().equals(casterId)) continue;

            Vec3 toCenter = position().subtract(entity.position());
            double distance = toCenter.horizontalDistance();
            if (distance > range) continue;

            UUID id = entity.getUUID();
            inRange.add(id);
            int caught = caughtTicks.getOrDefault(id, 0) + 1;

            if (caught >= buildaspell.config.ModConfig.comboInt("tornado", "catchTicks", 20)) {
                // Launch upward (and slightly outward) then release the catch timer.
                double throwUp = 1.2 + liftStrength * 0.15;
                Vec3 outward = entity.position().subtract(position());
                Vec3 horiz = (outward.x * outward.x + outward.z * outward.z) > 1.0e-4
                        ? new Vec3(outward.x, 0, outward.z).normalize().scale(0.4)
                        : Vec3.ZERO;
                entity.setDeltaMovement(horiz.x, throwUp, horiz.z);
                entity.hurtMarked = true;
                caughtTicks.remove(id);
                continue;
            }

            caughtTicks.put(id, caught);

            // Horizontal suction toward the centre, stronger the closer they get.
            double factor = 1.0 - Math.min(distance, range) / range;
            Vec3 horizToCenter = new Vec3(toCenter.x, 0, toCenter.z);
            Vec3 pull = (horizToCenter.x * horizToCenter.x + horizToCenter.z * horizToCenter.z) > 1.0e-4
                    ? horizToCenter.normalize().scale(pullStrength * (0.15 + factor * 0.15))
                    : Vec3.ZERO;
            Vec3 tangent = new Vec3(-toCenter.z, 0, toCenter.x);
            Vec3 spin = (tangent.x * tangent.x + tangent.z * tangent.z) > 1.0e-4
                    ? tangent.normalize().scale(spinStrength * factor * 0.12)
                    : Vec3.ZERO;

            // Damp existing horizontal drift so the suction dominates; hover so they don't sink out.
            Vec3 motion = entity.getDeltaMovement();
            double y = Math.max(motion.y, -0.08) + 0.04;
            entity.setDeltaMovement(motion.x * 0.6 + pull.x + spin.x, y, motion.z * 0.6 + pull.z + spin.z);
            entity.hurtMarked = true;
            entity.fallDistance = 0;
        }

        // Anyone who left the funnel this tick loses their catch progress.
        caughtTicks.keySet().retainAll(inRange);

        // Damage on an interval
        if (!nullify && tickCount % buildaspell.config.ModConfig.comboInt("tornado", "damageIntervalTicks", 10) == 0) {
            float damageRadius = range * (float) buildaspell.config.ModConfig.comboDouble("tornado", "damageRadiusFraction", 0.5);
            float tornadoDamage = spellPower * (float) buildaspell.config.ModConfig.comboDouble("tornado", "damageScale", 0.2);
            for (Entity entity : serverLevel.getEntities(this, getBoundingBox().inflate(damageRadius))) {
                if (entity == this) continue;
                if (casterId != null && entity.getUUID().equals(casterId)) continue;
                if (fortuneLevel > 0) {
                    // Fortunate Son: kills by the funnel drop bonus loot, same as direct spell damage.
                    buildaspell.spell.SpellLootingTracker.setLootingLevel(entity.getUUID(), fortuneLevel);
                }
                entity.hurt(damageSources().magic(), tornadoDamage);
            }
        }

        // Sound
        if (tickCount % 15 == 0) {
            level().playSound(null, blockPosition(), SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 0.5f, 1.5f);
        }

        // Particles: a swirling vortex of cloud that widens toward the top, with debris near
        // the base, so the funnel reads as a spinning column of moving air rather than a static puff.
        double columnHeight = range * 1.4;
        double spinPhase = (tickCount % 30) / 30.0 * Math.PI * 2.0;
        int rings = buildaspell.config.ModConfig.scaledParticleCount(7);
        for (int ring = 0; ring < rings; ring++) {
            double frac = ring / (double) rings;
            double h = frac * columnHeight;
            double ringRadius = 0.5 + frac * range * 0.5; // narrow at the base, flares out up high
            int perRing = 3;
            for (int k = 0; k < perRing; k++) {
                double ang = spinPhase + ring * 0.7 + k * (Math.PI * 2.0 / perRing);
                double px = getX() + Math.cos(ang) * ringRadius;
                double pz = getZ() + Math.sin(ang) * ringRadius;
                double py = getY() + h;
                double tvx = -Math.sin(ang) * 0.5;
                double tvz = Math.cos(ang) * 0.5;
                serverLevel.sendParticles(ParticleTypes.CLOUD, px, py, pz, 0, tvx, 0.04, tvz, 1.0);
            }
        }
        // Ground debris kicked up and dragged into the swirl.
        int debris = buildaspell.config.ModConfig.scaledParticleCount(4);
        for (int k = 0; k < debris; k++) {
            double ang = spinPhase * 1.5 + k * (Math.PI * 0.5);
            double r = range * 0.45;
            double px = getX() + Math.cos(ang) * r;
            double pz = getZ() + Math.sin(ang) * r;
            serverLevel.sendParticles(ParticleTypes.POOF, px, getY() + 0.2, pz,
                    0, -Math.sin(ang) * 0.4, 0.12, Math.cos(ang) * 0.4, 1.0);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        input.getString("Caster").ifPresent(s -> {
            try { this.casterId = UUID.fromString(s); } catch (IllegalArgumentException ignored) {}
        });
        this.range = input.getFloatOr("Range", 5.0f);
        this.duration = input.getIntOr("Duration", 100);
        this.tickCount = input.getIntOr("TickCount", 0);
        this.pullStrength = input.getFloatOr("PullStrength", 1.0f);
        this.liftStrength = input.getFloatOr("LiftStrength", 0.5f);
        this.spinStrength = input.getFloatOr("SpinStrength", 0.5f);
        this.spellPower = input.getFloatOr("SpellPower", 10.0f);
        this.nullify = input.getBooleanOr("Nullify", false);
        this.fortuneLevel = input.getIntOr("FortuneLevel", 0);
        this.moveX = input.getDoubleOr("MoveX", 0.0);
        this.moveZ = input.getDoubleOr("MoveZ", 0.0);
        this.entityData.set(DATA_RANGE, range);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        if (casterId != null) output.putString("Caster", casterId.toString());
        output.putFloat("Range", range);
        output.putInt("Duration", duration);
        output.putInt("TickCount", tickCount);
        output.putFloat("PullStrength", pullStrength);
        output.putFloat("LiftStrength", liftStrength);
        output.putFloat("SpinStrength", spinStrength);
        output.putFloat("SpellPower", spellPower);
        output.putBoolean("Nullify", nullify);
        output.putInt("FortuneLevel", fortuneLevel);
        output.putDouble("MoveX", moveX);
        output.putDouble("MoveZ", moveZ);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    public float getRange() { return entityData.get(DATA_RANGE); }
    public int getTickCount() { return tickCount; }
    public int getDuration() { return duration; }

    /**
     * Client-advancing age for the render animation. The custom {@link #tickCount} lifetime counter is
     * only bumped after the server-side early-return in {@link #tick()}, so it stays frozen at 0 on the
     * client — driving the funnel model off it made a large tornado twitch instead of spin. The inherited
     * vanilla {@code Entity.tickCount} advances every tick on both sides, so the render clock uses that.
     */
    public int getRenderAge() { return super.tickCount; }
}
