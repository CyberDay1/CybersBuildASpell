package buildaspell.spell.execution;

import buildaspell.block.SpellLightBlock;
import buildaspell.config.ModConfig;
import buildaspell.registry.ModBlocks;
import buildaspell.spell.MarkManager;
import buildaspell.spell.Spell;
import buildaspell.spell.SpellEffect;
import buildaspell.spell.SpellLootingTracker;
import buildaspell.spell.SpellModifier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class EffectExecutors {

    // ─── DAMAGE ─────────────────────────────────────────────────────────────────

    public static void executeDamage(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        executeDamage(caster, level, origin, spell, spellPower, 1);
    }

    /**
     * Applies one damage event worth {@code stacks} copies of the effect.
     *
     * <p>Repeating Damage sums into a single {@link LivingEntity#hurt} rather than landing one hit per
     * copy. Hitting the target repeatedly in the same tick used to be the mod's real damage amplifier:
     * every copy re-entered {@code hurt} with the invulnerability window forced low, so all of them
     * landed in full and armor, Protection and Resistance each only ever saw one small hit. Summing
     * first means the target takes the total as a single blow, which is what its defenses are supposed
     * to be measured against.
     */
    public static void executeDamage(Player caster, Level level, Vec3 origin, Spell spell, float spellPower, int stacks) {
        float range = spell.getRange();
        boolean nullify = spell.hasNullify();

        if (nullify) return;

        float damageMultiplier = ModConfig.getEffectDamageMultiplier(SpellEffect.DAMAGE);
        float powerMult = 1.0f + spell.getPowerLevel() * (float) ModConfig.effectDouble(SpellEffect.DAMAGE, "powerBonusPerLevel", 0.15);
        float damage = (spellPower * (float) ModConfig.effectDouble(SpellEffect.DAMAGE, "spellPowerScale", 0.20) * damageMultiplier)
                * powerMult * stacks;

        AABB box = new AABB(origin.subtract(range, range, range), origin.add(range, range, range));
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, box,
                entity -> entity != caster && entity.position().distanceTo(origin) <= range);

        if (!entities.isEmpty()) {
            SpellSounds.damage(level, origin);
        }

        DamageSource damageSource = level.damageSources().indirectMagic(caster, caster);
        int fortuneLevel = spell.getFortuneLevel();
        int sunderLevel = spell.getSunderLevel();
        int leechLevel = spell.getLeechLevel();
        double sunderPerArmor = ModConfig.modifierDouble(SpellModifier.SUNDER, "bonusPerArmorPerLevel", 0.5);
        double leechFraction = ModConfig.modifierDouble(SpellModifier.LEECH, "healFractionPerLevel", 0.25);
        float leechHealed = 0f;

        for (LivingEntity entity : entities) {
            if (fortuneLevel > 0) {
                SpellLootingTracker.setLootingLevel(entity.getUUID(), fortuneLevel);
            }
            // Sunder rewards punching through defenses: the more armor the target wears, the more
            // bonus damage it suffers (magic already ignores the armor bar, so this is the "anti-tank" knob).
            float dealt = damage;
            if (sunderLevel > 0) {
                dealt += (float) (entity.getArmorValue() * sunderLevel * sunderPerArmor);
            }
            entity.hurt(damageSource, dealt);
            entity.invulnerableTime = ModConfig.effectInt(SpellEffect.DAMAGE, "invulnTicks", 5);
            if (leechLevel > 0) {
                leechHealed += (float) (dealt * leechFraction * leechLevel);
            }

            if (level instanceof ServerLevel serverLevel) {
                SpellParticles.damageHit(serverLevel, entity.position().add(0, entity.getBbHeight() / 2, 0));
            }
        }

        if (leechHealed > 0f) {
            caster.heal(leechHealed);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.HEART,
                        caster.getX(), caster.getY() + caster.getBbHeight(), caster.getZ(),
                        3, 0.3, 0.2, 0.3, 0.0);
            }
        }
    }

    // ─── IGNITE ─────────────────────────────────────────────────────────────────

    public static void executeIgnite(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        float range = spell.getRange();
        int powerLevel = spell.getPowerLevel();
        int prolongedLevel = spell.getProlongedLevel();

        int duration = ModConfig.effectInt(SpellEffect.IGNITE, "durationBase", 100)
                + (powerLevel * ModConfig.effectInt(SpellEffect.IGNITE, "durationPerPower", 20))
                + (prolongedLevel * ModConfig.effectInt(SpellEffect.IGNITE, "durationPerProlonged", 40));

        SpellSounds.ignite(level, origin);
        if (level instanceof ServerLevel serverLevel) {
            SpellParticles.igniteEffect(serverLevel, origin);
        }

        // Nullify only zeroes direct spell damage; ignition is a status, so it always applies.
        AABB box = new AABB(origin.subtract(range, range, range), origin.add(range, range, range));
        List<Entity> entities = level.getEntitiesOfClass(Entity.class, box,
                entity -> entity != caster && entity.position().distanceTo(origin) <= range);

        for (Entity entity : entities) {
            entity.setRemainingFireTicks(duration);
        }

        BlockPos centerPos = BlockPos.containing(origin);
        for (BlockPos pos : BlockPos.betweenClosed(
                centerPos.offset((int) -range, (int) -range, (int) -range),
                centerPos.offset((int) range, (int) range, (int) range))) {
            if (!level.isLoaded(pos)) continue;
            if (pos.getCenter().distanceTo(origin) <= range) {
                BlockState state = level.getBlockState(pos);
                if (state.isAir()) {
                    BlockPos below = pos.below();
                    if (level.getBlockState(below).isSolid()) {
                        level.setBlock(pos, Blocks.FIRE.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    // ─── FREEZE ─────────────────────────────────────────────────────────────────

    public static void executeFreeze(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        float range = spell.getRange();
        int powerLevel = spell.getPowerLevel();
        int prolongedLevel = spell.getProlongedLevel();

        int freezeDuration = ModConfig.effectInt(SpellEffect.FREEZE, "durationBase", 100)
                + (powerLevel * ModConfig.effectInt(SpellEffect.FREEZE, "durationPerPower", 20))
                + (prolongedLevel * ModConfig.effectInt(SpellEffect.FREEZE, "durationPerProlonged", 40));

        SpellSounds.freeze(level, origin);
        if (level instanceof ServerLevel serverLevel) {
            SpellParticles.freezeEffect(serverLevel, origin);
        }

        // Nullify only zeroes direct spell damage; the frozen status itself always applies.
        AABB box = new AABB(origin.subtract(range, range, range), origin.add(range, range, range));
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, box,
                entity -> entity != caster && entity.position().distanceTo(origin) <= range);

        for (LivingEntity entity : entities) {
            entity.setTicksFrozen(Math.min(entity.getTicksFrozen() + freezeDuration,
                    entity.getTicksRequiredToFreeze() + ModConfig.effectInt(SpellEffect.FREEZE, "freezeOverflowCap", 100)));
        }

        BlockPos centerPos = BlockPos.containing(origin);
        for (BlockPos pos : BlockPos.betweenClosed(
                centerPos.offset((int) -range, (int) -range, (int) -range),
                centerPos.offset((int) range, (int) range, (int) range))) {
            if (!level.isLoaded(pos)) continue;
            if (pos.getCenter().distanceTo(origin) <= range) {
                BlockState state = level.getBlockState(pos);
                if (state.is(Blocks.WATER)) {
                    level.setBlock(pos, Blocks.ICE.defaultBlockState(), 3);
                } else if (state.is(Blocks.LAVA)) {
                    level.setBlock(pos, Blocks.OBSIDIAN.defaultBlockState(), 3);
                } else if (level.getBlockState(pos.above()).isAir() && state.isSolid()) {
                    level.setBlock(pos.above(), Blocks.SNOW.defaultBlockState(), 3);
                }
            }
        }
    }

    // ─── TELEPORT ───────────────────────────────────────────────────────────────

    public static void executeTeleport(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        Vec3 startPos = caster.position();

        // Self-cast Teleport used to be a no-op (teleporting you to where you already stand).
        // When the destination is the caster's own feet, teleport forward along the look vector
        // up to the spell's range instead, walking back until the destination is clear.
        if (origin.distanceToSqr(startPos) < 0.01) {
            float distance = spell.getRange();
            Vec3 lookVec = caster.getLookAngle();
            Vec3 targetPos = startPos.add(lookVec.scale(distance));
            BlockPos targetBlockPos = BlockPos.containing(targetPos);
            while (!level.getBlockState(targetBlockPos).isAir() && distance > 1.0f) {
                distance -= 1.0f;
                targetPos = startPos.add(lookVec.scale(distance));
                targetBlockPos = BlockPos.containing(targetPos);
            }
            origin = targetPos;
        }

        SpellSounds.teleport(level, startPos);
        if (level instanceof ServerLevel serverLevel) {
            SpellParticles.teleportEffect(serverLevel, startPos);
        }

        caster.teleportTo(origin.x, origin.y, origin.z);

        SpellSounds.teleportArrival(level, origin);
        if (level instanceof ServerLevel serverLevel) {
            SpellParticles.teleportEffect(serverLevel, origin);
        }
    }

    // ─── PULL ───────────────────────────────────────────────────────────────────

    public static void executePull(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        float range = spell.getRange();
        int powerLevel = spell.getPowerLevel();
        float pullStrength = (float) ModConfig.effectDouble(SpellEffect.PULL, "strengthBase", 0.5)
                + (powerLevel * (float) ModConfig.effectDouble(SpellEffect.PULL, "strengthPerPower", 0.1));

        SpellSounds.pull(level, origin);

        AABB box = new AABB(origin.subtract(range, range, range), origin.add(range, range, range));
        List<Entity> entities = level.getEntitiesOfClass(Entity.class, box,
                entity -> entity != caster && entity.position().distanceTo(origin) <= range);

        for (Entity entity : entities) {
            Vec3 direction = origin.subtract(entity.position()).normalize();
            entity.setDeltaMovement(entity.getDeltaMovement().add(direction.scale(pullStrength)));
            // Players own their own movement, so a server-side velocity change is discarded unless
            // the server pushes it out. hurtMarked makes ServerEntity send a motion packet next tick.
            entity.hurtMarked = true;
            if (level instanceof ServerLevel serverLevel) {
                SpellParticles.pullEffect(serverLevel, origin, entity.position());
            }
        }
    }

    // ─── PUSH ───────────────────────────────────────────────────────────────────

    public static void executePush(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        float range = spell.getRange();
        int powerLevel = spell.getPowerLevel();
        float pushStrength = (float) ModConfig.effectDouble(SpellEffect.PUSH, "strengthBase", 0.5)
                + (powerLevel * (float) ModConfig.effectDouble(SpellEffect.PUSH, "strengthPerPower", 0.1));

        SpellSounds.push(level, origin);

        AABB box = new AABB(origin.subtract(range, range, range), origin.add(range, range, range));
        List<Entity> entities = level.getEntitiesOfClass(Entity.class, box,
                entity -> entity != caster && entity.position().distanceTo(origin) <= range);

        for (Entity entity : entities) {
            Vec3 direction = entity.position().subtract(origin).normalize();
            entity.setDeltaMovement(entity.getDeltaMovement().add(direction.scale(pushStrength)));
            entity.hurtMarked = true;
            if (level instanceof ServerLevel serverLevel) {
                SpellParticles.pushEffect(serverLevel, origin, entity.position());
            }
        }
    }

    // ─── YEET ───────────────────────────────────────────────────────────────────

    public static void executeYeet(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        float range = spell.getRange();
        int powerLevel = spell.getPowerLevel();
        float yeetStrength = (float) ModConfig.effectDouble(SpellEffect.YEET, "strengthBase", 1.0)
                + (powerLevel * (float) ModConfig.effectDouble(SpellEffect.YEET, "strengthPerPower", 0.2))
                + (spellPower / (float) ModConfig.effectDouble(SpellEffect.YEET, "spellPowerDivisor", 50.0));

        SpellSounds.yeet(level, origin);

        AABB box = new AABB(origin.subtract(range, range, range), origin.add(range, range, range));
        List<Entity> entities = level.getEntitiesOfClass(Entity.class, box,
                entity -> entity != caster && entity.position().distanceTo(origin) <= range);

        for (Entity entity : entities) {
            Vec3 lookVec = entity.getLookAngle();
            entity.setDeltaMovement(lookVec.scale(yeetStrength));
            entity.hurtMarked = true;
            if (level instanceof ServerLevel serverLevel) {
                SpellParticles.yeetEffect(serverLevel, entity.position(), lookVec);
            }
        }
    }

    // ─── REAP ───────────────────────────────────────────────────────────────────

    public static void executeReap(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        float range = spell.getRange();
        boolean hasFortune = spell.getFortuneLevel() > 0;
        int fortuneLevel = spell.getFortuneLevel();
        boolean hasGentleness = spell.hasGentleness();

        BlockPos centerPos = BlockPos.containing(origin);
        for (BlockPos pos : BlockPos.betweenClosed(
                centerPos.offset((int) -range, (int) -range, (int) -range),
                centerPos.offset((int) range, (int) range, (int) range))) {
            if (!level.isLoaded(pos)) continue;
            if (pos.getCenter().distanceTo(origin) <= range) {
                BlockState state = level.getBlockState(pos);
                if (state.getBlock() instanceof CropBlock cropBlock && cropBlock.isMaxAge(state)) {
                    List<ItemStack> drops = Block.getDrops(state, (ServerLevel) level, pos,
                            level.getBlockEntity(pos), caster, ItemStack.EMPTY);

                    for (ItemStack drop : drops) {
                        if (hasFortune && !hasGentleness && drop.getCount() < drop.getMaxStackSize()) {
                            int bonusItems = level.getRandom().nextInt(fortuneLevel + 1);
                            // Clamped to the item's own limit so a harvest can never mint an
                            // oversized stack.
                            drop.setCount(Math.min(drop.getMaxStackSize(), drop.getCount() + bonusItems));
                        }
                        ItemEntity itemEntity = new ItemEntity(level, pos.getX() + 0.5,
                                pos.getY() + 0.5, pos.getZ() + 0.5, drop);
                        level.addFreshEntity(itemEntity);
                    }

                    level.setBlock(pos, cropBlock.getStateForAge(0), 3);
                }
            }
        }
    }

    // ─── EXPLOSION ──────────────────────────────────────────────────────────────

    public static void executeExplosion(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        int powerLevel = spell.getPowerLevel();
        int areaLevel = spell.getModifierCount(SpellModifier.INCREASED_AREA);
        boolean nullify = spell.hasNullify();

        float explosionPower = (float) ModConfig.effectDouble(SpellEffect.EXPLOSION, "powerBase", 2.0)
                + (powerLevel * (float) ModConfig.effectDouble(SpellEffect.EXPLOSION, "powerPerLevel", 0.75))
                + (areaLevel * (float) ModConfig.effectDouble(SpellEffect.EXPLOSION, "powerPerArea", 0.5));

        Level.ExplosionInteraction interaction;
        if (powerLevel >= ModConfig.effectInt(SpellEffect.EXPLOSION, "tntThreshold", 4)) {
            interaction = Level.ExplosionInteraction.TNT;
        } else if (powerLevel >= ModConfig.effectInt(SpellEffect.EXPLOSION, "blockThreshold", 2)) {
            interaction = Level.ExplosionInteraction.BLOCK;
        } else {
            interaction = Level.ExplosionInteraction.NONE;
        }

        // The calculator carries the mod-wide damage contract: Nullify zeroes all entity damage.
        // Block breaking is governed solely by the power-level thresholds above.
        SpellExplosionCalculator calculator = new SpellExplosionCalculator(caster, nullify);
        level.explode(caster, null, calculator, origin.x, origin.y, origin.z, explosionPower, false, interaction);
    }

    /**
     * Explosion damage calculator carrying the spell system's entity-damage contract: with
     * Nullify the blast damages no entities at all (block destruction is untouched), and the
     * caster is never damaged or knocked around by their own spell's explosion. Extends the
     * entity-based calculator so block resistance behaves exactly like a plain caster-sourced
     * explosion.
     */
    private static final class SpellExplosionCalculator extends net.minecraft.world.level.EntityBasedExplosionDamageCalculator {
        private final Player caster;
        private final boolean nullify;

        SpellExplosionCalculator(Player caster, boolean nullify) {
            super(caster);
            this.caster = caster;
            this.nullify = nullify;
        }

        @Override
        public boolean shouldDamageEntity(net.minecraft.world.level.Explosion explosion, Entity entity) {
            if (nullify || entity == caster) {
                return false;
            }
            return super.shouldDamageEntity(explosion, entity);
        }

        @Override
        public float getKnockbackMultiplier(Entity entity) {
            return entity == caster ? 0.0f : super.getKnockbackMultiplier(entity);
        }
    }

    // ─── HEAL ───────────────────────────────────────────────────────────────────

    public static void executeHeal(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        float range = spell.getRange();
        boolean nullify = spell.hasNullify();

        float powerMult = 1.0f + spell.getPowerLevel() * (float) ModConfig.effectDouble(SpellEffect.HEAL, "powerBonusPerLevel", 0.15);
        float healAmount = (spellPower * (float) ModConfig.effectDouble(SpellEffect.HEAL, "healScale", 0.3)) * powerMult;

        SpellSounds.heal(level, origin);

        AABB box = new AABB(origin.subtract(range, range, range), origin.add(range, range, range));
        // Heal is beneficial, so the caster is a valid target: a Self-cast Heal heals you.
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, box,
                entity -> (SpellEffect.HEAL.isBeneficial() || entity != caster)
                        && entity.position().distanceTo(origin) <= range);

        DamageSource damageSource = level.damageSources().indirectMagic(caster, caster);
        int fortuneLevel = spell.getFortuneLevel();

        for (LivingEntity entity : entities) {
            if (entity.isInvertedHealAndHarm()) {
                // Undead take damage from healing magic — which is exactly the part Nullify zeroes.
                if (nullify) {
                    continue;
                }
                float undeadDamage = (spellPower * (float) ModConfig.effectDouble(SpellEffect.HEAL, "undeadScale", 0.6)) * powerMult;
                if (fortuneLevel > 0) {
                    SpellLootingTracker.setLootingLevel(entity.getUUID(), fortuneLevel);
                }
                entity.hurt(damageSource, undeadDamage);
                entity.invulnerableTime = ModConfig.effectInt(SpellEffect.HEAL, "invulnTicks", 5);
                if (level instanceof ServerLevel serverLevel) {
                    SpellParticles.damageHit(serverLevel, entity.position().add(0, entity.getBbHeight() / 2, 0));
                }
            } else {
                entity.heal(healAmount);
                if (level instanceof ServerLevel serverLevel) {
                    SpellParticles.healEffect(serverLevel, entity.position());
                }
            }
        }
    }

    // ─── LIGHTNING ──────────────────────────────────────────────────────────────

    public static void executeLightning(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        float range = spell.getRange();
        int powerLevel = spell.getPowerLevel();
        boolean nullify = spell.hasNullify();

        // Continuous power scaling: whole extra strikes per powerPerStrike stacks, and the
        // remainder becomes a proportional chance of one more. The old integer division made
        // every Increased Power stack below the threshold do literally nothing.
        int powerPerStrike = ModConfig.effectInt(SpellEffect.LIGHTNING, "powerPerStrike", 10);
        int strikeCount = ModConfig.effectInt(SpellEffect.LIGHTNING, "strikeBase", 1)
                + (powerLevel / powerPerStrike);
        int remainder = powerLevel % powerPerStrike;
        if (remainder > 0 && level.getRandom().nextInt(powerPerStrike) < remainder) {
            strikeCount++;
        }

        AABB box = new AABB(origin.subtract(range, range, range), origin.add(range, range, range));
        List<Entity> entities = level.getEntitiesOfClass(Entity.class, box,
                entity -> entity != caster && entity.position().distanceTo(origin) <= range);

        if (!entities.isEmpty()) {
            for (int i = 0; i < Math.min(strikeCount, entities.size()); i++) {
                Entity target = entities.get(i);

                SpellParticles.lightningPreStrike(serverLevel, target.position());
                SpellSounds.lightningCharge(level, target.position());

                var lightning = EntityType.LIGHTNING_BOLT.create(serverLevel, EntitySpawnReason.TRIGGERED);
                if (lightning != null) {
                    lightning.setPos(target.position());
                    // Nullify keeps the strike as pure spectacle: full bolt, zero damage/fire.
                    lightning.setVisualOnly(nullify);
                    serverLevel.addFreshEntity(lightning);
                    LightningInteractions.onStrike(serverLevel, target.getX(), target.getZ(), nullify);
                }
            }
        } else {
            SpellParticles.lightningPreStrike(serverLevel, origin);
            SpellSounds.lightningCharge(level, origin);

            var lightning = EntityType.LIGHTNING_BOLT.create(serverLevel, EntitySpawnReason.TRIGGERED);
            if (lightning != null) {
                lightning.setPos(origin);
                lightning.setVisualOnly(nullify);
                serverLevel.addFreshEntity(lightning);
                LightningInteractions.onStrike(serverLevel, origin.x, origin.z, nullify);
            }
        }
    }

    // ─── POISON ─────────────────────────────────────────────────────────────────

    public static void executePoison(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        float range = spell.getRange();
        int powerLevel = spell.getPowerLevel();
        int prolongedLevel = spell.getProlongedLevel();

        int duration = ModConfig.effectInt(SpellEffect.POISON, "durationBase", 80)
                + (powerLevel * ModConfig.effectInt(SpellEffect.POISON, "durationPerPower", 40))
                + (prolongedLevel * ModConfig.effectInt(SpellEffect.POISON, "durationPerProlonged", 60));
        int amplifier = (int) (spellPower / ModConfig.effectDouble(SpellEffect.POISON, "amplifierDivisor", 15.0));

        SpellSounds.poison(level, origin);

        AABB box = new AABB(origin.subtract(range, range, range), origin.add(range, range, range));
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, box,
                entity -> entity != caster && entity.position().distanceTo(origin) <= range);

        for (LivingEntity entity : entities) {
            entity.addEffect(new MobEffectInstance(MobEffects.POISON, duration, amplifier));
            if (level instanceof ServerLevel serverLevel) {
                SpellParticles.poisonEffect(serverLevel, entity.position().add(0, entity.getBbHeight() / 2, 0));
            }
        }
    }

    // ─── WITHER ─────────────────────────────────────────────────────────────────

    public static void executeWither(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        float range = spell.getRange();
        int powerLevel = spell.getPowerLevel();
        int prolongedLevel = spell.getProlongedLevel();

        int duration = ModConfig.effectInt(SpellEffect.WITHER, "durationBase", 80)
                + (powerLevel * ModConfig.effectInt(SpellEffect.WITHER, "durationPerPower", 30))
                + (prolongedLevel * ModConfig.effectInt(SpellEffect.WITHER, "durationPerProlonged", 50));
        int amplifier = (int) (spellPower / ModConfig.effectDouble(SpellEffect.WITHER, "amplifierDivisor", 15.0));

        SpellSounds.wither(level, origin);

        AABB box = new AABB(origin.subtract(range, range, range), origin.add(range, range, range));
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, box,
                entity -> entity != caster && entity.position().distanceTo(origin) <= range);

        for (LivingEntity entity : entities) {
            entity.addEffect(new MobEffectInstance(MobEffects.WITHER, duration, amplifier));
            if (level instanceof ServerLevel serverLevel) {
                SpellParticles.witherEffect(serverLevel, entity.position().add(0, entity.getBbHeight() / 2, 0));
            }
        }
    }

    // ─── SATURATION ─────────────────────────────────────────────────────────────

    public static void executeSaturation(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        float range = spell.getRange();
        int powerLevel = spell.getPowerLevel();
        int prolongedLevel = spell.getProlongedLevel();

        int duration = ModConfig.effectInt(SpellEffect.SATURATION, "durationBase", 200)
                + (powerLevel * ModConfig.effectInt(SpellEffect.SATURATION, "durationPerPower", 100))
                + (prolongedLevel * ModConfig.effectInt(SpellEffect.SATURATION, "durationPerProlonged", 100));
        int amplifier = powerLevel;

        AABB box = new AABB(origin.subtract(range, range, range), origin.add(range, range, range));
        // Saturation is beneficial, so the caster feeds too on a Self cast.
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, box,
                entity -> (SpellEffect.SATURATION.isBeneficial() || entity != caster)
                        && entity.position().distanceTo(origin) <= range);

        for (LivingEntity entity : entities) {
            entity.addEffect(new MobEffectInstance(MobEffects.SATURATION, duration, amplifier));
        }
    }

    // ─── LAUNCH ─────────────────────────────────────────────────────────────────

    public static void executeLaunch(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        float range = spell.getRange();
        int powerLevel = spell.getPowerLevel();
        float launchStrength = (float) ModConfig.effectDouble(SpellEffect.LAUNCH, "strengthBase", 1.0)
                + (powerLevel * (float) ModConfig.effectDouble(SpellEffect.LAUNCH, "strengthPerPower", 0.3))
                + (spellPower / (float) ModConfig.effectDouble(SpellEffect.LAUNCH, "spellPowerDivisor", 50.0));

        SpellSounds.launch(level, origin);
        if (level instanceof ServerLevel serverLevel) {
            SpellParticles.launchEffect(serverLevel, origin);
        }

        AABB box = new AABB(origin.subtract(range, range, range), origin.add(range, range, range));
        List<Entity> entities = level.getEntitiesOfClass(Entity.class, box,
                entity -> entity != caster && entity.position().distanceTo(origin) <= range);

        for (Entity entity : entities) {
            Vec3 currentMotion = entity.getDeltaMovement();
            entity.setDeltaMovement(currentMotion.x, launchStrength, currentMotion.z);
            entity.hurtMarked = true;
        }
    }

    // ─── LIGHT ──────────────────────────────────────────────────────────────────

    public static void executeLight(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        float range = spell.getRange();
        int powerLevel = spell.getPowerLevel();

        int lightLevel = Math.min(ModConfig.effectInt(SpellEffect.LIGHT, "lightLevelCap", 15),
                ModConfig.effectInt(SpellEffect.LIGHT, "lightLevelBase", 12) + powerLevel);
        int durationTicks = ModConfig.effectInt(SpellEffect.LIGHT, "durationTicks", 1200);
        BlockPos center = BlockPos.containing(origin);
        int radius = (int) range;
        SpellLightBlock spellLight = ModBlocks.SPELL_LIGHT.get();

        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-radius, -radius, -radius),
                center.offset(radius, radius, radius))) {
            if (!level.isLoaded(pos)) continue;
            if (pos.distSqr(center) <= radius * radius) {
                BlockState currentState = level.getBlockState(pos);
                // Vanilla light is accepted here so that re-casting over the permanent lights left
                // by earlier versions converts them into ones that expire.
                if (currentState.isAir() || currentState.is(spellLight) || currentState.is(Blocks.LIGHT)) {
                    level.setBlock(pos, spellLight.defaultBlockState()
                            .setValue(LightBlock.LEVEL, lightLevel), 3);
                    level.scheduleTick(pos, spellLight, durationTicks);
                }
            }
        }
    }

    // ─── SLAM ───────────────────────────────────────────────────────────────────

    public static void executeSlam(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        float range = spell.getRange();
        int powerLevel = spell.getPowerLevel();
        float slamStrength = (float) ModConfig.effectDouble(SpellEffect.SLAM, "strengthBase", -1.5)
                - (powerLevel * (float) ModConfig.effectDouble(SpellEffect.SLAM, "strengthPerPower", 0.4))
                - (spellPower / (float) ModConfig.effectDouble(SpellEffect.SLAM, "spellPowerDivisor", 50.0));

        SpellSounds.slam(level, origin);
        if (level instanceof ServerLevel serverLevel) {
            SpellParticles.slamEffect(serverLevel, origin);
        }

        AABB box = new AABB(origin.subtract(range, range, range), origin.add(range, range, range));
        List<Entity> entities = level.getEntitiesOfClass(Entity.class, box,
                entity -> entity != caster && entity.position().distanceTo(origin) <= range);

        for (Entity entity : entities) {
            Vec3 currentMotion = entity.getDeltaMovement();
            entity.setDeltaMovement(currentMotion.x, slamStrength, currentMotion.z);
            entity.hurtMarked = true;
        }
    }

    // ─── LEVITATION ─────────────────────────────────────────────────────────────

    public static void executeLevitation(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        applyEffectInArea(caster, level, origin, spell, SpellEffect.LEVITATION, MobEffects.LEVITATION, spell.getPowerLevel());
    }

    // ─── SLOW FALL ──────────────────────────────────────────────────────────────

    public static void executeSlowFall(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        applyEffectInArea(caster, level, origin, spell, SpellEffect.SLOW_FALL, MobEffects.SLOW_FALLING, 0);
    }

    // ─── BREAK ──────────────────────────────────────────────────────────────────

    public static void executeBreak(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        float range = spell.getRange();
        int powerLevel = spell.getPowerLevel();
        boolean hasGentleness = spell.hasGentleness();
        int fortuneLevel = spell.getFortuneLevel();
        boolean hasWall = spell.hasWall();
        boolean hasFloor = spell.hasFloor();
        int fillLevel = spell.getFillLevel();

        float maxHardness = (float) ModConfig.effectDouble(SpellEffect.BREAK, "hardnessBase", 3.0)
                + (powerLevel * (float) ModConfig.effectDouble(SpellEffect.BREAK, "hardnessPerPower", 2.0));

        level.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.STONE_BREAK, SoundSource.PLAYERS, 0.8f, 0.9f);

        BlockPos centerPos = BlockPos.containing(origin);
        int fillRadius = fillLevel > 0
                ? ModConfig.effectInt(SpellEffect.BREAK, "fillRadiusBase", 25)
                    + (fillLevel * ModConfig.effectInt(SpellEffect.BREAK, "fillRadiusPerFill", 5))
                : (int) range;

        ItemStack tool = createToolForBreak(level, hasGentleness, fortuneLevel);

        // With Fill, Break follows the mass it is aimed at rather than clearing a sphere: it walks
        // from block to touching block, so it eats the vein or the wall it hit and stops at the
        // open air instead of reaching through into whatever is behind.
        boolean flood = usesFlood(fillLevel, hasWall, hasFloor);
        Iterable<BlockPos> targets = flood
                ? FloodFill.connected(level, centerPos,
                        FloodFill.seedsAround(level, centerPos, pos -> isBreakable(level, pos, maxHardness)),
                        fillRadius, ModConfig.effectInt(SpellEffect.BREAK, "fillBudget", 4096),
                        pos -> isBreakable(level, pos, maxHardness))
                : BlockPos.betweenClosed(
                        centerPos.offset(-fillRadius, -fillRadius, -fillRadius),
                        centerPos.offset(fillRadius, fillRadius, fillRadius));

        for (BlockPos pos : targets) {
            if (!level.isLoaded(pos)) continue;

            if (!flood && !isInShapeRange(pos, centerPos, origin, (int) range, fillRadius, hasWall, hasFloor, false)) continue;

            BlockState state = level.getBlockState(pos);
            // Light blocks are unbreakable by destroy speed, so the hardness gate below would skip
            // them. Clear them outright instead: it is the only way to remove the permanent lights
            // left in worlds by earlier versions, which cannot be mined in survival either.
            if (state.is(Blocks.LIGHT) || state.is(ModBlocks.SPELL_LIGHT.get())) {
                level.removeBlock(pos, false);
                continue;
            }

            if (!state.isAir() && state.getDestroySpeed(level, pos) >= 0 && state.getDestroySpeed(level, pos) <= maxHardness) {

                List<ItemStack> drops = Block.getDrops(state, (ServerLevel) level, pos,
                        level.getBlockEntity(pos), caster, tool);

                for (ItemStack drop : drops) {
                    ItemEntity itemEntity = new ItemEntity(level, pos.getX() + 0.5,
                            pos.getY() + 0.5, pos.getZ() + 0.5, drop);
                    level.addFreshEntity(itemEntity);
                }

                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.POOF,
                            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                            4, 0.3, 0.3, 0.3, 0.15);
                }

                level.removeBlock(pos, false);
            }
        }
    }

    // ─── INVISIBILITY ───────────────────────────────────────────────────────────

    public static void executeInvisibility(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        applyEffectInArea(caster, level, origin, spell, SpellEffect.INVISIBILITY, MobEffects.INVISIBILITY, 0);
    }

    // ─── SPEED ──────────────────────────────────────────────────────────────────

    public static void executeSpeed(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        applyEffectInArea(caster, level, origin, spell, SpellEffect.SPEED, MobEffects.SPEED,
                Math.min(spell.getPowerLevel(), ModConfig.effectInt(SpellEffect.SPEED, "amplifierCap", 3)));
    }

    // ─── HASTE ──────────────────────────────────────────────────────────────────

    public static void executeHaste(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        applyEffectInArea(caster, level, origin, spell, SpellEffect.HASTE, MobEffects.HASTE,
                Math.min(spell.getPowerLevel(), ModConfig.effectInt(SpellEffect.HASTE, "amplifierCap", 3)));
    }

    // ─── BLINK ──────────────────────────────────────────────────────────────────

    public static void executeBlink(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        int powerLevel = spell.getPowerLevel();
        float distance = (float) ModConfig.effectDouble(SpellEffect.BLINK, "distanceBase", 5.0)
                + (powerLevel * (float) ModConfig.effectDouble(SpellEffect.BLINK, "distancePerPower", 2.0))
                + (spellPower * (float) ModConfig.effectDouble(SpellEffect.BLINK, "spellPowerScale", 0.1));
        distance = Math.min(distance, (float) ModConfig.effectDouble(SpellEffect.BLINK, "distanceCap", 20.0));

        Vec3 lookVec = caster.getLookAngle();
        Vec3 targetPos = caster.position().add(lookVec.scale(distance));

        BlockPos targetBlockPos = BlockPos.containing(targetPos);
        while (!level.getBlockState(targetBlockPos).isAir() && distance > 1.0f) {
            distance -= 1.0f;
            targetPos = caster.position().add(lookVec.scale(distance));
            targetBlockPos = BlockPos.containing(targetPos);
        }

        Vec3 startPos = caster.position();
        SpellSounds.blink(level, startPos);

        caster.teleportTo(targetPos.x, targetPos.y, targetPos.z);

        SpellSounds.blink(level, targetPos);
        if (level instanceof ServerLevel serverLevel) {
            SpellParticles.blinkTrail(serverLevel, startPos, targetPos);
        }
    }

    // ─── SWAP ───────────────────────────────────────────────────────────────────

    public static void executeSwap(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        float range = spell.getRange();

        AABB box = new AABB(origin.subtract(range, range, range), origin.add(range, range, range));
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, box,
                entity -> entity != caster && entity.position().distanceTo(origin) <= range);

        if (!entities.isEmpty()) {
            entities.sort(Comparator.comparingDouble(e -> e.distanceToSqr(origin.x, origin.y, origin.z)));

            LivingEntity target = entities.get(0);
            Vec3 casterPos = caster.position();
            Vec3 targetPos = target.position();

            level.playSound(null, casterPos.x, casterPos.y, casterPos.z,
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.2f);

            if (level instanceof ServerLevel serverLevel) {
                for (int i = 0; i < 16; i++) {
                    double angle = (Math.PI * 2 * i) / 16;
                    double vx = Math.cos(angle) * 0.5;
                    double vz = Math.sin(angle) * 0.5;
                    serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            casterPos.x, casterPos.y + 1, casterPos.z, 1, vx, 0.2, vz, 0.1);
                    serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            targetPos.x, targetPos.y + 1, targetPos.z, 1, vx, 0.2, vz, 0.1);
                }
            }

            caster.teleportTo(targetPos.x, targetPos.y, targetPos.z);
            target.teleportTo(casterPos.x, casterPos.y, casterPos.z);

            level.playSound(null, targetPos.x, targetPos.y, targetPos.z,
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 0.8f);
        }
    }

    // ─── SUMMON ─────────────────────────────────────────────────────────────────

    public static void executeSummon(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        int powerLevel = spell.getPowerLevel();
        int wolfCount = Math.min(ModConfig.effectInt(SpellEffect.SUMMON, "countBase", 1) + powerLevel,
                ModConfig.effectInt(SpellEffect.SUMMON, "countCap", 5));

        level.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.WOLF_STEP, SoundSource.PLAYERS, 1.0f, 1.0f);

        if (level instanceof ServerLevel serverLevel) {
            SpellParticles.summonCircle(serverLevel, origin);
        }

        for (int i = 0; i < wolfCount; i++) {
            double angle = (2 * Math.PI * i) / wolfCount;
            double spawnRadius = ModConfig.effectDouble(SpellEffect.SUMMON, "spawnRadius", 2.0);
            double offsetX = Math.cos(angle) * spawnRadius;
            double offsetZ = Math.sin(angle) * spawnRadius;

            Wolf wolf = EntityType.WOLF.create(level, EntitySpawnReason.MOB_SUMMONED);
            if (wolf != null) {
                wolf.setPos(origin.x + offsetX, origin.y, origin.z + offsetZ);
                wolf.tame(caster);
                wolf.setOrderedToSit(false);
                level.addFreshEntity(wolf);
            }
        }
    }

    // ─── CREATE WATER ───────────────────────────────────────────────────────────

    public static void executeCreateWater(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        float range = spell.getRange();
        BlockPos center = BlockPos.containing(origin);
        boolean hasWall = spell.hasWall();
        boolean hasFloor = spell.hasFloor();
        int fillLevel = spell.getFillLevel();

        int radius = (int) range;
        int fillRadius = fillLevel > 0
                ? ModConfig.effectInt(SpellEffect.CREATE_WATER, "fillRadiusBase", 25)
                    + (fillLevel * ModConfig.effectInt(SpellEffect.CREATE_WATER, "fillRadiusPerFill", 5))
                : radius;
        int placed = 0;

        // With Fill, the water is poured rather than sprayed: it settles to the floor and rises a
        // layer at a time, so a hole fills to ground level and a room fills to its ceiling. Cast in
        // the open there is nothing holding it, and nothing is placed.
        boolean flood = usesFlood(fillLevel, hasWall, hasFloor);
        Iterable<BlockPos> targets = flood
                ? FloodFill.contained(level, center, fillRadius,
                        ModConfig.effectInt(SpellEffect.CREATE_WATER, "fillBudget", 4096),
                        pos -> isOpenSpace(level, pos))
                : BlockPos.betweenClosed(
                        center.offset(-fillRadius, -fillRadius, -fillRadius),
                        center.offset(fillRadius, fillRadius, fillRadius));

        for (BlockPos pos : targets) {
            if (!level.isLoaded(pos)) continue;

            if (flood || isInShapeRange(pos, center, origin, radius, fillRadius, hasWall, hasFloor, false)) {
                BlockState state = level.getBlockState(pos);
                if (state.isAir() || state.canBeReplaced()) {
                    level.setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
                    placed++;
                }
            }
        }

        if (placed > 0) {
            level.playSound(null, origin.x, origin.y, origin.z,
                    SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 1.0f, 1.0f);
        }
    }

    // ─── EVAPORATE WATER ────────────────────────────────────────────────────────

    public static void executeEvaporateWater(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        float range = spell.getRange();
        BlockPos center = BlockPos.containing(origin);
        boolean hasWall = spell.hasWall();
        boolean hasFloor = spell.hasFloor();
        int fillLevel = spell.getFillLevel();

        int radius = (int) range;
        int fillRadius = fillLevel > 0
                ? ModConfig.effectInt(SpellEffect.EVAPORATE_WATER, "fillRadiusBase", 25)
                    + (fillLevel * ModConfig.effectInt(SpellEffect.EVAPORATE_WATER, "fillRadiusPerFill", 5))
                : radius;
        int removed = 0;

        // With Fill, this drains the body of water it was cast into and stops at its shore, instead
        // of punching a sphere out of it and leaving standing walls of water behind.
        boolean flood = usesFlood(fillLevel, hasWall, hasFloor);
        Iterable<BlockPos> targets = flood
                ? FloodFill.connected(level, center,
                        FloodFill.seedsAround(level, center, pos -> level.getBlockState(pos).is(Blocks.WATER)),
                        fillRadius, ModConfig.effectInt(SpellEffect.EVAPORATE_WATER, "fillBudget", 4096),
                        pos -> level.getBlockState(pos).is(Blocks.WATER))
                : BlockPos.betweenClosed(
                        center.offset(-fillRadius, -fillRadius, -fillRadius),
                        center.offset(fillRadius, fillRadius, fillRadius));

        for (BlockPos pos : targets) {
            if (!level.isLoaded(pos)) continue;

            if (flood || isInShapeRange(pos, center, origin, radius, fillRadius, hasWall, hasFloor, false)) {
                BlockState state = level.getBlockState(pos);
                if (state.is(Blocks.WATER)) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    removed++;
                }
            }
        }

        if (removed > 0) {
            level.playSound(null, origin.x, origin.y, origin.z,
                    SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0f, 0.8f);
        }
    }

    // ─── MARK ───────────────────────────────────────────────────────────────────

    public static void executeMark(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        MarkManager.setMark(caster.getUUID(), origin);

        level.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.5f, 1.5f);

        if (level instanceof ServerLevel serverLevel) {
            SpellParticles.ring(serverLevel, origin, ParticleTypes.FLAME, 20, 0.5, 0.5);
        }
    }

    // ─── RECALL ─────────────────────────────────────────────────────────────────

    public static void executeRecall(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        Vec3 markPos = MarkManager.getMark(caster.getUUID());
        if (markPos == null) return;

        level.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);

        caster.teleportTo(markPos.x, markPos.y, markPos.z);
        caster.fallDistance = 0;

        level.playSound(null, markPos.x, markPos.y, markPos.z,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);

        if (level instanceof ServerLevel serverLevel) {
            SpellParticles.burst(serverLevel, markPos.add(0, 1, 0), ParticleTypes.PORTAL, 30, 1.0, 0.5);
        }
    }

    // ─── PICKUP ─────────────────────────────────────────────────────────────────

    public static void executePickup(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        float range = (float) ModConfig.effectDouble(SpellEffect.PICKUP, "range", 5.0)
                + spell.getModifierCount(SpellModifier.INCREASED_AREA)
                        * (float) ModConfig.modifierDouble(SpellModifier.INCREASED_AREA, "rangePerStack", 1.0);

        AABB box = new AABB(origin.subtract(range, range, range), origin.add(range, range, range));
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, box,
                entity -> entity.position().distanceTo(origin) <= range);

        for (ItemEntity itemEntity : items) {
            ItemStack stack = itemEntity.getItem();
            if (!stack.isEmpty() && caster.getInventory().add(stack)) {
                if (level instanceof ServerLevel serverLevel) {
                    Vec3 itemPos = itemEntity.position();
                    Vec3 casterPos = caster.position().add(0, 1, 0);
                    SpellParticles.line(serverLevel, itemPos, casterPos, ParticleTypes.HAPPY_VILLAGER, (int) (itemPos.distanceTo(casterPos) * 2));
                }
                itemEntity.discard();
                level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                        SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2f,
                        (level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.7f + 1.0f);
            }
        }
    }

    // ─── SHIELD ─────────────────────────────────────────────────────────────────

    public static void executeShield(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        float range = spell.getRange();
        float absorptionAmount = (float) ModConfig.effectDouble(SpellEffect.SHIELD, "absorptionBase", 2.0)
                + (spellPower * (float) ModConfig.effectDouble(SpellEffect.SHIELD, "absorptionScale", 0.15));

        SpellSounds.shield(level, origin);

        AABB box = new AABB(origin.subtract(range, range, range), origin.add(range, range, range));
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, box,
                entity -> entity.position().distanceTo(origin) <= range);

        for (LivingEntity entity : entities) {
            float currentAbsorption = entity.getAbsorptionAmount();
            entity.setAbsorptionAmount(Math.min(currentAbsorption + absorptionAmount,
                    (float) ModConfig.effectDouble(SpellEffect.SHIELD, "absorptionCap", 20.0)));
        }

        if (!entities.isEmpty() && level instanceof ServerLevel serverLevel) {
            SpellParticles.sphere(serverLevel, origin.add(0, 1, 0), ParticleTypes.END_ROD, 15, range);
        }
    }

    // ─── CONJURE ────────────────────────────────────────────────────────────────

    public static void executeConjure(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        int areaLevel = spell.getModifierCount(SpellModifier.INCREASED_AREA);
        boolean hasWall = spell.hasWall();
        boolean hasFloor = spell.hasFloor();
        int fillLevel = spell.getFillLevel();

        level.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 0.7f, 1.2f);

        BlockPos centerPos = BlockPos.containing(origin);
        BlockState targetState = level.getBlockState(centerPos);

        List<? extends String> allowedBlocks = ModConfig.getConjureAllowedBlocks();
        BlockState conjureState;

        if (!targetState.isAir() && allowedBlocks.contains(
                targetState.getBlock().builtInRegistryHolder().key().identifier().toString())) {
            conjureState = targetState;
        } else {
            conjureState = Blocks.STONE.defaultBlockState();
        }

        int expandRadius = ModConfig.effectInt(SpellEffect.CONJURE, "expandRadiusBase", 1) + areaLevel;
        int fillRadius = fillLevel > 0
                ? ModConfig.effectInt(SpellEffect.CONJURE, "fillRadiusBase", 25)
                    + (fillLevel * ModConfig.effectInt(SpellEffect.CONJURE, "fillRadiusPerFill", 5))
                : 0;

        int effectiveRadius = fillLevel > 0 ? fillRadius : expandRadius;

        // With Fill, Conjure packs the space it was cast into from the floor up: it fills the hole
        // or the room it is standing in and stops at the brim, rather than burying everything in
        // reach under a ball of stone.
        boolean flood = usesFlood(fillLevel, hasWall, hasFloor);
        Iterable<BlockPos> targets = flood
                ? FloodFill.contained(level, centerPos, fillRadius,
                        ModConfig.effectInt(SpellEffect.CONJURE, "fillBudget", 4096),
                        pos -> isOpenSpace(level, pos))
                : BlockPos.betweenClosed(
                        centerPos.offset(-effectiveRadius, -effectiveRadius, -effectiveRadius),
                        centerPos.offset(effectiveRadius, effectiveRadius, effectiveRadius));

        for (BlockPos pos : targets) {
            if (!level.isLoaded(pos)) continue;

            if (flood || isInShapeRange(pos, centerPos, origin, expandRadius, fillRadius, hasWall, hasFloor, false)) {
                BlockState existingState = level.getBlockState(pos);
                // Replaceable blocks (light, grass, snow layers) count as free space whether or not
                // Fill is on: testing only for air here made plain Conjure refuse to build through
                // its own Light effect.
                boolean canPlace = existingState.isAir() || existingState.canBeReplaced();

                if (canPlace) {
                    level.setBlockAndUpdate(pos, conjureState);
                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.ENCHANT,
                                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                                3, 0.2, 0.2, 0.2, 0.1);
                    }
                }
            }
        }
    }

    // ─── GROWTH ─────────────────────────────────────────────────────────────────

    public static void executeGrowth(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        float range = spell.getRange();
        int prolongedLevel = spell.getProlongedLevel();
        int powerLevel = spell.getPowerLevel();

        level.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.CROP_BREAK, SoundSource.PLAYERS, 0.7f, 1.2f);

        BlockPos centerPos = BlockPos.containing(origin);
        for (BlockPos pos : BlockPos.betweenClosed(
                centerPos.offset((int) -range, (int) -range, (int) -range),
                centerPos.offset((int) range, (int) range, (int) range))) {
            if (!level.isLoaded(pos)) continue;

            if (pos.getCenter().distanceTo(origin) <= range) {
                BlockState state = level.getBlockState(pos);
                Block block = state.getBlock();

                if (block instanceof CropBlock cropBlock) {
                    int age = cropBlock.getAge(state);
                    int maxAge = cropBlock.getMaxAge();
                    int newAge = Math.min(age + ModConfig.effectInt(SpellEffect.GROWTH, "ageBase", 1)
                            + prolongedLevel
                            + powerLevel * ModConfig.effectInt(SpellEffect.GROWTH, "agePerPower", 1), maxAge);
                    if (newAge != age) {
                        level.setBlock(pos, cropBlock.getStateForAge(newAge), 3);
                    }
                } else if (state.is(Blocks.VINE)) {
                    for (int i = 0; i < 4; i++) {
                        BlockPos spreadPos = pos.offset(
                                level.getRandom().nextInt(3) - 1,
                                level.getRandom().nextInt(3) - 1,
                                level.getRandom().nextInt(3) - 1
                        );
                        if (level.getBlockState(spreadPos).isAir()) {
                            level.setBlock(spreadPos, Blocks.VINE.defaultBlockState(), 3);
                        }
                    }
                }
            }
        }
    }

    // ─── CLEANSE ────────────────────────────────────────────────────────────────

    public static void executeCleanse(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        float range = spell.getRange();

        level.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.8f, 1.1f);

        AABB box = new AABB(origin.subtract(range, range, range), origin.add(range, range, range));
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, box,
                entity -> entity.position().distanceTo(origin) <= range);

        for (LivingEntity entity : entities) {
            var effects = entity.getActiveEffects();
            var toRemove = new ArrayList<Holder<MobEffect>>();

            for (var effect : effects) {
                if (!effect.getEffect().value().isBeneficial() ||
                        effect.getEffect().is(MobEffects.POISON) ||
                        effect.getEffect().is(MobEffects.WITHER) ||
                        effect.getEffect().is(MobEffects.WEAKNESS) ||
                        effect.getEffect().is(MobEffects.SLOWNESS) ||
                        effect.getEffect().is(MobEffects.BLINDNESS)) {
                    toRemove.add(effect.getEffect());
                }
            }

            for (var effect : toRemove) {
                entity.removeEffect(effect);
            }

            if (!toRemove.isEmpty() && level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        entity.getX(), entity.getY() + 1, entity.getZ(),
                        5, 0.3, 0.3, 0.3, 0.1);
            }
        }
    }

    // ─── CHARM ──────────────────────────────────────────────────────────────────

    public static void executeCharm(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        float range = spell.getRange();
        int prolongedLevel = spell.getProlongedLevel();
        int powerLevel = spell.getPowerLevel();
        int duration = ModConfig.effectInt(SpellEffect.CHARM, "durationBase", 100)
                + (prolongedLevel * ModConfig.effectInt(SpellEffect.CHARM, "durationPerProlonged", 40))
                + (powerLevel * ModConfig.effectInt(SpellEffect.CHARM, "durationPerPower", 40));

        level.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.7f, 1.3f);

        AABB box = new AABB(origin.subtract(range, range, range), origin.add(range, range, range));
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, box,
                entity -> entity != caster && entity.position().distanceTo(origin) <= range && entity instanceof Mob);

        for (LivingEntity entity : entities) {
            // Real pacify: drop the mob's current target and block new target acquisition for the
            // duration (enforced by the LivingChangeTargetEvent handler), with nausea as the tell.
            if (entity instanceof net.minecraft.world.entity.Mob mob) {
                buildaspell.spell.MobSpellState.pacify(mob, duration);
            }
            entity.addEffect(new MobEffectInstance(MobEffects.NAUSEA, duration, 0, false, true));
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.ENCHANT,
                        entity.getX(), entity.getY() + entity.getEyeHeight() * 0.5, entity.getZ(),
                        3, 0.2, 0.2, 0.2, 0.1);
            }
        }
    }

    // ─── BLIND ──────────────────────────────────────────────────────────────────

    public static void executeBlind(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        float range = spell.getRange();
        int powerLevel = spell.getPowerLevel();
        int prolongedLevel = spell.getProlongedLevel();
        int duration = ModConfig.effectInt(SpellEffect.BLIND, "durationBase", 100)
                + (powerLevel * ModConfig.effectInt(SpellEffect.BLIND, "durationPerPower", 20))
                + (prolongedLevel * ModConfig.effectInt(SpellEffect.BLIND, "durationPerProlonged", 40));

        level.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.AMBIENT_CAVE.value(), SoundSource.PLAYERS, 0.6f, 0.9f);

        AABB box = new AABB(origin.subtract(range, range, range), origin.add(range, range, range));
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, box,
                entity -> entity != caster && entity.position().distanceTo(origin) <= range);

        for (LivingEntity entity : entities) {
            entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, duration, 0, false, true));
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SMOKE,
                        entity.getX(), entity.getY() + entity.getEyeHeight(), entity.getZ(),
                        4, 0.15, 0.15, 0.15, 0.1);
            }
        }
    }

    // ─── HELPER METHODS ─────────────────────────────────────────────────────────

    /**
     * Applies a potion effect to living entities inside the spell's radius. Whether the caster is a
     * valid target comes from {@link SpellEffect#isBeneficial()}: buffs (Speed, Regenerate, Night
     * Vision, ...) include the caster, while hostile effects (Levitation and the debuff family)
     * never touch the person casting them.
     */
    private static void applyEffectInArea(Player caster, Level level, Vec3 origin, Spell spell,
                                           SpellEffect sourceEffect, Holder<MobEffect> effect, int amplifier) {
        float range = spell.getRange();
        int powerLevel = spell.getPowerLevel();
        int prolongedLevel = spell.getProlongedLevel();
        int duration = ModConfig.sharedEffectInt("areaDurationBase", 100)
                + (powerLevel * ModConfig.sharedEffectInt("areaDurationPerPower", 20))
                + (prolongedLevel * ModConfig.sharedEffectInt("areaDurationPerProlonged", 40));

        AABB box = new AABB(origin.subtract(range, range, range), origin.add(range, range, range));
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, box,
                entity -> (sourceEffect.isBeneficial() || entity != caster)
                        && entity.position().distanceTo(origin) <= range);

        for (LivingEntity entity : entities) {
            entity.addEffect(new MobEffectInstance(effect, duration, amplifier));
        }
    }

    /**
     * Area debuff with an explicit duration formula (used by Slow/Weaken, which — unlike the
     * shared-duration buffs — want their own per-effect duration knobs). Status effects are not
     * damage, so Nullify does not suppress them.
     */
    private static void applyDebuffInArea(Player caster, Level level, Vec3 origin, Spell spell,
                                          Holder<MobEffect> effect, int amplifier,
                                          int durationBase, int durationPerPower, int durationPerProlonged) {
        float range = spell.getRange();
        int duration = durationBase
                + (spell.getPowerLevel() * durationPerPower)
                + (spell.getProlongedLevel() * durationPerProlonged);

        AABB box = new AABB(origin.subtract(range, range, range), origin.add(range, range, range));
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, box,
                entity -> entity != caster && entity.position().distanceTo(origin) <= range);

        for (LivingEntity entity : entities) {
            entity.addEffect(new MobEffectInstance(effect, duration, amplifier));
        }
    }

    public static void executeSlow(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        applyDebuffInArea(caster, level, origin, spell, MobEffects.SLOWNESS,
                Math.min(spell.getPowerLevel(), ModConfig.effectInt(SpellEffect.SLOW, "amplifierCap", 3)),
                ModConfig.effectInt(SpellEffect.SLOW, "durationBase", 100),
                ModConfig.effectInt(SpellEffect.SLOW, "durationPerPower", 20),
                ModConfig.effectInt(SpellEffect.SLOW, "durationPerProlonged", 40));
    }

    public static void executeWeaken(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        applyDebuffInArea(caster, level, origin, spell, MobEffects.WEAKNESS,
                Math.min(spell.getPowerLevel(), ModConfig.effectInt(SpellEffect.WEAKEN, "amplifierCap", 3)),
                ModConfig.effectInt(SpellEffect.WEAKEN, "durationBase", 120),
                ModConfig.effectInt(SpellEffect.WEAKEN, "durationPerPower", 20),
                ModConfig.effectInt(SpellEffect.WEAKEN, "durationPerProlonged", 40));
    }

    public static void executeStrengthen(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        applyEffectInArea(caster, level, origin, spell, SpellEffect.STRENGTHEN, MobEffects.STRENGTH,
                Math.min(spell.getPowerLevel(), ModConfig.effectInt(SpellEffect.STRENGTHEN, "amplifierCap", 3)));
    }

    public static void executeRegenerate(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        applyEffectInArea(caster, level, origin, spell, SpellEffect.REGENERATE, MobEffects.REGENERATION,
                Math.min(spell.getPowerLevel(), ModConfig.effectInt(SpellEffect.REGENERATE, "amplifierCap", 2)));
    }

    public static void executeResist(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        applyEffectInArea(caster, level, origin, spell, SpellEffect.RESIST, MobEffects.RESISTANCE,
                Math.min(spell.getPowerLevel(), ModConfig.effectInt(SpellEffect.RESIST, "amplifierCap", 3)));
    }

    public static void executeNightVision(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        applyEffectInArea(caster, level, origin, spell, SpellEffect.NIGHT_VISION, MobEffects.NIGHT_VISION, 0);
    }

    public static void executeWaterBreathing(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        applyEffectInArea(caster, level, origin, spell, SpellEffect.WATER_BREATHING, MobEffects.WATER_BREATHING, 0);
    }

    public static void executeRoot(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        float range = spell.getRange();
        int duration = ModConfig.effectInt(SpellEffect.ROOT, "durationBase", 80)
                + (spell.getPowerLevel() * ModConfig.effectInt(SpellEffect.ROOT, "durationPerPower", 20))
                + (spell.getProlongedLevel() * ModConfig.effectInt(SpellEffect.ROOT, "durationPerProlonged", 40));
        // Slowness at a high amplifier collapses ground speed to near-zero while the negative
        // jump-boost (very high amplifier wraps to a downward jump impulse) stops the target
        // hopping out of the snare — together they read as "rooted in place".
        int slowAmp = ModConfig.effectInt(SpellEffect.ROOT, "slownessAmplifier", 6);
        int jumpAmp = ModConfig.effectInt(SpellEffect.ROOT, "jumpPreventAmplifier", 128);

        level.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.SCULK_BLOCK_PLACE, SoundSource.PLAYERS, 0.8f, 0.6f);

        AABB box = new AABB(origin.subtract(range, range, range), origin.add(range, range, range));
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, box,
                entity -> entity != caster && entity.position().distanceTo(origin) <= range);

        for (LivingEntity entity : entities) {
            entity.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, duration, slowAmp));
            entity.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, duration, jumpAmp));
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SCULK_CHARGE_POP,
                        entity.getX(), entity.getY() + 0.1, entity.getZ(),
                        6, 0.3, 0.1, 0.3, 0.0);
            }
        }
    }

    public static void executeGrapple(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        float strength = (float) ModConfig.effectDouble(SpellEffect.GRAPPLE, "strengthBase", 1.4)
                + (spell.getPowerLevel() * (float) ModConfig.effectDouble(SpellEffect.GRAPPLE, "strengthPerPower", 0.3));

        Vec3 toTarget = origin.subtract(caster.position());
        if (toTarget.lengthSqr() < 1.0e-4) return;
        Vec3 dir = toTarget.normalize();

        // Add a little lift so the caster arcs toward the anchor instead of skidding along the
        // ground and snagging on the first block edge.
        double lift = ModConfig.effectDouble(SpellEffect.GRAPPLE, "liftBonus", 0.35);
        caster.setDeltaMovement(dir.x * strength, dir.y * strength + lift, dir.z * strength);
        caster.hurtMarked = true;
        caster.fallDistance = 0;

        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.PLAYERS, 0.8f, 1.2f);
        if (level instanceof ServerLevel serverLevel) {
            SpellParticles.blinkTrail(serverLevel, caster.position(), origin);
        }
    }

    public static void executeGust(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        float range = spell.getRange();
        float strength = (float) ModConfig.effectDouble(SpellEffect.GUST, "strengthBase", 1.2)
                + (spell.getPowerLevel() * (float) ModConfig.effectDouble(SpellEffect.GUST, "strengthPerPower", 0.3));
        double lift = ModConfig.effectDouble(SpellEffect.GUST, "liftBonus", 0.4);
        // cosine of the half-angle of the cone the wind fills, measured from the caster's gaze.
        double minDot = ModConfig.effectDouble(SpellEffect.GUST, "coneMinDot", 0.3);

        Vec3 look = caster.getLookAngle();

        level.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.BREEZE_WIND_CHARGE_BURST, SoundSource.PLAYERS, 1.0f, 0.9f);

        AABB box = new AABB(origin.subtract(range, range, range), origin.add(range, range, range));
        List<Entity> entities = level.getEntitiesOfClass(Entity.class, box,
                entity -> entity != caster && entity.position().distanceTo(origin) <= range);

        for (Entity entity : entities) {
            Vec3 toEntity = entity.position().subtract(caster.position());
            if (toEntity.lengthSqr() < 1.0e-4 || look.dot(toEntity.normalize()) < minDot) continue;
            Vec3 push = look.scale(strength).add(0, lift, 0);
            Vec3 motion = entity.getDeltaMovement();
            entity.setDeltaMovement(motion.x + push.x, push.y, motion.z + push.z);
            entity.hurtMarked = true;
        }

        if (level instanceof ServerLevel serverLevel) {
            Vec3 puff = caster.position().add(look.scale(2.0)).add(0, caster.getEyeHeight() * 0.5, 0);
            serverLevel.sendParticles(ParticleTypes.CLOUD, puff.x, puff.y, puff.z,
                    20, 0.6, 0.4, 0.6, 0.1);
        }
    }

    /**
     * Whether this cast should use the flood searches instead of the plain shape test.
     *
     * <p>Wall and Floor describe a shape outright, and they already win over Fill in
     * {@link #isInShapeRange}, so Fill only has anything to say when neither is present.
     */
    private static boolean usesFlood(int fillLevel, boolean hasWall, boolean hasFloor) {
        return fillLevel > 0 && !hasWall && !hasFloor;
    }

    /**
     * Space a Fill can occupy. Deliberately the same test the placement below uses, so the flood
     * never counts on room it will then refuse to build in.
     */
    private static boolean isOpenSpace(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.canBeReplaced();
    }

    /** A block this cast of Break would actually remove — the flood walks these and nothing else. */
    private static boolean isBreakable(Level level, BlockPos pos, float maxHardness) {
        BlockState state = level.getBlockState(pos);
        // Matches the removal below: spell lights are unbreakable by destroy speed but must still be
        // clearable, so they count as part of the mass rather than a wall the flood stops at.
        if (state.is(Blocks.LIGHT) || state.is(ModBlocks.SPELL_LIGHT.get())) {
            return true;
        }
        if (state.isAir()) {
            return false;
        }
        float speed = state.getDestroySpeed(level, pos);
        return speed >= 0 && speed <= maxHardness;
    }

    private static boolean isInShapeRange(BlockPos pos, BlockPos center, Vec3 origin,
                                           int radius, int fillRadius,
                                           boolean hasWall, boolean hasFloor, boolean hasFill) {
        double distance = pos.getCenter().distanceTo(origin);

        if (hasWall) {
            return Math.abs(pos.getX() - center.getX()) <= radius &&
                    Math.abs(pos.getZ() - center.getZ()) <= radius;
        } else if (hasFloor) {
            return Math.abs(pos.getY() - center.getY()) <= 1 && distance <= radius;
        } else if (hasFill) {
            return distance <= fillRadius;
        } else {
            return pos.distSqr(center) <= (long) radius * radius;
        }
    }

    private static ItemStack createToolForBreak(Level level, boolean hasGentleness, int fortuneLevel) {
        if (hasGentleness) {
            ItemStack tool = new ItemStack(Items.DIAMOND_PICKAXE);
            tool.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(Enchantments.SILK_TOUCH), 1);
            return tool;
        } else if (fortuneLevel > 0) {
            ItemStack tool = new ItemStack(Items.DIAMOND_PICKAXE);
            tool.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(Enchantments.FORTUNE), fortuneLevel);
            return tool;
        }
        return ItemStack.EMPTY;
    }
}
