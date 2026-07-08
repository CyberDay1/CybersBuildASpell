package buildaspell.registry;

import buildaspell.BuildASpell;
import buildaspell.entity.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, BuildASpell.MOD_ID);

    private static ResourceKey<EntityType<?>> key(String name) {
        return ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(BuildASpell.MOD_ID, name));
    }

    public static final DeferredHolder<EntityType<?>, EntityType<SpellProjectileEntity>> SPELL_PROJECTILE =
            ENTITY_TYPES.register("spell_projectile", () -> EntityType.Builder.<SpellProjectileEntity>of(
                    SpellProjectileEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .clientTrackingRange(8)
                    .updateInterval(2)
                    .build(key("spell_projectile")));

    public static final DeferredHolder<EntityType<?>, EntityType<BlackHoleEntity>> BLACK_HOLE =
            ENTITY_TYPES.register("black_hole", () -> EntityType.Builder.<BlackHoleEntity>of(
                    BlackHoleEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .clientTrackingRange(10)
                    .updateInterval(20)
                    .fireImmune()
                    .build(key("black_hole")));

    public static final DeferredHolder<EntityType<?>, EntityType<TornadoEntity>> TORNADO =
            ENTITY_TYPES.register("tornado", () -> EntityType.Builder.<TornadoEntity>of(
                    TornadoEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .clientTrackingRange(10)
                    .updateInterval(20)
                    .fireImmune()
                    .build(key("tornado")));

    public static final DeferredHolder<EntityType<?>, EntityType<PortalEntity>> PORTAL =
            ENTITY_TYPES.register("portal", () -> EntityType.Builder.<PortalEntity>of(
                    PortalEntity::new, MobCategory.MISC)
                    .sized(1.5f, 2.5f)
                    .clientTrackingRange(10)
                    .updateInterval(20)
                    .fireImmune()
                    .build(key("portal")));

    public static final DeferredHolder<EntityType<?>, EntityType<DelayedSpellEntity>> DELAYED_SPELL =
            ENTITY_TYPES.register("delayed_spell", () -> EntityType.Builder.<DelayedSpellEntity>of(
                    DelayedSpellEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .clientTrackingRange(4)
                    .updateInterval(20)
                    .noSummon()
                    .build(key("delayed_spell")));

    public static final DeferredHolder<EntityType<?>, EntityType<DurationAreaEntity>> DURATION_AREA =
            ENTITY_TYPES.register("duration_area", () -> EntityType.Builder.<DurationAreaEntity>of(
                    DurationAreaEntity::new, MobCategory.MISC)
                    .sized(1.0f, 1.0f)
                    .clientTrackingRange(8)
                    .updateInterval(20)
                    .fireImmune()
                    .build(key("duration_area")));

    public static final DeferredHolder<EntityType<?>, EntityType<FlightDurationEntity>> FLIGHT_DURATION =
            ENTITY_TYPES.register("flight_duration", () -> EntityType.Builder.<FlightDurationEntity>of(
                    FlightDurationEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .clientTrackingRange(4)
                    .updateInterval(40)
                    .noSummon()
                    .build(key("flight_duration")));

    public static final DeferredHolder<EntityType<?>, EntityType<FortressBarrierEntity>> FORTRESS_BARRIER =
            ENTITY_TYPES.register("fortress_barrier", () -> EntityType.Builder.<FortressBarrierEntity>of(
                    FortressBarrierEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .clientTrackingRange(8)
                    .updateInterval(20)
                    .fireImmune()
                    .noSummon()
                    .build(key("fortress_barrier")));

    public static final DeferredHolder<EntityType<?>, EntityType<BlizzardEntity>> BLIZZARD =
            ENTITY_TYPES.register("blizzard", () -> EntityType.Builder.<BlizzardEntity>of(
                    BlizzardEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .clientTrackingRange(10)
                    .updateInterval(20)
                    .fireImmune()
                    .noSummon()
                    .build(key("blizzard")));

    public static final DeferredHolder<EntityType<?>, EntityType<StormCloudEntity>> STORM_CLOUD =
            ENTITY_TYPES.register("storm_cloud", () -> EntityType.Builder.<StormCloudEntity>of(
                    StormCloudEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .clientTrackingRange(12)
                    .updateInterval(20)
                    .fireImmune()
                    .noSummon()
                    .build(key("storm_cloud")));

    public static final DeferredHolder<EntityType<?>, EntityType<RuneMarkerEntity>> RUNE_MARKER =
            ENTITY_TYPES.register("rune_marker", () -> EntityType.Builder.<RuneMarkerEntity>of(
                    RuneMarkerEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.1f)
                    .clientTrackingRange(8)
                    .updateInterval(10)
                    .noSummon()
                    .build(key("rune_marker")));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
