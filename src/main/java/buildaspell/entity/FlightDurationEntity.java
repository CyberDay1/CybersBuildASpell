package buildaspell.entity;

import buildaspell.registry.ModEntities;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class FlightDurationEntity extends Entity {
    @Nullable
    private UUID playerId;
    private int duration;
    private int tickCount;

    public FlightDurationEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public FlightDurationEntity(Level level, Player player, int duration) {
        super(ModEntities.FLIGHT_DURATION.get(), level);
        this.playerId = player.getUUID();
        this.duration = duration;
        this.setPos(player.position());
        this.noPhysics = true;

        // Grant flight
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.getAbilities().mayfly = true;
            serverPlayer.getAbilities().flying = true;
            serverPlayer.onUpdateAbilities();
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // No synced data needed — this entity is invisible and server-driven only
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) return;

        tickCount++;

        // Follow player
        if (tickCount % 20 == 0 && playerId != null && level() instanceof ServerLevel serverLevel) {
            Player player = serverLevel.getPlayerByUUID(playerId);
            if (player != null) {
                setPos(player.position());
            }
        }

        if (tickCount >= duration) {
            removeFlight();
            discard();
        }
    }

    private void removeFlight() {
        if (playerId == null || !(level() instanceof ServerLevel serverLevel)) return;
        Player player = serverLevel.getPlayerByUUID(playerId);
        if (player instanceof ServerPlayer serverPlayer && !serverPlayer.isCreative()) {
            serverPlayer.getAbilities().mayfly = false;
            serverPlayer.getAbilities().flying = false;
            serverPlayer.onUpdateAbilities();

            // Give slow falling to prevent fall damage
            serverPlayer.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0));
            level().playSound(null, serverPlayer.blockPosition(), SoundEvents.BEACON_DEACTIVATE,
                    SoundSource.PLAYERS, 1.0f, 1.0f);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide() && reason != RemovalReason.DISCARDED) {
            removeFlight();
        }
        super.remove(reason);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        input.getString("Player").ifPresent(s -> {
            try { this.playerId = UUID.fromString(s); } catch (IllegalArgumentException ignored) {}
        });
        this.duration = input.getIntOr("Duration", 200);
        this.tickCount = input.getIntOr("TickCount", 0);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        if (playerId != null) output.putString("Player", playerId.toString());
        output.putInt("Duration", duration);
        output.putInt("TickCount", tickCount);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }
}
