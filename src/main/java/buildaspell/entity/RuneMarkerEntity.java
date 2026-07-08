package buildaspell.entity;

import buildaspell.registry.ModEntities;
import buildaspell.spell.Spell;
import buildaspell.spell.execution.SpellExecutor;
import net.minecraft.core.Direction;
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
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class RuneMarkerEntity extends Entity {
    private static final EntityDataAccessor<Float> DATA_CHARGE_PROGRESS =
            SynchedEntityData.defineId(RuneMarkerEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_IS_TRAP =
            SynchedEntityData.defineId(RuneMarkerEntity.class, EntityDataSerializers.BOOLEAN);

    @Nullable
    private Spell spell;
    @Nullable
    private UUID casterUUID;
    private int lifetime;
    private boolean hasExecuted;
    private Direction surfaceDirection = Direction.DOWN;
    /** When true this marker arms instead of auto-casting, then fires on proximity (TRAP delivery). */
    private boolean trap;

    public RuneMarkerEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public RuneMarkerEntity(Level level, Player owner, Spell spell, Vec3 position, Direction surfaceDirection) {
        this(level, owner, spell, position, surfaceDirection, false);
    }

    public RuneMarkerEntity(Level level, Player owner, Spell spell, Vec3 position, Direction surfaceDirection, boolean trap) {
        super(ModEntities.RUNE_MARKER.get(), level);
        this.casterUUID = owner.getUUID();
        this.spell = spell;
        this.surfaceDirection = surfaceDirection;
        this.trap = trap;
        this.entityData.set(DATA_IS_TRAP, trap);
        this.setPos(position);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_CHARGE_PROGRESS, 0.0f);
        builder.define(DATA_IS_TRAP, false);
    }

    @Override
    public void tick() {
        super.tick();
        lifetime++;

        int baseCastDelay = trap
                ? buildaspell.config.ModConfig.deliveryInt(buildaspell.spell.DeliveryMethod.TRAP, "armingTicks", 20)
                : buildaspell.config.ModConfig.deliveryInt(buildaspell.spell.DeliveryMethod.RUNE, "castDelayTicks", 20);
        int castDelay = baseCastDelay;
        // The Duration modifier lets a player tune a rune's timing: each level adds to the charge time
        // before a rune fires. (Traps keep a snappy arming time; their Duration extends how long they
        // stay armed instead — see tickTrap.)
        int durationLevel = spell != null ? spell.getDurationLevel() : 0;
        if (!trap && durationLevel > 0) {
            castDelay += durationLevel * buildaspell.config.ModConfig.deliveryInt(
                    buildaspell.spell.DeliveryMethod.RUNE, "ticksPerDuration", 40);
        }
        float chargeProgress = Math.min(1.0f, (float) lifetime / castDelay);
        entityData.set(DATA_CHARGE_PROGRESS, chargeProgress);

        if (level().isClientSide()) {
            // Circular glow particles around rune
            double angle = lifetime * 0.2;
            double radius = 0.5;
            Vec3 normal = Vec3.atLowerCornerOf(surfaceDirection.getNormal());
            Vec3 t1 = getTangent1(normal);
            Vec3 t2 = normal.cross(t1);
            double px = getX() + t1.x * Math.cos(angle) * radius + t2.x * Math.sin(angle) * radius;
            double py = getY() + t1.y * Math.cos(angle) * radius + t2.y * Math.sin(angle) * radius;
            double pz = getZ() + t1.z * Math.cos(angle) * radius + t2.z * Math.sin(angle) * radius;
            level().addParticle(ParticleTypes.GLOW, px, py, pz, 0, 0, 0);

            if (lifetime % 5 == 0) {
                level().addParticle(ParticleTypes.ENCHANT,
                        getX() + random.nextGaussian() * 0.3,
                        getY() + 0.5,
                        getZ() + random.nextGaussian() * 0.3,
                        0, 0.2, 0);
            }
            return;
        }

        // Server-side
        if (level() instanceof ServerLevel serverLevel) {
            // Charging particles
            if (lifetime % 4 == 0) {
                serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT, getX(), getY() + 0.1, getZ(),
                        3, 0.3, 0.1, 0.3, 0.02);
            }
            // Charge sound
            if (lifetime % 10 == 0) {
                level().playSound(null, blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                        SoundSource.PLAYERS, 0.3f, 0.5f + chargeProgress);
            }
        }

        if (trap) {
            tickTrap(castDelay);
            return;
        }

        // Execute when charged
        if (lifetime >= castDelay && !hasExecuted) {
            execute();
            hasExecuted = true;
            discard();
        }

        // Safety discard
        if (lifetime > castDelay + 10 && !hasExecuted) {
            discard();
        }
    }

    /**
     * TRAP behavior: spend the first {@code armingTicks} charging (no trigger yet, so the caster can
     * walk away), then sit armed and fire the spell at the first living non-owner entity that steps
     * within the trigger radius. Self-discards after the configured lifetime if never sprung.
     */
    private void tickTrap(int armingTicks) {
        if (hasExecuted) {
            return;
        }
        if (lifetime < armingTicks) {
            return; // still arming
        }

        int lifetimeTicks = buildaspell.config.ModConfig.deliveryInt(
                buildaspell.spell.DeliveryMethod.TRAP, "lifetimeTicks", 1200);
        int durationLevel = spell != null ? spell.getDurationLevel() : 0;
        if (durationLevel > 0) {
            lifetimeTicks += durationLevel * buildaspell.config.ModConfig.deliveryInt(
                    buildaspell.spell.DeliveryMethod.TRAP, "lifetimePerDuration", 600);
        }
        if (lifetime > lifetimeTicks) {
            discard();
            return;
        }

        if (!(level() instanceof ServerLevel)) {
            return;
        }

        double triggerRadius = buildaspell.config.ModConfig.deliveryDouble(
                buildaspell.spell.DeliveryMethod.TRAP, "triggerRadius", 2.5);
        net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(blockPosition()).inflate(triggerRadius);
        java.util.List<net.minecraft.world.entity.LivingEntity> nearby = level().getEntitiesOfClass(
                net.minecraft.world.entity.LivingEntity.class, box,
                e -> e.isAlive() && !e.getUUID().equals(casterUUID)
                        && e.distanceToSqr(getX(), getY(), getZ()) <= triggerRadius * triggerRadius);
        if (!nearby.isEmpty()) {
            execute();
            hasExecuted = true;
            discard();
        }
    }

    private void execute() {
        if (spell != null && casterUUID != null) {
            Player caster = level().getPlayerByUUID(casterUUID);
            if (caster != null) {
                // Duration already shaped this marker's timing (charge/armed time), so the
                // rune-triggered path strips it before execution; Delay defers the firing.
                SpellExecutor.executeRuneTriggeredSpell(caster, spell, position());
            }
        }
    }

    private Vec3 getTangent1(Vec3 normal) {
        if (Math.abs(normal.y) < 0.9) {
            return normal.cross(new Vec3(0, 1, 0)).normalize();
        }
        return normal.cross(new Vec3(1, 0, 0)).normalize();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag input) {
        if (input.contains("Spell")) {
            Spell.CODEC.parse(NbtOps.INSTANCE, input.get("Spell")).result().ifPresent(s -> this.spell = s);
        }
        if (input.contains("CasterUUID")) {
            String s = input.getString("CasterUUID");
            try { this.casterUUID = UUID.fromString(s); } catch (IllegalArgumentException ignored) {}
        }
        this.lifetime = input.contains("Lifetime") ? input.getInt("Lifetime") : 0;
        this.hasExecuted = input.contains("HasExecuted") ? input.getBoolean("HasExecuted") : false;
        this.surfaceDirection = Direction.from3DDataValue(input.contains("SurfaceDirection") ? input.getByte("SurfaceDirection") : (byte) 0);
        this.trap = input.contains("Trap") && input.getBoolean("Trap");
        this.entityData.set(DATA_IS_TRAP, this.trap);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag output) {
        if (spell != null) output.put("Spell", Spell.CODEC.encodeStart(NbtOps.INSTANCE, spell).getOrThrow());
        if (casterUUID != null) output.putString("CasterUUID", casterUUID.toString());
        output.putInt("Lifetime", lifetime);
        output.putBoolean("HasExecuted", hasExecuted);
        output.putByte("SurfaceDirection", (byte) surfaceDirection.get3DDataValue());
        output.putBoolean("Trap", trap);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    public float getChargeProgress() { return entityData.get(DATA_CHARGE_PROGRESS); }
    public boolean isTrap() { return entityData.get(DATA_IS_TRAP); }
    @Nullable
    public Spell getSpell() { return spell; }
    public Direction getSurfaceDirection() { return surfaceDirection; }
}
