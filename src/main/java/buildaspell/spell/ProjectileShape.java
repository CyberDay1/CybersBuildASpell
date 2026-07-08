package buildaspell.spell;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/**
 * Procedural projectile shapes drawn by SpellProjectileRenderer.
 * CROSS is the historical look (two crossed billboard quads).
 */
public enum ProjectileShape implements StringRepresentable {
    CROSS("cross"),
    CUBE("cube"),
    RING("ring"),
    SPHERE("sphere");

    public static final Codec<ProjectileShape> CODEC = StringRepresentable.fromEnum(ProjectileShape::values);

    private final String id;

    ProjectileShape(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    public static ProjectileShape fromId(String id) {
        for (ProjectileShape shape : values()) {
            if (shape.id.equals(id)) return shape;
        }
        return CROSS;
    }
}
