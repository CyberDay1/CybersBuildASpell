package buildaspell.spell.execution;

import buildaspell.config.ModConfig;
import buildaspell.entity.*;
import buildaspell.portal.PortalInfo;
import buildaspell.portal.PortalManager;
import buildaspell.spell.Spell;
import buildaspell.spell.SpellModifier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class ComboExecutors {

    public static void executeBlackHole(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        float range = spell.getRange() * (float) ModConfig.comboDouble("black_hole", "rangeMultiplier", 2.5);
        boolean nullify = spell.hasNullify();
        int fortuneLevel = spell.getFortuneLevel();

        float pullStrength = (float) ModConfig.comboDouble("black_hole", "pullStrength", 0.5);
        // Per-hit crush damage now scales with Increased Power (it previously ignored it, which is
        // why investing in the singularity felt flat vs the tornado, whose forces scale with power).
        int powerLevel = spell.getPowerLevel();
        float damagePerTick = spellPower * (float) (ModConfig.comboDouble("black_hole", "damagePerTickScale", 0.20)
                + powerLevel * ModConfig.comboDouble("black_hole", "damagePerTickPerPower", 0.05));
        int duration = ModConfig.comboInt("black_hole", "durationTicks", 90)
                + spell.getDurationLevel() * ModConfig.comboInt("black_hole", "durationPerDuration", 30);

        if (level instanceof ServerLevel serverLevel) {
            BlackHoleEntity blackHole = new BlackHoleEntity(
                    serverLevel, caster, origin,
                    range, pullStrength, damagePerTick,
                    duration, nullify, fortuneLevel
            );
            serverLevel.addFreshEntity(blackHole);
        }

        level.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 1.0f, 0.5f);
    }

    public static void executeTornado(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        // Floor the funnel radius so a small spell range still produces a usable suction/throw
        // zone — testers found the pull felt dead when the radius collapsed near the caster.
        float range = Math.max(
                spell.getRange() * (float) ModConfig.comboDouble("tornado", "rangeMultiplier", 2.5),
                10.0f);
        boolean nullify = spell.hasNullify();
        int fortuneLevel = spell.getFortuneLevel();

        int powerLevel = spell.getPowerLevel();
        // Forces are multiplied by 0.1 / 0.05 inside TornadoEntity.tick(), so these
        // need to be large enough to overcome those dampeners and Minecraft gravity (0.08/tick).
        float pullStrength = (float) (ModConfig.comboDouble("tornado", "pullBase", 1.2)
                + powerLevel * ModConfig.comboDouble("tornado", "pullPerPower", 0.3));
        float liftStrength = (float) (ModConfig.comboDouble("tornado", "liftBase", 2.5)
                + powerLevel * ModConfig.comboDouble("tornado", "liftPerPower", 0.5));
        float spinStrength = (float) (ModConfig.comboDouble("tornado", "spinBase", 0.8)
                + powerLevel * ModConfig.comboDouble("tornado", "spinPerPower", 0.2));
        int duration = ModConfig.comboInt("tornado", "durationBase", 100)
                + powerLevel * ModConfig.comboInt("tornado", "durationPerPower", 20)
                + spell.getDurationLevel() * ModConfig.comboInt("tornado", "durationPerDuration", 40);

        if (level instanceof ServerLevel serverLevel) {
            TornadoEntity tornado = new TornadoEntity(
                    serverLevel, caster, origin,
                    range, duration, pullStrength,
                    liftStrength, spinStrength,
                    spellPower, nullify, fortuneLevel
            );
            serverLevel.addFreshEntity(tornado);
        }

        level.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 1.0f, 0.7f);
    }

    public static void executeCreativeFlight(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        int powerLevel = spell.getPowerLevel();
        int prolongedLevel = spell.getProlongedLevel();

        int duration = ModConfig.comboInt("creative_flight", "durationBase", 200)
                + (powerLevel * ModConfig.comboInt("creative_flight", "durationPerPower", 40))
                + (prolongedLevel * ModConfig.comboInt("creative_flight", "durationPerProlonged", 80));

        if (level instanceof ServerLevel serverLevel) {
            FlightDurationEntity flightTracker = new FlightDurationEntity(
                    serverLevel, caster, duration
            );
            serverLevel.addFreshEntity(flightTracker);
        }

        level.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8f, 1.2f);
    }

    public static void executeIronGolem(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        int powerLevel = spell.getPowerLevel();
        int golemCount = ModConfig.comboInt("iron_golem", "countBase", 1)
                + Math.min(powerLevel / ModConfig.comboInt("iron_golem", "countPerPowerDivisor", 3),
                        ModConfig.comboInt("iron_golem", "countBonusCap", 1));

        level.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.IRON_GOLEM_HURT, SoundSource.PLAYERS, 1.0f, 0.8f);

        if (level instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 16; i++) {
                double angle = (Math.PI * 2 * i) / 16;
                double vx = Math.cos(angle) * 0.5;
                double vz = Math.sin(angle) * 0.5;
                serverLevel.sendParticles(ParticleTypes.CRIT,
                        origin.x, origin.y + 1, origin.z,
                        2, vx, 0.4, vz, 0.1);
            }
        }

        for (int i = 0; i < golemCount; i++) {
            double angle = (2 * Math.PI * i) / golemCount;
            double golemOffset = ModConfig.comboDouble("iron_golem", "spawnOffset", 2.5);
            double offsetX = Math.cos(angle) * golemOffset;
            double offsetZ = Math.sin(angle) * golemOffset;

            IronGolem golem = EntityType.IRON_GOLEM.create(level);
            if (golem != null) {
                golem.setPos(origin.x + offsetX, origin.y, origin.z + offsetZ);
                golem.setPlayerCreated(true);
                level.addFreshEntity(golem);
            }
        }
    }

    public static void executeVexes(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        int powerLevel = spell.getPowerLevel();
        int vexCount = Math.min(ModConfig.comboInt("vexes", "countBase", 2) + powerLevel,
                ModConfig.comboInt("vexes", "countCap", 4));
        // Duration extends how long the summons persist before they dissolve.
        int vexLifeSeconds = ModConfig.comboInt("vexes", "lifeSeconds", 60)
                + spell.getDurationLevel() * ModConfig.comboInt("vexes", "lifePerDuration", 30);

        level.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.VEX_AMBIENT, SoundSource.PLAYERS, 1.0f, 1.0f);

        if (level instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 20; i++) {
                double angle = (Math.PI * 2 * i) / 20;
                double vx = Math.cos(angle) * 0.5;
                double vz = Math.sin(angle) * 0.5;
                serverLevel.sendParticles(ParticleTypes.ENCHANT,
                        origin.x, origin.y + 1, origin.z,
                        2, vx, 0.3, vz, 0.1);
            }
        }

        for (int i = 0; i < vexCount; i++) {
            double angle = (2 * Math.PI * i) / vexCount;
            double vexOffset = ModConfig.comboDouble("vexes", "spawnOffset", 2.0);
            double offsetX = Math.cos(angle) * vexOffset;
            double offsetZ = Math.sin(angle) * vexOffset;

            Vex vex = EntityType.VEX.create(level);
            if (vex != null) {
                vex.setPos(origin.x + offsetX, origin.y + 1, origin.z + offsetZ);
                vex.setLimitedLife(20 * vexLifeSeconds);
                // Lifetime 0: limited life above handles despawn; the tag stops caster re-aggro.
                buildaspell.spell.MobSpellState.tagSummon(vex, caster, 0);
                level.addFreshEntity(vex);
            }
        }
    }

    public static void executeSkeletons(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        int powerLevel = spell.getPowerLevel();
        int skeletonCount = ModConfig.comboInt("skeletons", "countBase", 2)
                + Math.min(powerLevel / ModConfig.comboInt("skeletons", "countPerPowerDivisor", 2),
                        ModConfig.comboInt("skeletons", "countBonusCap", 1));
        // Duration extends how long the summons persist before they dissolve.
        int skeletonLifeSeconds = ModConfig.comboInt("skeletons", "lifeSeconds", 60)
                + spell.getDurationLevel() * ModConfig.comboInt("skeletons", "lifePerDuration", 30);

        level.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.SKELETON_AMBIENT, SoundSource.PLAYERS, 1.0f, 0.9f);

        if (level instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 16; i++) {
                double angle = (Math.PI * 2 * i) / 16;
                double vx = Math.cos(angle) * 0.4;
                double vz = Math.sin(angle) * 0.4;
                serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        origin.x, origin.y + 1, origin.z,
                        1, vx, 0.3, vz, 0.1);
            }
        }

        for (int i = 0; i < skeletonCount; i++) {
            double angle = (2 * Math.PI * i) / skeletonCount;
            double skeletonOffset = ModConfig.comboDouble("skeletons", "spawnOffset", 2.0);
            double offsetX = Math.cos(angle) * skeletonOffset;
            double offsetZ = Math.sin(angle) * skeletonOffset;

            Skeleton skeleton = EntityType.SKELETON.create(level);
            if (skeleton != null) {
                skeleton.setPos(origin.x + offsetX, origin.y, origin.z + offsetZ);
                skeleton.setPersistenceRequired();
                // Summons never re-aggro their caster and dissolve after their lifetime runs out.
                buildaspell.spell.MobSpellState.tagSummon(skeleton, caster,
                        20 * skeletonLifeSeconds);
                level.addFreshEntity(skeleton);
                skeleton.setTarget(null);
            }
        }
    }

    public static void executeVindicators(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        int powerLevel = spell.getPowerLevel();
        int vindicatorCount = ModConfig.comboInt("vindicators", "countBase", 2)
                + Math.min(powerLevel / ModConfig.comboInt("vindicators", "countPerPowerDivisor", 2),
                        ModConfig.comboInt("vindicators", "countBonusCap", 1));
        // Duration extends how long the summons persist before they dissolve.
        int vindicatorLifeSeconds = ModConfig.comboInt("vindicators", "lifeSeconds", 60)
                + spell.getDurationLevel() * ModConfig.comboInt("vindicators", "lifePerDuration", 30);

        level.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.VINDICATOR_AMBIENT, SoundSource.PLAYERS, 1.0f, 0.9f);

        if (level instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 16; i++) {
                double angle = (Math.PI * 2 * i) / 16;
                double vx = Math.cos(angle) * 0.4;
                double vz = Math.sin(angle) * 0.4;
                serverLevel.sendParticles(ParticleTypes.CRIMSON_SPORE,
                        origin.x, origin.y + 1, origin.z,
                        2, vx, 0.3, vz, 0.1);
            }
        }

        for (int i = 0; i < vindicatorCount; i++) {
            double angle = (2 * Math.PI * i) / vindicatorCount;
            double vindicatorOffset = ModConfig.comboDouble("vindicators", "spawnOffset", 2.0);
            double offsetX = Math.cos(angle) * vindicatorOffset;
            double offsetZ = Math.sin(angle) * vindicatorOffset;

            Vindicator vindicator = EntityType.VINDICATOR.create(level);
            if (vindicator != null) {
                vindicator.setPos(origin.x + offsetX, origin.y, origin.z + offsetZ);
                vindicator.setPersistenceRequired();
                // Summons never re-aggro their caster and dissolve after their lifetime runs out.
                buildaspell.spell.MobSpellState.tagSummon(vindicator, caster,
                        20 * vindicatorLifeSeconds);
                level.addFreshEntity(vindicator);
                vindicator.setTarget(null);
            }
        }
    }

    public static void executeVoidRift(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        int maxPortals = ModConfig.getMaxPortalsPerPlayer();
        if (maxPortals > 0) {
            List<PortalInfo> existing = PortalManager.getDiscoveredPortals(caster.getUUID());
            if (existing.size() >= maxPortals) {
                caster.sendSystemMessage(
                        Component.literal("Portal limit reached!").withStyle(ChatFormatting.RED));
                return;
            }
        }

        PortalEntity newPortal = new PortalEntity(level, origin, caster.getUUID());
        newPortal.setYRot(caster.getYRot());
        level.addFreshEntity(newPortal);

        level.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.PORTAL_TRIGGER, SoundSource.PLAYERS, 0.8f, 0.9f);

        caster.sendSystemMessage(Component.literal(
                "Portal created! Right-click to dial destinations."));

        serverLevel.sendParticles(ParticleTypes.PORTAL,
                origin.x, origin.y + 1.0, origin.z,
                50, 0.5, 1.0, 0.5, 0.5);
        serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
                origin.x, origin.y + 1.0, origin.z,
                20, 0.3, 0.8, 0.3, 0.3);
    }

    public static void executeFortress(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        int areaLevel = spell.getModifierCount(SpellModifier.INCREASED_AREA);
        int durationLevel = spell.getDurationLevel();

        float radius = (float) (ModConfig.comboDouble("fortress", "radiusBase", 5.0)
                + (areaLevel * ModConfig.comboDouble("fortress", "radiusPerArea", 1.0)));
        int duration = ModConfig.comboInt("fortress", "durationBase", 100)
                + (durationLevel * ModConfig.comboInt("fortress", "durationPerDuration", 50));

        BlockPos center = BlockPos.containing(origin);
        List<BlockPos> barrierBlocks = new ArrayList<>();

        // Voxelize a watertight hollow dome instead of sampling sphere angles. The old angular
        // sampling (PI/10 steps + int truncation) left wide gaps between blocks, so the "wall"
        // was full of holes the player could walk straight through. Here we walk the integer
        // block volume and place a barrier wherever the distance from centre falls inside a
        // one-block-thick shell band — that guarantees a solid, gap-free upper hemisphere.
        int r = Math.round(radius);
        double inner = radius - 0.5;
        double outer = radius + 0.5;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = 0; dy <= r; dy++) { // upper hemisphere only — open at the base, like a dome
                for (int dz = -r; dz <= r; dz++) {
                    double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (dist < inner || dist > outer) continue;

                    BlockPos pos = center.offset(dx, dy, dz);
                    if (!level.isLoaded(pos)) continue;
                    BlockState state = level.getBlockState(pos);

                    if (state.isAir()) {
                        level.setBlock(pos, Blocks.BARRIER.defaultBlockState(), 3);
                        barrierBlocks.add(pos.immutable());
                    }
                }
            }
        }

        if (level instanceof ServerLevel serverLevel && !barrierBlocks.isEmpty()) {
            FortressBarrierEntity fortressEntity = new FortressBarrierEntity(
                    serverLevel, barrierBlocks, duration, radius
            );
            fortressEntity.setPos(origin.x, origin.y, origin.z);
            serverLevel.addFreshEntity(fortressEntity);
        }

        level.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.2f);
    }

    public static void executeFlood(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        // Water evaporates instantly in the Nether — just play the sponge absorb sound
        if (level.dimension() == Level.NETHER) {
            level.playSound(null, origin.x, origin.y, origin.z,
                    SoundEvents.WET_SPONGE_DRIES, SoundSource.PLAYERS, 2.0f, 0.8f);
            return;
        }

        float range = spell.getRange();
        int radius = Math.min(Math.max((int) range, ModConfig.comboInt("flood", "radiusMin", 10)),
                ModConfig.comboInt("flood", "radiusMax", 15));
        int blockCap = ModConfig.comboInt("flood", "blockCap", 2000);
        BlockPos center = BlockPos.containing(origin);
        int blocksPlaced = 0;

        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-radius, -radius, -radius),
                center.offset(radius, radius, radius))) {
            if (blocksPlaced >= blockCap) break;
            if (!level.isLoaded(pos)) continue;
            if (pos.distSqr(center) <= radius * radius) {
                BlockState state = level.getBlockState(pos);
                if (state.isAir()) {
                    level.setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
                    blocksPlaced++;
                }
            }
        }

        level.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 2.0f, 0.5f);
    }

    public static void executeFloodLava(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        float range = spell.getRange();
        int radius = Math.min(Math.max((int) range, ModConfig.comboInt("flood_lava", "radiusMin", 10)),
                ModConfig.comboInt("flood_lava", "radiusMax", 15));
        int blockCap = ModConfig.comboInt("flood_lava", "blockCap", 2000);
        BlockPos center = BlockPos.containing(origin);
        int blocksPlaced = 0;

        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-radius, -radius, -radius),
                center.offset(radius, 0, radius))) {
            if (blocksPlaced >= blockCap) break;
            if (!level.isLoaded(pos)) continue;
            if (pos.distSqr(center) <= radius * radius) {
                BlockState state = level.getBlockState(pos);
                if (state.isAir() || state.canBeReplaced()) {
                    level.setBlock(pos, Blocks.LAVA.defaultBlockState(), 3);
                    blocksPlaced++;
                }
            }
        }

        level.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.BUCKET_EMPTY_LAVA, SoundSource.PLAYERS, 2.0f, 0.5f);
    }

    public static void executeEmergencyEscape(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        int maxAttempts = ModConfig.comboInt("emergency_escape", "maxAttempts", 50);
        int searchRadius = ModConfig.comboInt("emergency_escape", "searchRadius", 1000);

        for (int i = 0; i < maxAttempts; i++) {
            double angle = level.getRandom().nextDouble() * 2 * Math.PI;
            double distance = level.getRandom().nextDouble() * searchRadius;
            double offsetX = Math.cos(angle) * distance;
            double offsetZ = Math.sin(angle) * distance;

            double targetX = origin.x + offsetX;
            double targetZ = origin.z + offsetZ;

            if (!level.isLoaded(new BlockPos((int) targetX, 0, (int) targetZ))) continue;

            for (int y = 319; y >= -64; y--) {
                BlockPos checkPos = new BlockPos((int) targetX, y, (int) targetZ);
                BlockState groundState = level.getBlockState(checkPos);
                BlockState aboveState = level.getBlockState(checkPos.above());
                BlockState above2State = level.getBlockState(checkPos.above(2));

                if (!groundState.isAir() && groundState.isSolid()
                        && (aboveState.isAir() || aboveState.canBeReplaced())
                        && (above2State.isAir() || above2State.canBeReplaced())) {

                    Vec3 safePos = new Vec3(targetX, y + 1, targetZ);

                    level.playSound(null, origin.x, origin.y, origin.z,
                            SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 1.0f, 0.8f);

                    caster.teleportTo(safePos.x, safePos.y, safePos.z);
                    caster.fallDistance = 0;

                    level.playSound(null, safePos.x, safePos.y, safePos.z,
                            SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);

                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.PORTAL,
                                safePos.x, safePos.y + 1.0, safePos.z,
                                50, 0.5, 1.0, 0.5, 0.5);
                    }

                    return;
                }
            }
        }

        caster.sendSystemMessage(Component.literal("No safe location found!").withStyle(ChatFormatting.RED));
    }

    public static void executeMeteorStrike(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        // 6-10 very large fireballs raining down on the target area.
        // Increased Area widens the spread (via getRange) and adds more fireballs to fill it.
        int countRandomMax = ModConfig.comboInt("meteor_strike", "countRandom", 5);
        int count = ModConfig.comboInt("meteor_strike", "countBase", 6)
                + (countRandomMax > 0 ? serverLevel.getRandom().nextInt(countRandomMax) : 0)
                + spell.getModifierCount(SpellModifier.INCREASED_AREA)
                        * ModConfig.comboInt("meteor_strike", "countPerArea", 2);
        // Bigger explosions with more power; clamped so it stays "very large" but not absurd.
        int explosionPower = ModConfig.comboInt("meteor_strike", "explosionPowerBase", 4)
                + Math.min(spell.getPowerLevel(), ModConfig.comboInt("meteor_strike", "explosionPowerPerPowerCap", 2));
        double spread = Math.max(ModConfig.comboDouble("meteor_strike", "spreadMin", 6.0), spell.getRange());

        for (int i = 0; i < count; i++) {
            double angle = serverLevel.getRandom().nextDouble() * Math.PI * 2;
            double dist = serverLevel.getRandom().nextDouble() * spread;

            // Impact point scattered around the origin on the ground plane.
            Vec3 target = new Vec3(
                    origin.x + Math.cos(angle) * dist,
                    origin.y,
                    origin.z + Math.sin(angle) * dist);

            // Spawn high in the sky, roughly above (but pulled toward) the impact point. Each meteor
            // gets its own vertical tier (i * stagger) so no two fireballs overlap at spawn — without
            // this they collide on the spawn tick and detonate each other, shattering the volley.
            Vec3 spawn = new Vec3(
                    origin.x + Math.cos(angle) * dist * 0.3,
                    origin.y + ModConfig.comboInt("meteor_strike", "spawnHeightBase", 35)
                            + i * ModConfig.comboInt("meteor_strike", "spawnHeightStagger", 6),
                    origin.z + Math.sin(angle) * dist * 0.3);

            Vec3 dir = target.subtract(spawn).normalize();

            LargeFireball fireball = new LargeFireball(serverLevel, caster, dir, explosionPower);
            fireball.setPos(spawn.x, spawn.y, spawn.z);
            fireball.setDeltaMovement(dir.scale(ModConfig.comboDouble("meteor_strike", "fireballSpeed", 0.8)));
            serverLevel.addFreshEntity(fireball);
        }

        SpellParticles.impact(serverLevel, origin, ParticleTypes.LAVA, 30, 0.5);
        level.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.GHAST_WARN, SoundSource.PLAYERS, 1.0f, 0.5f);
    }

    public static void executeBlizzard(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        if (level instanceof ServerLevel serverLevel) {
            float radius = spell.getRange() * (float) ModConfig.comboDouble("blizzard", "rangeMultiplier", 2.0);
            int duration = ModConfig.comboInt("blizzard", "durationBase", 200)
                    + spell.getDurationLevel() * ModConfig.comboInt("blizzard", "durationPerDuration", 60);

            BlizzardEntity blizzard = new BlizzardEntity(serverLevel, caster.getUUID(), origin, radius, duration,
                    spellPower, spell.hasNullify());
            serverLevel.addFreshEntity(blizzard);

            level.playSound(null, origin.x, origin.y, origin.z,
                    SoundEvents.WEATHER_RAIN, SoundSource.WEATHER, 2.0f, 1.2f);
        }
    }

    /**
     * LIGHTNING_STORM: spawns a set of drifting {@link StormCloudEntity storm clouds} above the target
     * area that float across the sky and rain down real lightning bolts over time, rather than a single
     * instant scatter. Increased Area widens the spawn spread and adds more clouds; Duration extends how
     * long each cloud lingers (and so how many bolts it drops). Each bolt is a vanilla
     * {@link LightningBolt}, so it ignites and damages on its own.
     */
    public static void executeLightningStorm(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        double spread = Math.max(ModConfig.comboDouble("lightning_storm", "spreadMin", 5.0), spell.getRange());
        int cloudCount = ModConfig.comboInt("lightning_storm", "cloudBase", 2)
                + spell.getModifierCount(SpellModifier.INCREASED_AREA) * ModConfig.comboInt("lightning_storm", "cloudsPerArea", 1);
        int duration = ModConfig.comboInt("lightning_storm", "durationBase", 120)
                + spell.getDurationLevel() * ModConfig.comboInt("lightning_storm", "durationPerDuration", 80);
        int strikeInterval = ModConfig.comboInt("lightning_storm", "strikeIntervalTicks", 30);
        double strikeRadius = ModConfig.comboDouble("lightning_storm", "strikeRadius", 3.0);
        double drift = ModConfig.comboDouble("lightning_storm", "driftSpeed", 0.03);
        double cloudHeight = ModConfig.comboDouble("lightning_storm", "cloudHeight", 6.0);

        var rand = serverLevel.getRandom();
        for (int i = 0; i < cloudCount; i++) {
            double angle = rand.nextDouble() * Math.PI * 2;
            double dist = rand.nextDouble() * spread;
            Vec3 cloudPos = new Vec3(origin.x + Math.cos(angle) * dist, origin.y + cloudHeight,
                    origin.z + Math.sin(angle) * dist);
            double driftAngle = rand.nextDouble() * Math.PI * 2;
            double driftX = Math.cos(driftAngle) * drift;
            double driftZ = Math.sin(driftAngle) * drift;
            StormCloudEntity cloud = new StormCloudEntity(serverLevel, caster, cloudPos,
                    driftX, driftZ, duration, strikeInterval, strikeRadius, spell.hasNullify());
            serverLevel.addFreshEntity(cloud);
        }

        level.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 2.0f, 0.8f);
    }

    /**
     * EARTHQUAKE: a ground shockwave that damages and heaves nearby entities into the air AND ruptures
     * the terrain itself — surface blocks near the epicenter are flung up as falling blocks so the
     * ground visibly bucks and resettles. Damage scales with spell power; Nullify makes it a pure
     * knock-up with no damage. The heave skips block-entities and fluids so it never eats chests/water.
     */
    public static void executeEarthquake(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        float range = spell.getRange() * (float) ModConfig.comboDouble("earthquake", "rangeMultiplier", 1.5);
        boolean nullify = spell.hasNullify();
        float damage = nullify ? 0f
                : spellPower * (float) ModConfig.comboDouble("earthquake", "damageScale", 0.5);
        float launch = (float) ModConfig.comboDouble("earthquake", "launchStrength", 1.4);

        AABB box = new AABB(origin.subtract(range, range, range), origin.add(range, range, range));
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != caster && e.position().distanceTo(origin) <= range);
        for (LivingEntity entity : entities) {
            if (damage > 0f) {
                entity.hurt(caster.damageSources().magic(), damage);
            }
            Vec3 away = entity.position().subtract(origin);
            if (away.lengthSqr() < 1.0e-4) {
                away = new Vec3(0, 0, 0);
            } else {
                away = away.normalize().scale(ModConfig.comboDouble("earthquake", "pushStrength", 0.75));
            }
            entity.setDeltaMovement(entity.getDeltaMovement().add(away.x, launch, away.z));
            entity.hurtMarked = true;
        }

        if (level instanceof ServerLevel serverLevel) {
            heaveTerrain(serverLevel, caster, origin, range);
            SpellParticles.ring(serverLevel, origin, ParticleTypes.CLOUD,
                    ModConfig.comboInt("earthquake", "particleCount", 70), range, 0.2);
        }
        level.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.0f, 0.4f);
    }

    /**
     * Rupture the surface around the epicenter: pick random columns within the heave radius, take the
     * topmost solid block of each, and launch it as a {@link FallingBlockEntity} with an upward/outward
     * kick. Falling blocks resettle when they land, so this reads as the ground bucking rather than
     * permanent griefing. Skips bedrock-hard blocks, block-entities and fluids to stay non-destructive.
     */
    private static void heaveTerrain(ServerLevel level, Player caster, Vec3 origin, float range) {
        double heaveRadius = range * ModConfig.comboDouble("earthquake", "terrainRadiusFraction", 0.85);
        int maxBlocks = ModConfig.comboInt("earthquake", "terrainBlocks", 44);
        double heaveLaunch = ModConfig.comboDouble("earthquake", "terrainLaunch", 0.75);
        if (maxBlocks <= 0 || heaveRadius < 0.5) return;

        var rand = level.getRandom();
        BlockPos center = BlockPos.containing(origin);
        for (int i = 0; i < maxBlocks; i++) {
            double ang = rand.nextDouble() * Math.PI * 2;
            double r = Math.sqrt(rand.nextDouble()) * heaveRadius;
            int bx = center.getX() + (int) Math.round(Math.cos(ang) * r);
            int bz = center.getZ() + (int) Math.round(Math.sin(ang) * r);
            int topY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz) - 1;
            BlockPos pos = new BlockPos(bx, topY, bz);
            if (!level.isLoaded(pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.hasBlockEntity() || !state.getFluidState().isEmpty()) continue;
            if (state.getDestroySpeed(level, pos) < 0) continue; // unbreakable (bedrock etc.)

            FallingBlockEntity fb = FallingBlockEntity.fall(level, pos, state);
            double vx = Math.cos(ang) * 0.08 * (0.5 + rand.nextDouble());
            double vz = Math.sin(ang) * 0.08 * (0.5 + rand.nextDouble());
            double vy = heaveLaunch * (0.6 + rand.nextDouble() * 0.8);
            fb.setDeltaMovement(vx, vy, vz);
            fb.setHurtsEntities(0.0f, 0); // cosmetic rubble — don't anvil-crush whatever it lands on
        }
    }

    /**
     * SANCTUARY: consecrates the area, wrapping every player inside (including the caster) in a
     * sustained regeneration/resistance/absorption ward. Buff strength scales with Increased Power,
     * duration with the Duration modifier. Only players are warded so it never heals hostile mobs.
     */
    public static void executeSanctuary(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        float range = spell.getRange() * (float) ModConfig.comboDouble("sanctuary", "rangeMultiplier", 1.5);
        int duration = ModConfig.comboInt("sanctuary", "durationBase", 200)
                + spell.getDurationLevel() * ModConfig.comboInt("sanctuary", "durationPerDuration", 100);
        int amp = Math.min(spell.getPowerLevel(), ModConfig.comboInt("sanctuary", "amplifierCap", 2));

        AABB box = new AABB(origin.subtract(range, range, range), origin.add(range, range, range));
        List<Player> players = level.getEntitiesOfClass(Player.class, box,
                p -> p.position().distanceTo(origin) <= range);
        for (Player player : players) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, amp));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, amp));
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, amp));
        }

        if (level instanceof ServerLevel serverLevel) {
            SpellParticles.ring(serverLevel, origin, ParticleTypes.HEART,
                    ModConfig.comboInt("sanctuary", "particleCount", 24), range, 0.5);
        }
        level.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.4f);
    }

    /**
     * FIRESTORM: a rain of small fireballs across the area plus lingering ground fire. Increased Area
     * widens the storm and adds projectiles. Lighter and wider than Meteor Strike (which needs Launch
     * and hurls a few huge fireballs); Firestorm blankets the zone in many small ones.
     */
    public static void executeFirestorm(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        double spread = Math.max(ModConfig.comboDouble("firestorm", "spreadMin", 5.0), spell.getRange());
        int count = ModConfig.comboInt("firestorm", "countBase", 8)
                + spell.getModifierCount(SpellModifier.INCREASED_AREA) * ModConfig.comboInt("firestorm", "countPerArea", 4);
        int spawnHeight = ModConfig.comboInt("firestorm", "spawnHeight", 18);
        double speed = ModConfig.comboDouble("firestorm", "fireballSpeed", 0.5);

        for (int i = 0; i < count; i++) {
            double angle = serverLevel.getRandom().nextDouble() * Math.PI * 2;
            double dist = serverLevel.getRandom().nextDouble() * spread;
            Vec3 target = new Vec3(origin.x + Math.cos(angle) * dist, origin.y, origin.z + Math.sin(angle) * dist);
            Vec3 spawn = new Vec3(target.x, origin.y + spawnHeight, target.z);
            Vec3 dir = target.subtract(spawn).normalize().scale(speed);
            SmallFireball fireball = new SmallFireball(serverLevel, caster, dir);
            fireball.setPos(spawn.x, spawn.y, spawn.z);
            serverLevel.addFreshEntity(fireball);
        }

        SpellParticles.impact(serverLevel, origin, ParticleTypes.FLAME, 30, 0.4);
        level.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0f, 0.6f);
    }

    /**
     * GEYSER: an erupting column of water that blasts every entity above it skyward. The eruption is
     * purely a visual jet of water particles — it leaves no water-source blocks behind. Increased Area
     * widens the jet. Pairs Create Water with Launch for a mobility tool / fall-trap.
     */
    public static void executeGeyser(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        float range = spell.getRange();
        double jetRadius = Math.max(0.6, range * ModConfig.comboDouble("geyser", "jetRadiusMultiplier", 0.4));

        float launch = (float) ModConfig.comboDouble("geyser", "launchStrength", 2.2)
                + spell.getPowerLevel() * (float) ModConfig.comboDouble("geyser", "launchPerPower", 0.45);
        // Direct hit on top of the launch — the erupting column bursts up under the target, so it should
        // hurt, not just toss. The launch then adds fall damage on the way down.
        float geyserDamage = spellPower * (float) ModConfig.comboDouble("geyser", "damageScale", 0.3);
        boolean geyserNullify = spell.hasNullify();
        AABB box = new AABB(origin.subtract(range, range, range), origin.add(range, range, range));
        List<Entity> entities = level.getEntitiesOfClass(Entity.class, box,
                e -> e != caster && e.position().distanceTo(origin) <= range);
        for (Entity entity : entities) {
            Vec3 motion = entity.getDeltaMovement();
            entity.setDeltaMovement(motion.x, launch, motion.z);
            entity.hurtMarked = true;
            if (!geyserNullify && geyserDamage > 0 && entity instanceof LivingEntity living) {
                living.hurt(caster.damageSources().magic(), geyserDamage);
            }
        }

        // Erupting water jet: a tall column of upward-shooting splash/water particles plus a crown of
        // droplets at the base, scaled by the jet radius. Server-sent so all nearby players see it.
        if (level instanceof ServerLevel serverLevel) {
            double jetHeight = ModConfig.comboDouble("geyser", "jetHeight", 4.0);
            int columnParticles = ModConfig.comboInt("geyser", "columnParticles", 60);
            int crownParticles = ModConfig.comboInt("geyser", "crownParticles", 40);
            var rand = serverLevel.getRandom();
            // Upward column: particles launched straight up from a small disc at the origin.
            for (int i = 0; i < columnParticles; i++) {
                double r = Math.sqrt(rand.nextDouble()) * jetRadius * 0.5;
                double ang = rand.nextDouble() * Math.PI * 2;
                double ox = Math.cos(ang) * r;
                double oz = Math.sin(ang) * r;
                double upSpeed = 0.4 + rand.nextDouble() * (jetHeight * 0.15);
                serverLevel.sendParticles(ParticleTypes.SPLASH,
                        origin.x + ox, origin.y + 0.1, origin.z + oz,
                        0, 0.0, upSpeed, 0.0, 1.0);
                serverLevel.sendParticles(ParticleTypes.FALLING_WATER,
                        origin.x + ox, origin.y + 0.5 + rand.nextDouble() * jetHeight, origin.z + oz,
                        1, 0.05, 0.0, 0.05, 0.0);
            }
            // Crown of droplets spraying outward at the base.
            for (int i = 0; i < crownParticles; i++) {
                double ang = rand.nextDouble() * Math.PI * 2;
                double vx = Math.cos(ang) * (0.15 + rand.nextDouble() * 0.2);
                double vz = Math.sin(ang) * (0.15 + rand.nextDouble() * 0.2);
                serverLevel.sendParticles(ParticleTypes.SPLASH,
                        origin.x, origin.y + 0.2, origin.z,
                        0, vx, 0.25 + rand.nextDouble() * 0.2, vz, 1.0);
            }
        }
        level.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 1.0f, 0.8f);
    }
}
