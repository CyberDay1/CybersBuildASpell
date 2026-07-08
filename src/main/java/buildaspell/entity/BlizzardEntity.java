package buildaspell.entity;

import buildaspell.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Blizzard combo: an invisible area entity that rains blue + white tiny particles from
 * the sky and steadily freezes living entities caught inside it. The visual is entirely
 * server-sent particles, so the entity itself uses a no-op renderer.
 */
public class BlizzardEntity extends Entity {
    private static final Vector3f ICE_BLUE = new Vector3f(0.6f, 0.8f, 1.0f);

    @org.jetbrains.annotations.Nullable
    private java.util.UUID casterUUID;
    private float radius = 5.0f;
    private int lifetime;
    private int maxLifetime = 200;
    private float spellPower = 10.0f;
    private boolean nullify;

    public BlizzardEntity(EntityType<? extends BlizzardEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public BlizzardEntity(Level level, @org.jetbrains.annotations.Nullable java.util.UUID casterUUID,
                          Vec3 position, float radius, int duration, float spellPower, boolean nullify) {
        super(ModEntities.BLIZZARD.get(), level);
        this.casterUUID = casterUUID;
        this.setPos(position);
        this.radius = radius;
        this.maxLifetime = duration;
        this.spellPower = spellPower;
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

        spawnSnow(serverLevel);

        // Build up freeze on any living entity standing in the blizzard. Adding faster than
        // the vanilla -2/tick thaw means they progress to fully-frozen (vanilla then applies
        // the freeze tint + freeze damage automatically).
        if (lifetime % buildaspell.config.ModConfig.comboInt("blizzard", "freezeIntervalTicks", 5) == 0) {
            // Freeze the full visible column height (matches spawnSnow's columnHeight), so
            // airborne/flying targets inside the storm are affected too — not just ground-level.
            double columnTop = 8.0 + radius * 0.4;
            AABB area = new AABB(
                    getX() - radius, getY() - 2, getZ() - radius,
                    getX() + radius, getY() + columnTop, getZ() + radius);
            int capBonus = buildaspell.config.ModConfig.comboInt("blizzard", "freezeCapBonus", 80);
            int perApply = buildaspell.config.ModConfig.comboInt("blizzard", "freezePerApply", 30);
            // The caster is never frozen by their own storm: standing inside your own blizzard is safe.
            for (LivingEntity living : serverLevel.getEntitiesOfClass(LivingEntity.class, area,
                    e -> e.isAlive() && e.canFreeze() && !e.getUUID().equals(casterUUID))) {
                int cap = living.getTicksRequiredToFreeze() + capBonus;
                living.setTicksFrozen(Math.min(living.getTicksFrozen() + perApply, cap));
            }
        }

        // Direct cold damage on an interval, scaled by spell power — the freeze meter above only
        // drives the vanilla tint/slow (and vanilla only chips ~1 HP once fully frozen, skipping every
        // freeze-immune mob), so without this the storm did essentially no damage. Nullify keeps a
        // blizzard as pure spectacle. Uses the same full-height column as the freeze pass.
        if (!nullify && lifetime % buildaspell.config.ModConfig.comboInt("blizzard", "damageIntervalTicks", 20) == 0) {
            double columnTop = 8.0 + radius * 0.4;
            AABB dmgArea = new AABB(
                    getX() - radius, getY() - 2, getZ() - radius,
                    getX() + radius, getY() + columnTop, getZ() + radius);
            float coldDamage = spellPower * (float) buildaspell.config.ModConfig.comboDouble("blizzard", "damageScale", 0.15);
            if (coldDamage > 0) {
                double r2 = radius * radius;
                for (LivingEntity living : serverLevel.getEntitiesOfClass(LivingEntity.class, dmgArea,
                        e -> e.isAlive() && !e.getUUID().equals(casterUUID))) {
                    double dx = living.getX() - getX();
                    double dz = living.getZ() - getZ();
                    if (dx * dx + dz * dz > r2) continue; // keep the footprint circular, not square
                    living.hurt(damageSources().freeze(), coldDamage);
                }
            }
        }

        // Freeze surface water within range while the storm rages. Uses frosted ice (which thaws on
        // its own once the storm passes) so it reads as a temporary deep-freeze rather than griefing.
        int waterCap = buildaspell.config.ModConfig.comboInt("blizzard", "waterFreezeBlocks", 40);
        if (waterCap > 0
                && lifetime % buildaspell.config.ModConfig.comboInt("blizzard", "waterFreezeIntervalTicks", 10) == 0) {
            freezeWater(serverLevel, waterCap);
        }

        if (lifetime % 60 == 0) {
            level().playSound(null, getX(), getY(), getZ(),
                    SoundEvents.WEATHER_RAIN, SoundSource.WEATHER, 0.6f, 1.4f);
        }
    }

    /**
     * Converts surface water source blocks within the storm radius into frosted ice. Frosted ice melts
     * back to water on its own (scheduled tick), so the freeze is temporary and self-cleaning — no
     * permanent terrain change. Capped per pass so a huge storm over an ocean can't lag the server.
     */
    private void freezeWater(ServerLevel level, int cap) {
        int r = Mth.ceil(radius);
        BlockPos center = BlockPos.containing(getX(), getY(), getZ());
        double r2 = radius * radius;
        int frozen = 0;
        for (int dx = -r; dx <= r && frozen < cap; dx++) {
            for (int dz = -r; dz <= r && frozen < cap; dz++) {
                if (dx * dx + dz * dz > r2) continue;
                int x = center.getX() + dx;
                int z = center.getZ() + dz;
                int topY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                BlockPos pos = new BlockPos(x, topY, z);
                if (!level.isLoaded(pos)) continue;
                BlockState state = level.getBlockState(pos);
                if (state.is(Blocks.WATER) && state.getFluidState().isSource()) {
                    level.setBlockAndUpdate(pos, Blocks.FROSTED_ICE.defaultBlockState());
                    level.scheduleTick(pos, Blocks.FROSTED_ICE, Mth.nextInt(level.getRandom(), 60, 120));
                    frozen++;
                }
            }
        }
    }

    private void spawnSnow(ServerLevel level) {
        var rand = level.getRandom();
        DustParticleOptions blue = new DustParticleOptions(ICE_BLUE, 0.7f);

        // The storm fills a whole air column, not just a thin band near the top. Height scales
        // gently with width so a big blizzard feels tall too; counts scale with the volume so a
        // wide storm doesn't read as sparse.
        final double columnHeight = 8.0 + radius * 0.4;
        final int airFlakes = buildaspell.config.ModConfig.scaledParticleCount((int) Math.min(64, Math.max(16, radius * 4)));
        final int glints = buildaspell.config.ModConfig.scaledParticleCount((int) Math.min(40, Math.max(8, radius * 2.5)));

        // Volumetric swirling snow spread through the ENTIRE column (ground up to the top), each
        // flake given a tangential "wind" velocity plus its own gravity → a churning snowstorm.
        for (int i = 0; i < airFlakes; i++) {
            double ang = rand.nextDouble() * Math.PI * 2;
            double r = Math.sqrt(rand.nextDouble()) * radius; // sqrt → even area distribution
            double ox = Math.cos(ang) * r;
            double oz = Math.sin(ang) * r;
            double oy = 0.3 + rand.nextDouble() * columnHeight;
            double swirl = 0.14;
            double vx = -Math.sin(ang) * swirl;
            double vz = Math.cos(ang) * swirl;
            level.sendParticles(ParticleTypes.SNOWFLAKE,
                    getX() + ox, getY() + oy, getZ() + oz,
                    0, vx, -0.04, vz, 1.0);
        }

        // Ice-blue glints shimmering throughout the column for the cold-magic tint.
        for (int i = 0; i < glints; i++) {
            double ang = rand.nextDouble() * Math.PI * 2;
            double r = Math.sqrt(rand.nextDouble()) * radius;
            double ox = Math.cos(ang) * r;
            double oz = Math.sin(ang) * r;
            double oy = 0.3 + rand.nextDouble() * columnHeight;
            double swirl = 0.18;
            double vx = -Math.sin(ang) * swirl;
            double vz = Math.cos(ang) * swirl;
            level.sendParticles(blue,
                    getX() + ox, getY() + oy, getZ() + oz,
                    0, vx, -0.12, vz, 1.0);
        }

        // A few heavier flakes raining from the very top for a sense of precipitation falling in.
        int topFlakes = buildaspell.config.ModConfig.scaledParticleCount(6);
        for (int i = 0; i < topFlakes; i++) {
            double ox = (rand.nextDouble() * 2 - 1) * radius;
            double oz = (rand.nextDouble() * 2 - 1) * radius;
            double oy = columnHeight - rand.nextDouble() * 2;
            level.sendParticles(ParticleTypes.SNOWFLAKE,
                    getX() + ox, getY() + oy, getZ() + oz,
                    1, 0.0, -0.05, 0.0, 0.0);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.radius = tag.contains("Radius") ? tag.getFloat("Radius") : 5.0f;
        this.lifetime = tag.getInt("Lifetime");
        this.maxLifetime = tag.contains("MaxLifetime") ? tag.getInt("MaxLifetime") : 200;
        this.spellPower = tag.contains("SpellPower") ? tag.getFloat("SpellPower") : 10.0f;
        this.nullify = tag.getBoolean("Nullify");
        if (tag.contains("CasterUUID")) {
            try {
                this.casterUUID = java.util.UUID.fromString(tag.getString("CasterUUID"));
            } catch (IllegalArgumentException ignored) {}
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("Radius", radius);
        tag.putInt("Lifetime", lifetime);
        tag.putInt("MaxLifetime", maxLifetime);
        tag.putFloat("SpellPower", spellPower);
        tag.putBoolean("Nullify", nullify);
        if (casterUUID != null) tag.putString("CasterUUID", casterUUID.toString());
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }
}
