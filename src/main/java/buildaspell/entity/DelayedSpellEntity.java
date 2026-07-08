package buildaspell.entity;

import buildaspell.registry.ModEntities;
import buildaspell.spell.Spell;
import buildaspell.spell.execution.SpellExecutor;
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

public class DelayedSpellEntity extends Entity {
    @Nullable
    private Spell spell;
    @Nullable
    private UUID casterId;
    private float spellPower;
    private int delay;
    private int tickCount;
    /**
     * When true, execution runs the spell's effect groups directly (no combo detection, no
     * DURATION wrap) via {@link SpellExecutor#executeEffectsOnce}. Used for ECHO repeats, which
     * must replay only the already-resolved effects — routing them back through the full cast
     * path would re-detect combos and re-wrap durations.
     */
    private boolean effectsOnly;

    public DelayedSpellEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public DelayedSpellEntity(Level level, Player caster, Spell spell, float spellPower, int delay, Vec3 position) {
        this(level, caster, spell, spellPower, delay, position, false);
    }

    public DelayedSpellEntity(Level level, Player caster, Spell spell, float spellPower, int delay, Vec3 position, boolean effectsOnly) {
        super(ModEntities.DELAYED_SPELL.get(), level);
        this.casterId = caster.getUUID();
        this.spell = spell;
        this.spellPower = spellPower;
        this.delay = delay;
        this.effectsOnly = effectsOnly;
        this.setPos(position);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) return;

        tickCount++;
        if (tickCount >= delay) {
            execute();
            discard();
        }
    }

    private void execute() {
        if (spell != null && casterId != null) {
            Player caster = level().getPlayerByUUID(casterId);
            if (caster != null) {
                if (effectsOnly) {
                    // ECHO repeat: replay just the effects at the stored (falloff-scaled) power.
                    SpellExecutor.executeEffectsOnce(caster, spell, position(), spellPower);
                } else {
                    SpellExecutor.executeSpellAtLocation(caster, spell, position(), spellPower);
                }
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        input.read("Spell", Spell.CODEC).ifPresent(s -> this.spell = s);
        input.getString("Caster").ifPresent(s -> {
            try { this.casterId = UUID.fromString(s); } catch (IllegalArgumentException ignored) {}
        });
        this.spellPower = input.getFloatOr("SpellPower", 10.0f);
        this.delay = input.getIntOr("Delay", 20);
        this.tickCount = input.getIntOr("TickCount", 0);
        this.effectsOnly = input.getBooleanOr("EffectsOnly", false);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        if (spell != null) output.store("Spell", Spell.CODEC, spell);
        if (casterId != null) output.putString("Caster", casterId.toString());
        output.putFloat("SpellPower", spellPower);
        output.putInt("Delay", delay);
        output.putInt("TickCount", tickCount);
        output.putBoolean("EffectsOnly", effectsOnly);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Nullable
    public Spell getSpell() { return spell; }
    @Nullable
    public UUID getCasterId() { return casterId; }
    public float getSpellPower() { return spellPower; }
}
