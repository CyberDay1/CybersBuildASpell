package buildaspell.entity;

import buildaspell.registry.ModEntities;
import buildaspell.spell.DeliveryMethod;
import buildaspell.spell.Spell;
import buildaspell.spell.SpellModifier;
import buildaspell.spell.SpellVisual;
import buildaspell.spell.execution.SpellExecutor;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class SpellProjectileEntity extends Projectile {
    private static final EntityDataAccessor<String> DATA_EFFECT_TYPE =
            SynchedEntityData.defineId(SpellProjectileEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_COLOR =
            SynchedEntityData.defineId(SpellProjectileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> DATA_SHAPE =
            SynchedEntityData.defineId(SpellProjectileEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_TRAIL =
            SynchedEntityData.defineId(SpellProjectileEntity.class, EntityDataSerializers.STRING);

    @Nullable
    private Spell spell;
    private boolean isTracking;
    private int lifetime;
    private int blocksPierced;
    private int bouncesRemaining;
    private boolean returning;
    /**
     * Entities this projectile has already affected. Each target is hit at most once per projectile
     * lifetime: without this, a returning (or piercing) projectile overlapping a target re-applied
     * the whole spell every tick it spent inside the hitbox.
     */
    private final java.util.Set<java.util.UUID> hitEntityIds = new java.util.HashSet<>();

    public SpellProjectileEntity(EntityType<? extends SpellProjectileEntity> entityType, Level level) {
        super(entityType, level);
    }

    public SpellProjectileEntity(Level level, Player owner, Spell spell, boolean isTracking) {
        super(ModEntities.SPELL_PROJECTILE.get(), level);
        this.setOwner(owner);
        this.spell = spell;
        this.isTracking = isTracking;
        this.bouncesRemaining = spell.hasModifier(SpellModifier.BOUNCE)
                ? buildaspell.config.ModConfig.modifierInt(SpellModifier.BOUNCE, "bounceCount", 3) : 0;
        this.setPos(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
        if (!spell.getEffects().isEmpty()) {
            setEffectType(spell.getEffects().get(0).getSerializedName());
        }
        SpellVisual visual = spell.getVisual();
        entityData.set(DATA_COLOR, visual.color());
        entityData.set(DATA_SHAPE, visual.shape().getSerializedName());
        entityData.set(DATA_TRAIL, visual.trail());
    }

    public void setEffectType(String type) {
        entityData.set(DATA_EFFECT_TYPE, type);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_EFFECT_TYPE, "");
        builder.define(DATA_COLOR, SpellVisual.COLOR_DEFAULT);
        builder.define(DATA_SHAPE, "cross");
        builder.define(DATA_TRAIL, SpellVisual.DEFAULT_TRAIL);
    }

    @Override
    public void tick() {
        super.tick();
        lifetime++;
        DeliveryMethod projDelivery = isTracking ? DeliveryMethod.TRACKING : DeliveryMethod.CAST;
        if (lifetime > buildaspell.config.ModConfig.deliveryInt(projDelivery, "projectileLifetimeTicks", 200)) {
            discard();
            return;
        }

        if (level().isClientSide()) {
            level().addParticle(trailParticle(entityData.get(DATA_TRAIL)), getX(), getY(), getZ(), 0, 0, 0);
            // Move client-side using the velocity synced in the spawn packet. Without this the
            // client never integrates motion (it only got coarse position syncs), so a fast or
            // short-lived projectile looked frozen at the muzzle while the server one flew on.
            Vec3 clientMotion = getDeltaMovement();
            setPos(getX() + clientMotion.x, getY() + clientMotion.y, getZ() + clientMotion.z);
            return;
        }

        // Tracking logic
        if (isTracking && level() instanceof ServerLevel serverLevel) {
            Entity nearest = null;
            double nearestDist = buildaspell.config.ModConfig.deliveryDouble(
                    DeliveryMethod.TRACKING, "homingRange", 16.0);
            for (Entity e : serverLevel.getEntities(this, getBoundingBox().inflate(nearestDist))) {
                if (e != getOwner() && e.isAlive() && !(e instanceof SpellProjectileEntity)) {
                    double dist = distanceTo(e);
                    if (dist < nearestDist) {
                        nearestDist = dist;
                        nearest = e;
                    }
                }
            }
            if (nearest != null) {
                double homingStrength = buildaspell.config.ModConfig.deliveryDouble(
                        DeliveryMethod.TRACKING, "homingStrength", 0.3);
                Vec3 direction = nearest.position().subtract(position()).normalize().scale(homingStrength);
                setDeltaMovement(getDeltaMovement().add(direction));
            }
        }

        // RETURN: once the projectile has reached its outbound target it homes back to the caster,
        // re-applying its effects on anything it passes through, and is caught (discarded) on arrival.
        if (returning && level() instanceof ServerLevel && getOwner() != null) {
            Entity owner = getOwner();
            Vec3 center = owner.position().add(0, owner.getBbHeight() * 0.5, 0);
            Vec3 toOwner = center.subtract(position());
            double catchRadius = buildaspell.config.ModConfig.modifierDouble(
                    SpellModifier.RETURN, "catchRadius", 2.0);
            if (toOwner.lengthSqr() < catchRadius * catchRadius) {
                level().playSound(null, getX(), getY(), getZ(),
                        SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.6f, 1.4f);
                discard();
                return;
            }
            double speed = Math.max(getDeltaMovement().length(),
                    buildaspell.config.ModConfig.modifierDouble(SpellModifier.RETURN, "minSpeed", 0.4));
            setDeltaMovement(toOwner.normalize().scale(speed));
        }

        // Hit detection
        HitResult hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hitResult.getType() != HitResult.Type.MISS) {
            if (hitResult instanceof EntityHitResult entityHit) {
                onHitEntity(entityHit);
            } else if (hitResult instanceof BlockHitResult blockHit) {
                boolean hasBounce = spell != null && spell.hasModifier(SpellModifier.BOUNCE);
                if (hasBounce && bouncesRemaining > 0) {
                    // Reflect velocity off the hit surface normal
                    Vec3 normal = Vec3.atLowerCornerOf(blockHit.getDirection().getUnitVec3i());
                    Vec3 vel = getDeltaMovement();
                    double dot = vel.dot(normal);
                    setDeltaMovement(vel.subtract(normal.scale(2.0 * dot)));
                    bouncesRemaining--;
                } else {
                    // Execute spell at block hit location if no bounces left
                    if (spell != null && getOwner() instanceof Player caster) {
                        SpellExecutor.executeSpellAtLocationWithDelay(caster, spell, blockHit.getLocation());
                    }
                    if (spell != null && spell.hasReturn() && !returning) {
                        startReturn();
                    } else {
                        discard();
                    }
                }
                return;
            }
        }

        // Move
        Vec3 motion = getDeltaMovement();
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (level().isClientSide()) return;
        // Dedup: phase through anything already hit instead of re-applying the spell.
        if (!hitEntityIds.add(result.getEntity().getUUID())) {
            return;
        }
        if (spell != null && getOwner() instanceof Player caster) {
            SpellExecutor.executeSpellAtLocationWithDelay(caster, spell, result.getEntity().position());
        }
        // While homing back, phase through every target, re-applying the spell as it passes.
        if (returning) {
            return;
        }
        // PIERCE: pass through entities instead of discarding
        int pierceLevel = spell != null ? spell.getModifierCount(SpellModifier.PIERCE) : 0;
        if (pierceLevel > blocksPierced) {
            blocksPierced++;
        } else if (spell != null && spell.hasReturn()) {
            startReturn();
        } else {
            discard();
        }
    }

    /** Flips the projectile into homing-return mode, aiming it back at its owner at its current speed. */
    private void startReturn() {
        returning = true;
        Entity owner = getOwner();
        if (owner != null) {
            Vec3 toOwner = owner.position().add(0, owner.getBbHeight() * 0.5, 0).subtract(position());
            double speed = Math.max(getDeltaMovement().length(),
                    buildaspell.config.ModConfig.modifierDouble(SpellModifier.RETURN, "minSpeed", 0.4));
            setDeltaMovement(toOwner.normalize().scale(speed));
        } else {
            setDeltaMovement(getDeltaMovement().scale(-1));
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        // Already-hit entities are transparent to the ray so targets behind them can still be hit.
        return entity != getOwner() && !hitEntityIds.contains(entity.getUUID()) && super.canHitEntity(entity);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        input.read("Spell", Spell.CODEC).ifPresent(s -> this.spell = s);
        this.isTracking = input.getBooleanOr("IsTracking", false);
        this.lifetime = input.getIntOr("Lifetime", 0);
        this.blocksPierced = input.getIntOr("BlocksPierced", 0);
        this.bouncesRemaining = input.getIntOr("BouncesRemaining", 0);
        this.returning = input.getBooleanOr("Returning", false);
        this.hitEntityIds.clear();
        input.childrenList("HitEntities").ifPresent(list -> {
            for (ValueInput child : list) {
                child.getString("Id").ifPresent(s -> {
                    try {
                        this.hitEntityIds.add(java.util.UUID.fromString(s));
                    } catch (IllegalArgumentException ignored) {}
                });
            }
        });
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (spell != null) {
            output.store("Spell", Spell.CODEC, spell);
        }
        output.putBoolean("IsTracking", isTracking);
        output.putInt("Lifetime", lifetime);
        output.putInt("BlocksPierced", blocksPierced);
        output.putInt("BouncesRemaining", bouncesRemaining);
        output.putBoolean("Returning", returning);
        if (!hitEntityIds.isEmpty()) {
            ValueOutput.ValueOutputList list = output.childrenList("HitEntities");
            for (java.util.UUID id : hitEntityIds) {
                list.addChild().putString("Id", id.toString());
            }
        }
    }

    @Nullable
    public Spell getSpell() { return spell; }
    public boolean isTracking() { return isTracking; }
    public int getLifetime() { return lifetime; }
    public String getEffectType() { return entityData.get(DATA_EFFECT_TYPE); }

    /** Synced packed-RGB color, or {@link SpellVisual#COLOR_DEFAULT} to use the renderer's effect default. */
    public int getVisualColor() { return entityData.get(DATA_COLOR); }
    public String getVisualShape() { return entityData.get(DATA_SHAPE); }

    /** Maps a curated trail id to its particle; falls back to WITCH for unknown ids. */
    private static ParticleOptions trailParticle(String id) {
        return switch (id) {
            case "flame" -> ParticleTypes.FLAME;
            case "soul_fire_flame" -> ParticleTypes.SOUL_FIRE_FLAME;
            case "end_rod" -> ParticleTypes.END_ROD;
            case "crit" -> ParticleTypes.CRIT;
            case "enchanted_hit" -> ParticleTypes.ENCHANTED_HIT;
            case "electric_spark" -> ParticleTypes.ELECTRIC_SPARK;
            case "dragon_breath" -> net.minecraft.core.particles.PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0F);
            case "glow" -> ParticleTypes.GLOW;
            case "cherry" -> ParticleTypes.CHERRY_LEAVES;
            case "smoke" -> ParticleTypes.SMOKE;
            case "snowflake" -> ParticleTypes.SNOWFLAKE;
            case "happy_villager" -> ParticleTypes.HAPPY_VILLAGER;
            default -> ParticleTypes.WITCH;
        };
    }
}
