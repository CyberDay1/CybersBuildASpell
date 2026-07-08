package buildaspell.portal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class PortalInfo {
    public static final Codec<PortalInfo> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.fieldOf("uuid").forGetter(p -> p.portalUUID.toString()),
            Codec.STRING.fieldOf("name").forGetter(PortalInfo::getName),
            ResourceLocation.CODEC.fieldOf("dimension").forGetter(p -> p.dimension.location()),
            Vec3.CODEC.fieldOf("position").forGetter(PortalInfo::getPosition),
            Codec.STRING.optionalFieldOf("owner_uuid").forGetter(p -> Optional.ofNullable(p.ownerUUID).map(UUID::toString))
    ).apply(inst, (uuid, name, dim, pos, owner) -> new PortalInfo(
            UUID.fromString(uuid),
            name,
            ResourceKey.create(Registries.DIMENSION, dim),
            pos,
            owner.map(UUID::fromString).orElse(null)
    )));

    private final UUID portalUUID;
    private String name;
    private final ResourceKey<Level> dimension;
    private final Vec3 position;
    @Nullable
    private final UUID ownerUUID;

    public PortalInfo(UUID portalUUID, String name, ResourceKey<Level> dimension, Vec3 position, @Nullable UUID ownerUUID) {
        this.portalUUID = portalUUID;
        this.name = name;
        this.dimension = dimension;
        this.position = position;
        this.ownerUUID = ownerUUID;
    }

    public UUID getPortalUUID() { return portalUUID; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public ResourceKey<Level> getDimension() { return dimension; }
    public Vec3 getPosition() { return position; }
    @Nullable
    public UUID getOwnerUUID() { return ownerUUID; }
}
