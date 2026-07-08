package buildaspell.entity;

import buildaspell.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.List;

public class FortressBarrierEntity extends Entity {
    private final List<BlockPos> barrierBlocks = new ArrayList<>();
    private int lifetime;
    private int maxLifetime;
    private float radius;

    public FortressBarrierEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.maxLifetime = 100;
        this.radius = 3.0f;
    }

    public FortressBarrierEntity(Level level, List<BlockPos> blocks, int duration, float radius) {
        super(ModEntities.FORTRESS_BARRIER.get(), level);
        this.barrierBlocks.addAll(blocks);
        this.maxLifetime = duration;
        this.radius = radius;
        this.noPhysics = true;

        // Set position to center of barrier blocks
        if (!blocks.isEmpty()) {
            double cx = blocks.stream().mapToDouble(BlockPos::getX).average().orElse(0);
            double cy = blocks.stream().mapToDouble(BlockPos::getY).average().orElse(0);
            double cz = blocks.stream().mapToDouble(BlockPos::getZ).average().orElse(0);
            setPos(cx, cy, cz);
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) return;

        lifetime++;

        // Spawn lattice particles every 5 ticks
        if (lifetime % 5 == 0 && level() instanceof ServerLevel serverLevel) {
            spawnLatticeParticles(serverLevel);
        }

        if (lifetime >= maxLifetime) {
            removeBarriers();
            discard();
        }
    }

    private void spawnLatticeParticles(ServerLevel serverLevel) {
        int latLines = 6;
        int lonLines = 8;
        int pointsPerLine = 12;

        for (int i = 0; i < latLines; i++) {
            double phi = Math.PI * i / latLines;
            for (int j = 0; j < pointsPerLine; j++) {
                double theta = 2 * Math.PI * j / pointsPerLine;
                double x = getX() + radius * Math.sin(phi) * Math.cos(theta);
                double y = getY() + radius * Math.cos(phi);
                double z = getZ() + radius * Math.sin(phi) * Math.sin(theta);
                serverLevel.sendParticles(ParticleTypes.END_ROD, x, y, z, 1, 0, 0, 0, 0);
            }
        }
    }

    private void removeBarriers() {
        for (BlockPos pos : barrierBlocks) {
            if (level().getBlockState(pos).is(Blocks.BARRIER)) {
                level().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            }
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide() && reason != RemovalReason.UNLOADED_TO_CHUNK && reason != RemovalReason.UNLOADED_WITH_PLAYER) {
            removeBarriers();
        }
        super.remove(reason);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.lifetime = input.getIntOr("Lifetime", 0);
        this.maxLifetime = input.getIntOr("MaxLifetime", 100);
        this.radius = input.getFloatOr("Radius", 3.0f);

        barrierBlocks.clear();
        input.childrenList("BarrierBlocks").ifPresent(list -> {
            for (ValueInput child : list) {
                int x = child.getIntOr("X", 0);
                int y = child.getIntOr("Y", 0);
                int z = child.getIntOr("Z", 0);
                barrierBlocks.add(new BlockPos(x, y, z));
            }
        });
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("Lifetime", lifetime);
        output.putInt("MaxLifetime", maxLifetime);
        output.putFloat("Radius", radius);

        ValueOutput.ValueOutputList list = output.childrenList("BarrierBlocks");
        for (BlockPos pos : barrierBlocks) {
            ValueOutput child = list.addChild();
            child.putInt("X", pos.getX());
            child.putInt("Y", pos.getY());
            child.putInt("Z", pos.getZ());
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

}
