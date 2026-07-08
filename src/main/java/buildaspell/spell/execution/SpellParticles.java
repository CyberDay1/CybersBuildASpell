package buildaspell.spell.execution;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

public final class SpellParticles {
    private static final RandomSource RANDOM = RandomSource.create();

    public static void burst(ServerLevel level, Vec3 pos, ParticleOptions particle, int count, double spread, double speed) {
        level.sendParticles(particle, pos.x, pos.y, pos.z, count, spread, spread, spread, speed);
    }

    public static void ring(ServerLevel level, Vec3 pos, ParticleOptions particle, int count, double radius, double yOffset) {
        for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2 * i) / count;
            double x = pos.x + Math.cos(angle) * radius;
            double z = pos.z + Math.sin(angle) * radius;
            level.sendParticles(particle, x, pos.y + yOffset, z, 1, 0, 0, 0, 0);
        }
    }

    public static void sphere(ServerLevel level, Vec3 pos, ParticleOptions particle, int count, double radius) {
        for (int i = 0; i < count; i++) {
            double theta = RANDOM.nextDouble() * Math.PI * 2;
            double phi = Math.acos(2 * RANDOM.nextDouble() - 1);
            double x = pos.x + radius * Math.sin(phi) * Math.cos(theta);
            double y = pos.y + radius * Math.sin(phi) * Math.sin(theta);
            double z = pos.z + radius * Math.cos(phi);
            level.sendParticles(particle, x, y, z, 1, 0, 0, 0, 0);
        }
    }

    public static void line(ServerLevel level, Vec3 start, Vec3 end, ParticleOptions particle, int segments) {
        Vec3 direction = end.subtract(start);
        double length = direction.length();
        Vec3 step = direction.normalize().scale(length / segments);

        for (int i = 0; i <= segments; i++) {
            Vec3 point = start.add(step.scale(i));
            level.sendParticles(particle, point.x, point.y, point.z, 1, 0.02, 0.02, 0.02, 0);
        }
    }

    public static void arc(ServerLevel level, Vec3 start, Vec3 end, ParticleOptions particle, int segments, double arcHeight) {
        Vec3 direction = end.subtract(start);

        for (int i = 0; i <= segments; i++) {
            double t = (double) i / segments;
            double x = start.x + direction.x * t;
            double z = start.z + direction.z * t;
            double yArc = Math.sin(t * Math.PI) * arcHeight;
            double y = start.y + direction.y * t + yArc;

            level.sendParticles(particle, x, y, z, 1, 0.02, 0.02, 0.02, 0);
        }
    }

    public static void spiral(ServerLevel level, Vec3 pos, ParticleOptions particle, int count, double radius, double height, double rotations) {
        for (int i = 0; i < count; i++) {
            double t = (double) i / count;
            double angle = t * Math.PI * 2 * rotations;
            double r = radius * (1 - t * 0.5);
            double x = pos.x + Math.cos(angle) * r;
            double z = pos.z + Math.sin(angle) * r;
            double y = pos.y + t * height;
            level.sendParticles(particle, x, y, z, 1, 0, 0, 0, 0);
        }
    }

    public static void impact(ServerLevel level, Vec3 pos, ParticleOptions particle, int count, double speed) {
        for (int i = 0; i < count; i++) {
            double vx = (RANDOM.nextDouble() - 0.5) * speed;
            double vy = RANDOM.nextDouble() * speed * 0.5;
            double vz = (RANDOM.nextDouble() - 0.5) * speed;
            level.sendParticles(particle, pos.x, pos.y, pos.z, 0, vx, vy, vz, 1);
        }
    }

    public static void directional(ServerLevel level, Vec3 pos, Vec3 direction, ParticleOptions particle, int count, double spread) {
        Vec3 norm = direction.normalize();
        for (int i = 0; i < count; i++) {
            double vx = norm.x + (RANDOM.nextDouble() - 0.5) * spread;
            double vy = norm.y + (RANDOM.nextDouble() - 0.5) * spread;
            double vz = norm.z + (RANDOM.nextDouble() - 0.5) * spread;
            level.sendParticles(particle, pos.x, pos.y, pos.z, 0, vx, vy, vz, 0.3);
        }
    }

    // Themed particle effects

    public static void damageHit(ServerLevel level, Vec3 pos) {
        burst(level, pos, ParticleTypes.CRIT, 8, 0.3, 0.1);
        burst(level, pos, ParticleTypes.ENCHANTED_HIT, 5, 0.2, 0.05);
    }

    public static void healEffect(ServerLevel level, Vec3 pos) {
        burst(level, pos, ParticleTypes.HEART, 3, 0.3, 0.05);
        spiral(level, pos, ParticleTypes.HAPPY_VILLAGER, 12, 0.5, 1.5, 1.5);
    }

    public static void teleportEffect(ServerLevel level, Vec3 pos) {
        burst(level, pos, ParticleTypes.PORTAL, 30, 0.5, 0.3);
        ring(level, pos, ParticleTypes.REVERSE_PORTAL, 16, 0.8, 0);
    }

    public static void pullEffect(ServerLevel level, Vec3 origin, Vec3 entityPos) {
        Vec3 direction = origin.subtract(entityPos).normalize();
        directional(level, entityPos.add(0, 0.5, 0), direction, ParticleTypes.CLOUD, 5, 0.2);
    }

    public static void pushEffect(ServerLevel level, Vec3 origin, Vec3 entityPos) {
        Vec3 direction = entityPos.subtract(origin).normalize();
        directional(level, entityPos.add(0, 0.5, 0), direction, ParticleTypes.CLOUD, 5, 0.2);
    }

    public static void freezeEffect(ServerLevel level, Vec3 pos) {
        burst(level, pos, ParticleTypes.SNOWFLAKE, 15, 0.5, 0.1);
        burst(level, pos, ParticleTypes.WHITE_ASH, 10, 0.4, 0.05);
        ring(level, pos, ParticleTypes.SNOWFLAKE, 12, 1.0, 0.1);
    }

    public static void igniteEffect(ServerLevel level, Vec3 pos) {
        burst(level, pos, ParticleTypes.FLAME, 12, 0.4, 0.15);
        burst(level, pos, ParticleTypes.LAVA, 5, 0.3, 0.1);
        spiral(level, pos, ParticleTypes.SMOKE, 8, 0.3, 1.0, 1);
    }

    public static void lightningPreStrike(ServerLevel level, Vec3 pos) {
        ring(level, pos, ParticleTypes.ELECTRIC_SPARK, 16, 1.0, 0);
        burst(level, pos, ParticleTypes.ELECTRIC_SPARK, 10, 0.2, 0.3);
    }

    public static void poisonEffect(ServerLevel level, Vec3 pos) {
        burst(level, pos, ParticleTypes.ITEM_SLIME, 8, 0.3, 0.05);
        for (int i = 0; i < 5; i++) {
            double x = pos.x + (RANDOM.nextDouble() - 0.5) * 0.5;
            double z = pos.z + (RANDOM.nextDouble() - 0.5) * 0.5;
            level.sendParticles(ParticleTypes.ITEM_SLIME, x, pos.y + 1.5, z, 1, 0, -0.1, 0, 0.02);
        }
    }

    public static void witherEffect(ServerLevel level, Vec3 pos) {
        burst(level, pos, ParticleTypes.SMOKE, 10, 0.3, 0.05);
        burst(level, pos, ParticleTypes.SQUID_INK, 5, 0.2, 0.03);
    }

    public static void chainConnect(ServerLevel level, Vec3 from, Vec3 to) {
        arc(level, from.add(0, 1, 0), to.add(0, 1, 0), ParticleTypes.ELECTRIC_SPARK, 15, 0.3);
        burst(level, to.add(0, 1, 0), ParticleTypes.CRIT, 5, 0.2, 0.1);
    }

    public static void projectileImpact(ServerLevel level, Vec3 pos) {
        burst(level, pos, ParticleTypes.GLOW, 15, 0.3, 0.2);
        impact(level, pos, ParticleTypes.ENCHANTED_HIT, 12, 0.5);
        ring(level, pos, ParticleTypes.END_ROD, 8, 0.5, 0);
    }

    public static void runeCharging(ServerLevel level, Vec3 pos, float progress) {
        int particleCount = (int) (progress * 20);
        double radius = 0.8 + progress * 0.4;
        ring(level, pos, ParticleTypes.ENCHANT, particleCount, radius, 0.1);

        if (progress > 0.5f) {
            spiral(level, pos, ParticleTypes.END_ROD, (int)(progress * 10), radius * 0.5, 1.5, 2);
        }

        if (progress > 0.8f) {
            burst(level, pos.add(0, 0.5, 0), ParticleTypes.WITCH, 3, 0.2, 0.05);
        }
    }

    public static void runeActivate(ServerLevel level, Vec3 pos) {
        ring(level, pos, ParticleTypes.END_ROD, 1, 0, 0.5);
        burst(level, pos, ParticleTypes.ENCHANTED_HIT, 25, 0.8, 0.3);
        sphere(level, pos.add(0, 0.5, 0), ParticleTypes.END_ROD, 20, 1.2);
    }

    public static void linger(ServerLevel level, Vec3 pos, ParticleOptions particle, double radius) {
        for (int i = 0; i < 3; i++) {
            double x = pos.x + (RANDOM.nextDouble() - 0.5) * radius * 2;
            double y = pos.y + RANDOM.nextDouble() * 0.5;
            double z = pos.z + (RANDOM.nextDouble() - 0.5) * radius * 2;
            level.sendParticles(particle, x, y, z, 1, 0, 0.02, 0, 0);
        }
    }

    public static void summonCircle(ServerLevel level, Vec3 pos) {
        ring(level, pos, ParticleTypes.ENCHANT, 24, 1.5, 0.1);
        ring(level, pos, ParticleTypes.SOUL_FIRE_FLAME, 12, 1.2, 0.1);
        spiral(level, pos, ParticleTypes.WITCH, 15, 1.0, 2.0, 2);
    }

    public static void blinkTrail(ServerLevel level, Vec3 start, Vec3 end) {
        line(level, start.add(0, 1, 0), end.add(0, 1, 0), ParticleTypes.REVERSE_PORTAL, 20);
        burst(level, start.add(0, 1, 0), ParticleTypes.PORTAL, 15, 0.3, 0.1);
        burst(level, end.add(0, 1, 0), ParticleTypes.PORTAL, 15, 0.3, 0.1);
    }

    public static void yeetEffect(ServerLevel level, Vec3 pos, Vec3 direction) {
        directional(level, pos.add(0, 0.5, 0), direction, ParticleTypes.CLOUD, 10, 0.3);
        burst(level, pos.add(0, 0.5, 0), ParticleTypes.POOF, 8, 0.3, 0.1);
    }

    public static void launchEffect(ServerLevel level, Vec3 pos) {
        burst(level, pos, ParticleTypes.CLOUD, 15, 0.4, 0.2);
        spiral(level, pos, ParticleTypes.FIREWORK, 12, 0.5, 1.5, 1.5);
        ring(level, pos, ParticleTypes.POOF, 10, 0.6, 0);
    }

    public static void slamEffect(ServerLevel level, Vec3 pos) {
        ring(level, pos, ParticleTypes.CAMPFIRE_COSY_SMOKE, 16, 1.0, 0.1);
        burst(level, pos, ParticleTypes.CLOUD, 12, 0.5, 0.15);
        impact(level, pos, ParticleTypes.CRIT, 10, 0.4);
    }
}
