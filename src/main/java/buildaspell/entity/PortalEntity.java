package buildaspell.entity;

import buildaspell.compat.NeoPortalsCompat;
import buildaspell.portal.PortalChunkLoader;
import buildaspell.portal.PortalManager;
import buildaspell.registry.ModEntities;
import buildaspell.config.ModConfig;
import buildaspell.network.OpenPortalDialScreenPacket;
import buildaspell.network.OpenPortalNamingScreenPacket;
import buildaspell.portal.PortalInfo;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class PortalEntity extends Entity {
    private static final EntityDataAccessor<Float> DATA_WIDTH =
            SynchedEntityData.defineId(PortalEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_HEIGHT =
            SynchedEntityData.defineId(PortalEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_IS_DIALED =
            SynchedEntityData.defineId(PortalEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> DATA_DEST_DIMENSION =
            SynchedEntityData.defineId(PortalEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_HAS_NEO_PORTALS =
            SynchedEntityData.defineId(PortalEntity.class, EntityDataSerializers.BOOLEAN);

    @Nullable
    private UUID dialedDestinationUUID;
    @Nullable
    private UUID casterUUID;
    @Nullable
    private ResourceKey<Level> destinationDimension;
    @Nullable
    private Vec3 destinationPos;
    private String portalName = "";
    private int ticksExisted;
    private boolean registered;
    /**
     * True when this portal's dialed state was established by another portal dialing TO it.
     * The reflected side does not own the NeoPortals pair and must not rebuild/remove it.
     */
    private boolean reciprocal;

    // NeoPortals integration: UUIDs of the Portal entities created by NeoPortals for this link
    @Nullable
    private UUID neoPortalSourceUUID;
    @Nullable
    private UUID neoPortalDestUUID;
    /** Tracks whether we have already tried to rebuild NeoPortals portals this session. */
    private boolean neoPortalsRebuildAttempted;

    public PortalEntity(EntityType<? extends PortalEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public PortalEntity(Level level, Vec3 position, UUID casterUUID) {
        super(ModEntities.PORTAL.get(), level);
        this.casterUUID = casterUUID;
        this.setPos(position);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_WIDTH, 1.5f);
        builder.define(DATA_HEIGHT, 3.5f);
        builder.define(DATA_IS_DIALED, false);
        builder.define(DATA_DEST_DIMENSION, "");
        builder.define(DATA_HAS_NEO_PORTALS, false);
    }

    @Override
    public void tick() {
        super.tick();
        ticksExisted++;

        // Register portal on first tick
        if (!level().isClientSide() && !registered) {
            PortalManager.registerPortal(getUUID(), portalName, level().dimension(), position(), casterUUID);
            PortalChunkLoader.updateTickets(this);
            registered = true;
        }

        // After server restart, recreate NeoPortals portal entities if needed (owner side only —
        // the reflected/reciprocal side does not own the pair). We defer a few ticks so the
        // destination dimension has time to load. Also validates that stored UUIDs still reference
        // living entities (handles crash recovery).
        if (!level().isClientSide() && isDialed() && !reciprocal && NeoPortalsCompat.isLoaded()
                && !neoPortalsRebuildAttempted && ticksExisted > 10) {
            neoPortalsRebuildAttempted = true;
            if (neoPortalSourceUUID != null && level() instanceof ServerLevel serverLevel) {
                // Validate the stored UUID still points to an actual NeoPortals entity
                if (NeoPortalsCompat.getPortal(serverLevel, neoPortalSourceUUID) == null) {
                    neoPortalSourceUUID = null;
                    neoPortalDestUUID = null;
                    this.entityData.set(DATA_HAS_NEO_PORTALS, false);
                }
            }
            if (neoPortalSourceUUID == null) {
                tryRebuildNeoPortals();
            }
        }

        if (level().isClientSide()) {
            // Particles
            if (isDialed()) {
                level().addParticle(ParticleTypes.WARPED_SPORE,
                        getX() + random.nextGaussian() * 0.5,
                        getY() + random.nextFloat() * getPortalHeight(),
                        getZ() + random.nextGaussian() * 0.5,
                        0, 0.05, 0);
            } else {
                level().addParticle(ParticleTypes.PORTAL,
                        getX() + random.nextGaussian() * 0.5,
                        getY() + random.nextFloat() * getPortalHeight(),
                        getZ() + random.nextGaussian() * 0.5,
                        0, 0.1, 0);
            }
            return;
        }

        // Sound
        if (ticksExisted % 40 == 0) {
            level().playSound(null, blockPosition(), SoundEvents.PORTAL_AMBIENT, SoundSource.BLOCKS, 0.3f, 1.0f);
        }

        // Discover for nearby players (throttled to every 20 ticks)
        if (ticksExisted % 20 == 0) {
            AABB discoverBox = getBoundingBox().inflate(10);
            for (Entity entity : level().getEntities(this, discoverBox)) {
                if (entity instanceof ServerPlayer player) {
                    PortalManager.discoverPortal(player.getUUID(), getUUID());
                }
            }
        }

        // Native walk-through teleport. NeoPortals, when present, owns both the see-through render
        // AND the teleport, so we only do this fallback when NeoPortals is NOT handling this portal.
        if (level() instanceof ServerLevel serverLevel) {
            tryNativeTeleport(serverLevel);
        }
    }

    /**
     * Teleports entities that step into a dialed portal to its destination. Only runs when NeoPortals
     * isn't managing the link (otherwise NeoPortals handles it). Both link sides carry destinationPos,
     * so this works from either end. Portal cooldown prevents the destination portal bouncing the
     * entity straight back.
     */
    private void tryNativeTeleport(ServerLevel serverLevel) {
        if (!isDialed() || hasNeoPortals()) return;
        if (destinationPos == null || destinationDimension == null) return;

        AABB box = getBoundingBox().inflate(0.2);
        List<Entity> entities = serverLevel.getEntities(this, box);
        if (entities.isEmpty()) return;

        ServerLevel destLevel = serverLevel.getServer().getLevel(destinationDimension);
        if (destLevel == null) return;

        for (Entity entity : entities) {
            if (entity instanceof PortalEntity) continue;
            if (entity.isOnPortalCooldown()) continue;

            // Carry the entity's horizontal momentum a short way past the destination portal so it
            // lands clear of the partner's hitbox rather than inside it (which would ping-pong).
            Vec3 motion = entity.getDeltaMovement();
            Vec3 horiz = new Vec3(motion.x, 0, motion.z);
            Vec3 offset = horiz.lengthSqr() > 1.0e-3 ? horiz.normalize().scale(1.5) : Vec3.ZERO;
            double dx = destinationPos.x + offset.x;
            double dy = destinationPos.y + 0.2;
            double dz = destinationPos.z + offset.z;

            entity.setPortalCooldown(100);
            if (destLevel == serverLevel) {
                entity.teleportTo(dx, dy, dz);
            } else {
                entity.teleportTo(destLevel, dx, dy, dz, java.util.Set.of(), entity.getYRot(), entity.getXRot());
            }
            entity.setPortalCooldown(100);

            serverLevel.playSound(null, blockPosition(), SoundEvents.PORTAL_TRAVEL, SoundSource.BLOCKS, 0.25f, 1.0f);
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
        if (player.distanceToSqr(this) > 16 * 16) return InteractionResult.PASS;

        // Only caster or OP players can interact
        if (casterUUID != null && !casterUUID.equals(player.getUUID())
                && !serverPlayer.hasPermissions(net.minecraft.commands.Commands.LEVEL_GAMEMASTERS)) {
            return InteractionResult.PASS;
        }

        if (portalName.isEmpty()) {
            PacketDistributor.sendToPlayer(serverPlayer,
                    new OpenPortalNamingScreenPacket(getUUID(), portalName));
        } else {
            List<PortalInfo> discovered = PortalManager.getDiscoveredPortals(player.getUUID());
            PacketDistributor.sendToPlayer(serverPlayer,
                    new OpenPortalDialScreenPacket(getUUID(), discovered,
                            getPortalWidth(), getPortalHeight(),
                            (float) ModConfig.getPortalMinSize(), (float) ModConfig.getPortalMaxSize()));
        }

        return InteractionResult.SUCCESS;
    }

    // The portal must be ray-pickable for right-click interaction to reach interact();
    // vanilla Entity.isPickable() returns false by default, which would swallow all clicks.
    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    // Hitbox tracks the portal's actual dialed width/height so the clickable area matches the
    // rendered portal at any size (the registered EntityType size is only a fallback default).
    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.fixed(getPortalWidth(), getPortalHeight());
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_WIDTH.equals(key) || DATA_HEIGHT.equals(key)) {
            refreshDimensions();
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level() instanceof ServerLevel serverLevel) {
            // Sever any reciprocal link before removing.
            if (isDialed() && dialedDestinationUUID != null && destinationDimension != null) {
                ServerLevel partnerLevel = serverLevel.getServer().getLevel(destinationDimension);
                if (partnerLevel != null && partnerLevel.getEntity(dialedDestinationUUID) instanceof PortalEntity partner) {
                    if (reciprocal) {
                        partner.undial(); // partner is the owner; this tears down the whole link
                    } else {
                        partner.clearReciprocal();
                    }
                }
            }
            PortalManager.unregisterPortal(getUUID());
            removeNeoPortals(serverLevel);
            PortalChunkLoader.releaseTickets(this);
            level().playSound(null, blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.0f, 1.0f);
            discard();
        }
        return true;
    }

    public void dialTo(UUID destinationUUID, ResourceKey<Level> dimension, Vec3 pos) {
        this.dialedDestinationUUID = destinationUUID;
        this.destinationDimension = dimension;
        this.destinationPos = pos;
        this.reciprocal = false;
        this.entityData.set(DATA_IS_DIALED, true);
        this.entityData.set(DATA_DEST_DIMENSION, dimension.location().toString());
        this.neoPortalsRebuildAttempted = false;

        if (level().isClientSide() || !(level() instanceof ServerLevel serverLevel)) return;

        ServerLevel destLevel = serverLevel.getServer().getLevel(dimension);
        UUID neoDestForReciprocal = null;

        // Create a NeoPortals portal pair so players see through the portal
        if (destLevel != null && NeoPortalsCompat.isLoaded()) {
            // Try to use the destination PortalEntity's geometry for the dest side
            Vec3 destCenter;
            Vec3 destAxisW;
            Vec3 destAxisH;
            float destWidth;
            float destHeight;
            net.minecraft.world.entity.Entity destEntity = destLevel.getEntity(destinationUUID);
            if (destEntity instanceof PortalEntity destPortal) {
                destCenter = destPortal.getPortalCenter();
                destAxisW = destPortal.getAxisW();
                destAxisH = destPortal.getAxisH();
                destWidth = destPortal.getPortalWidth();
                destHeight = destPortal.getPortalHeight();
            } else {
                // Destination portal not loaded yet — fall back to same orientation/size
                destCenter = pos.add(0, getPortalHeight() * 0.5, 0);
                destAxisW = getAxisW();
                destAxisH = getAxisH();
                destWidth = getPortalWidth();
                destHeight = getPortalHeight();
            }

            UUID[] uuids = NeoPortalsCompat.createPortalPair(
                    serverLevel, getPortalCenter(), getAxisW(), getAxisH(), getPortalWidth(), getPortalHeight(),
                    destLevel, destCenter, destAxisW, destAxisH, destWidth, destHeight);
            if (uuids != null) {
                this.neoPortalSourceUUID = uuids[0];
                this.neoPortalDestUUID = uuids[1];
                this.entityData.set(DATA_HAS_NEO_PORTALS, true);
                neoDestForReciprocal = uuids[1];
            }
        }

        // Reflect the link on the destination portal so it renders dialed and points back here.
        // The reflected side does NOT own the NeoPortals pair (this portal does).
        if (destLevel != null && destLevel.getEntity(destinationUUID) instanceof PortalEntity destPortal) {
            destPortal.linkReciprocal(getUUID(), level().dimension(), position(), neoDestForReciprocal);
        }

        PortalChunkLoader.updateTickets(this);
    }

    /**
     * Marks this portal as the reflected/destination side of a dial link owned by another portal.
     * It renders dialed and points back at the owner, but never creates, rebuilds, or removes the
     * NeoPortals pair — the owner is solely responsible for that lifecycle.
     */
    public void linkReciprocal(UUID ownerPortalUUID, ResourceKey<Level> ownerDimension, Vec3 ownerPos,
                               @Nullable UUID neoPortalThisSideUUID) {
        this.dialedDestinationUUID = ownerPortalUUID;
        this.destinationDimension = ownerDimension;
        this.destinationPos = ownerPos;
        this.reciprocal = true;
        this.neoPortalsRebuildAttempted = true; // reflected side never rebuilds
        this.entityData.set(DATA_IS_DIALED, true);
        this.entityData.set(DATA_DEST_DIMENSION, ownerDimension.location().toString());
        // The owner created the NeoPortals entity on this side; track it for rendering only.
        this.neoPortalSourceUUID = neoPortalThisSideUUID;
        this.entityData.set(DATA_HAS_NEO_PORTALS, neoPortalThisSideUUID != null);
        if (level() instanceof ServerLevel) {
            PortalChunkLoader.updateTickets(this);
        }
    }

    /** Clears the reflected link state without touching NeoPortals (owner handles that). */
    public void clearReciprocal() {
        this.dialedDestinationUUID = null;
        this.destinationDimension = null;
        this.destinationPos = null;
        this.reciprocal = false;
        this.neoPortalSourceUUID = null;
        this.neoPortalDestUUID = null;
        this.neoPortalsRebuildAttempted = false;
        this.entityData.set(DATA_IS_DIALED, false);
        this.entityData.set(DATA_DEST_DIMENSION, "");
        this.entityData.set(DATA_HAS_NEO_PORTALS, false);
        if (level() instanceof ServerLevel) {
            PortalChunkLoader.updateTickets(this);
        }
    }

    public void undial() {
        if (!level().isClientSide() && level() instanceof ServerLevel serverLevel) {
            // If we're the reflected side, delegate to the owner which holds the NeoPortals pair.
            if (reciprocal && dialedDestinationUUID != null && destinationDimension != null) {
                ServerLevel ownerLevel = serverLevel.getServer().getLevel(destinationDimension);
                if (ownerLevel != null && ownerLevel.getEntity(dialedDestinationUUID) instanceof PortalEntity owner) {
                    owner.undial();
                    return;
                }
            }
            // Owner side: tear down NeoPortals and clear the reflected destination side.
            if (NeoPortalsCompat.isLoaded()) {
                removeNeoPortals(serverLevel);
            }
            if (dialedDestinationUUID != null && destinationDimension != null) {
                ServerLevel destLevel = serverLevel.getServer().getLevel(destinationDimension);
                if (destLevel != null && destLevel.getEntity(dialedDestinationUUID) instanceof PortalEntity destPortal) {
                    destPortal.clearReciprocal();
                }
            }
        }
        this.dialedDestinationUUID = null;
        this.destinationDimension = null;
        this.destinationPos = null;
        this.reciprocal = false;
        this.entityData.set(DATA_IS_DIALED, false);
        this.entityData.set(DATA_DEST_DIMENSION, "");
        this.entityData.set(DATA_HAS_NEO_PORTALS, false);
        this.neoPortalsRebuildAttempted = false;
        if (level() instanceof ServerLevel) {
            PortalChunkLoader.updateTickets(this);
        }
    }

    /**
     * Removes both NeoPortals portal entities associated with this dial link.
     * Safe to call if they are already gone.
     */
    private void removeNeoPortals(ServerLevel serverLevel) {
        if (!NeoPortalsCompat.isLoaded()) return;
        if (neoPortalSourceUUID != null) {
            NeoPortalsCompat.removePortal(serverLevel, neoPortalSourceUUID);
            neoPortalSourceUUID = null;
        }
        if (neoPortalDestUUID != null && destinationDimension != null) {
            ServerLevel destLevel = NeoPortalsCompat.getLevel(serverLevel, destinationDimension);
            if (destLevel != null) {
                NeoPortalsCompat.removePortal(destLevel, neoPortalDestUUID);
            }
            neoPortalDestUUID = null;
        }
        this.entityData.set(DATA_HAS_NEO_PORTALS, false);
    }

    /**
     * Called on the first tick after server restart when the portal is still dialed
     * but the NeoPortals visual portals are missing. Attempts to recreate them.
     */
    private void tryRebuildNeoPortals() {
        if (!(level() instanceof ServerLevel serverLevel)) return;
        if (destinationDimension == null || destinationPos == null || dialedDestinationUUID == null) return;

        ServerLevel destLevel = NeoPortalsCompat.getLevel(serverLevel, destinationDimension);
        if (destLevel == null) return;

        Vec3 destCenter;
        Vec3 destAxisW;
        Vec3 destAxisH;
        float destWidth;
        float destHeight;
        net.minecraft.world.entity.Entity destEntity = destLevel.getEntity(dialedDestinationUUID);
        if (destEntity instanceof PortalEntity destPortal) {
            destCenter = destPortal.getPortalCenter();
            destAxisW = destPortal.getAxisW();
            destAxisH = destPortal.getAxisH();
            destWidth = destPortal.getPortalWidth();
            destHeight = destPortal.getPortalHeight();
        } else {
            destCenter = destinationPos.add(0, getPortalHeight() * 0.5, 0);
            destAxisW = getAxisW();
            destAxisH = getAxisH();
            destWidth = getPortalWidth();
            destHeight = getPortalHeight();
        }

        UUID[] uuids = NeoPortalsCompat.createPortalPair(
                serverLevel, getPortalCenter(), getAxisW(), getAxisH(), getPortalWidth(), getPortalHeight(),
                destLevel, destCenter, destAxisW, destAxisH, destWidth, destHeight);
        if (uuids != null) {
            this.neoPortalSourceUUID = uuids[0];
            this.neoPortalDestUUID = uuids[1];
            this.entityData.set(DATA_HAS_NEO_PORTALS, true);
            // Re-establish the reflected link so the destination side renders the rebuilt portal.
            if (destEntity instanceof PortalEntity destPortal) {
                destPortal.linkReciprocal(getUUID(), level().dimension(), position(), uuids[1]);
            }
        }
    }

    // --- Portal geometry methods ---

    /**
     * Returns the horizontal axis of the portal plane (right vector).
     * Derived from entity yaw: perpendicular to the facing direction in the horizontal plane.
     */
    public Vec3 getAxisW() {
        float yawRad = getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(Mth.cos(yawRad), 0, Mth.sin(yawRad));
    }

    /**
     * Returns the vertical axis of the portal plane (up vector). Always straight up.
     */
    public Vec3 getAxisH() {
        return new Vec3(0, 1, 0);
    }

    /**
     * Returns the normal (facing direction) of the portal plane.
     * Points from the portal surface toward the viewer side.
     */
    public Vec3 getNormal() {
        float yawRad = getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yawRad), 0, Mth.cos(yawRad));
    }

    /**
     * Returns the center of the portal (bottom-center + half height).
     */
    public Vec3 getPortalCenter() {
        return position().add(0, getPortalHeight() * 0.5, 0);
    }

    /**
     * Transforms a point from the source portal's reference frame to the destination position.
     * Simple origin-to-origin mapping (no rotation transform for Phase 1).
     */
    public Vec3 transformPoint(Vec3 point) {
        if (destinationPos == null) return point;
        Vec3 offset = point.subtract(position());
        return destinationPos.add(offset);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag input) {
        if (input.contains("DialedDestination")) {
            String s = input.getString("DialedDestination");
            try { this.dialedDestinationUUID = UUID.fromString(s); } catch (IllegalArgumentException ignored) {}
        }
        if (input.contains("Caster")) {
            String s = input.getString("Caster");
            try { this.casterUUID = UUID.fromString(s); } catch (IllegalArgumentException ignored) {}
        }
        this.portalName = input.contains("PortalName") ? input.getString("PortalName") : "";
        this.ticksExisted = input.contains("TicksExisted") ? input.getInt("TicksExisted") : 0;
        this.reciprocal = input.contains("Reciprocal") && input.getBoolean("Reciprocal");
        this.entityData.set(DATA_WIDTH, input.contains("Width") ? input.getFloat("Width") : 1.5f);
        this.entityData.set(DATA_HEIGHT, input.contains("Height") ? input.getFloat("Height") : 3.5f);
        if (input.contains("DestDimension")) {
            String s = input.getString("DestDimension");
            this.destinationDimension = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(s));
            this.entityData.set(DATA_DEST_DIMENSION, s);
        }
        if (input.contains("DestPos")) {
            CompoundTag child = input.getCompound("DestPos");
            double x = child.contains("X") ? child.getDouble("X") : 0;
            double y = child.contains("Y") ? child.getDouble("Y") : 0;
            double z = child.contains("Z") ? child.getDouble("Z") : 0;
            this.destinationPos = new Vec3(x, y, z);
        }
        if (input.contains("NeoPortalSource")) {
            String s = input.getString("NeoPortalSource");
            try { this.neoPortalSourceUUID = UUID.fromString(s); } catch (IllegalArgumentException ignored) {}
        }
        if (input.contains("NeoPortalDest")) {
            String s = input.getString("NeoPortalDest");
            try { this.neoPortalDestUUID = UUID.fromString(s); } catch (IllegalArgumentException ignored) {}
        }
        // Restore synched dialed state
        this.entityData.set(DATA_IS_DIALED, this.dialedDestinationUUID != null);
        this.entityData.set(DATA_HAS_NEO_PORTALS, this.neoPortalSourceUUID != null);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag output) {
        if (dialedDestinationUUID != null) output.putString("DialedDestination", dialedDestinationUUID.toString());
        if (casterUUID != null) output.putString("Caster", casterUUID.toString());
        output.putString("PortalName", portalName);
        output.putInt("TicksExisted", ticksExisted);
        output.putBoolean("Reciprocal", reciprocal);
        output.putFloat("Width", entityData.get(DATA_WIDTH));
        output.putFloat("Height", entityData.get(DATA_HEIGHT));
        if (destinationDimension != null) {
            output.putString("DestDimension", destinationDimension.location().toString());
        }
        if (destinationPos != null) {
            CompoundTag posOut = new CompoundTag();
            posOut.putDouble("X", destinationPos.x);
            posOut.putDouble("Y", destinationPos.y);
            posOut.putDouble("Z", destinationPos.z);
            output.put("DestPos", posOut);
        }
        if (neoPortalSourceUUID != null) output.putString("NeoPortalSource", neoPortalSourceUUID.toString());
        if (neoPortalDestUUID != null) output.putString("NeoPortalDest", neoPortalDestUUID.toString());
    }

    public float getPortalWidth() { return entityData.get(DATA_WIDTH); }
    public float getPortalHeight() { return entityData.get(DATA_HEIGHT); }

    public void setPortalWidth(float width) {
        float min = (float) ModConfig.getPortalMinSize();
        float max = (float) ModConfig.getPortalMaxSize();
        this.entityData.set(DATA_WIDTH, Mth.clamp(width, min, max));
    }

    public void setPortalHeight(float height) {
        float min = (float) ModConfig.getPortalMinSize();
        float max = (float) ModConfig.getPortalMaxSize();
        this.entityData.set(DATA_HEIGHT, Mth.clamp(height, min, max));
    }
    public String getPortalName() { return portalName; }
    public void setPortalName(String name) { this.portalName = name; }
    @Nullable
    public UUID getDialedDestinationUUID() { return dialedDestinationUUID; }
    @Nullable
    public UUID getCasterUUID() { return casterUUID; }
    @Nullable
    public ResourceKey<Level> getDestinationDimension() { return destinationDimension; }
    @Nullable
    public Vec3 getDestinationPos() { return destinationPos; }
    public boolean isDialed() { return entityData.get(DATA_IS_DIALED); }
    public String getDestDimensionId() { return entityData.get(DATA_DEST_DIMENSION); }
    public boolean hasNeoPortals() { return entityData.get(DATA_HAS_NEO_PORTALS); }
}
