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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

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
    protected void readAdditionalSaveData(CompoundTag input) {
        this.lifetime = input.contains("Lifetime") ? input.getInt("Lifetime") : 0;
        this.maxLifetime = input.contains("MaxLifetime") ? input.getInt("MaxLifetime") : 100;
        this.radius = input.contains("Radius") ? input.getFloat("Radius") : 3.0f;

        barrierBlocks.clear();
        if (input.contains("BarrierBlocks")) {
            ListTag list = input.getList("BarrierBlocks", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag child = list.getCompound(i);
                int x = child.getInt("X");
                int y = child.getInt("Y");
                int z = child.getInt("Z");
                barrierBlocks.add(new BlockPos(x, y, z));
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag output) {
        output.putInt("Lifetime", lifetime);
        output.putInt("MaxLifetime", maxLifetime);
        output.putFloat("Radius", radius);

        ListTag list = new ListTag();
        for (BlockPos pos : barrierBlocks) {
            CompoundTag child = new CompoundTag();
            child.putInt("X", pos.getX());
            child.putInt("Y", pos.getY());
            child.putInt("Z", pos.getZ());
            list.add(child);
        }
        output.put("BarrierBlocks", list);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

}
