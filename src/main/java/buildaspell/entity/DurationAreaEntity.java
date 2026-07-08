package buildaspell.entity;

import buildaspell.registry.ModEntities;
import buildaspell.spell.Spell;
import buildaspell.spell.SpellModifier;
import buildaspell.spell.execution.SpellExecutor;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class DurationAreaEntity extends Entity {
    private static final EntityDataAccessor<Float> DATA_RANGE =
            SynchedEntityData.defineId(DurationAreaEntity.class, EntityDataSerializers.FLOAT);

    @Nullable
    private Spell spell;
    @Nullable
    private UUID casterUUID;
    private float spellPower;
    private int lifetime;
    private int maxLifetime;

    public DurationAreaEntity(EntityType<? extends DurationAreaEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public DurationAreaEntity(Level level, Player caster, Spell spell, float spellPower, Vec3 position) {
        super(ModEntities.DURATION_AREA.get(), level);
        this.casterUUID = caster.getUUID();
        this.spell = spell;
        this.spellPower = spellPower;
        this.setPos(position);
        this.noPhysics = true;

        // Calculate max lifetime based on duration modifier
        int durationLevel = spell != null ? spell.getModifierCounts().getOrDefault(SpellModifier.DURATION, 0) : 0;
        this.maxLifetime = buildaspell.config.ModConfig.modifierInt(
                buildaspell.spell.SpellModifier.LINGER, "durationBase", 100)
                + durationLevel * buildaspell.config.ModConfig.modifierInt(
                buildaspell.spell.SpellModifier.LINGER, "durationPerDuration", 60);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_RANGE, 3.0f);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) return;

        lifetime++;
        if (lifetime >= maxLifetime) {
            discard();
            return;
        }

        // Execute effects on a pulse interval
        if (lifetime % buildaspell.config.ModConfig.modifierInt(
                buildaspell.spell.SpellModifier.LINGER, "pulseIntervalTicks", 5) == 0
                && spell != null && casterUUID != null) {
            Player caster = level().getPlayerByUUID(casterUUID);
            if (caster != null) {
                // Run the effects directly — NOT executeSpellAtLocation — otherwise a Duration
                // spell would re-enter the duration branch and spawn another DurationAreaEntity
                // every pulse, exploding exponentially.
                SpellExecutor.executeEffectsOnce(caster, spell, position(), spellPower);
            }
        }

        // Render as a vanilla lingering-potion cloud: the ENTITY_EFFECT particle (same one a real
        // AreaEffectCloud emits) tinted to the spell's visual color, scattered across the current
        // radius each tick. We drive the effects ourselves via executeEffectsOnce above; only the
        // LOOK is borrowed from vanilla lingering potions.
        if (level() instanceof ServerLevel serverLevel) {
            emitLingerCloud(serverLevel);
        }

        // Gravity
        setDeltaMovement(getDeltaMovement().add(0, -0.04, 0));
        move(net.minecraft.world.entity.MoverType.SELF, getDeltaMovement());
    }

    /**
     * Emits vanilla lingering-potion cloud particles (ENTITY_EFFECT tinted to the spell's visual
     * color) scattered across the current radius, matching how a real {@code AreaEffectCloud}
     * paints itself: a handful of colored motes per tick at random points inside the disc, drifting
     * gently. Mirrors vanilla's per-tick spawn count scaling with radius.
     */
    private void emitLingerCloud(ServerLevel serverLevel) {
        float radius = getRange();
        int argb = lingerColorArgb();

        // Vanilla scales particle output with area; keep it light so many stacked clouds don't spam.
        int count = Math.max(1, (int) (radius * radius * Math.PI * 0.06f));
        net.minecraft.util.RandomSource rng = serverLevel.getRandom();
        for (int p = 0; p < count; p++) {
            // Uniform point within the disc (sqrt for even area distribution), like AreaEffectCloud.
            float angle = rng.nextFloat() * (float) (Math.PI * 2.0);
            float dist = (float) Math.sqrt(rng.nextFloat()) * radius;
            double px = getX() + Math.cos(angle) * dist;
            double pz = getZ() + Math.sin(angle) * dist;
            double py = getY() + 0.1 + rng.nextFloat() * 0.3;
            // sendParticles with count 0 uses the x/y/z offsets as an explicit velocity, so the mote
            // sits at (px,py,pz) and drifts almost imperceptibly — the settled lingering-cloud look.
            serverLevel.sendParticles(
                    net.minecraft.core.particles.ColorParticleOption.create(
                            ParticleTypes.ENTITY_EFFECT, argb),
                    px, py, pz, 0, 0.0, 0.02, 0.0, 0.0);
        }
    }

    /**
     * The linger cloud tint as packed ARGB. Uses the spell's chosen visual color when set; otherwise
     * falls back to the standard vanilla potion-cloud blue-grey so an un-themed spell still reads as
     * a lingering cloud.
     */
    private int lingerColorArgb() {
        int rgb = spell != null ? spell.getVisual().color() : buildaspell.spell.SpellVisual.COLOR_DEFAULT;
        if (rgb == buildaspell.spell.SpellVisual.COLOR_DEFAULT) {
            rgb = 0x385DC6; // vanilla water-potion cloud tone
        }
        return 0xFF000000 | (rgb & 0x00FFFFFF);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        input.read("Spell", Spell.CODEC).ifPresent(s -> this.spell = s);
        this.spellPower = input.getFloatOr("SpellPower", 10.0f);
        this.lifetime = input.getIntOr("Lifetime", 0);
        this.maxLifetime = input.getIntOr("MaxLifetime", 100);
        input.getString("CasterUUID").ifPresent(s -> {
            try { this.casterUUID = java.util.UUID.fromString(s); } catch (IllegalArgumentException ignored) {}
        });
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        if (spell != null) output.store("Spell", Spell.CODEC, spell);
        if (casterUUID != null) output.putString("CasterUUID", casterUUID.toString());
        output.putFloat("SpellPower", spellPower);
        output.putInt("Lifetime", lifetime);
        output.putInt("MaxLifetime", maxLifetime);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Nullable
    public Spell getSpell() { return spell; }
    @Nullable
    public UUID getCasterUUID() { return casterUUID; }
    public float getSpellPower() { return spellPower; }
    public float getRange() { return entityData.get(DATA_RANGE); }
    public int getLifetime() { return lifetime; }
    public int getMaxLifetime() { return maxLifetime; }
}
