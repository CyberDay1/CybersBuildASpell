package buildaspell.spell.execution;

import buildaspell.config.ModConfig;
import buildaspell.entity.DelayedSpellEntity;
import buildaspell.entity.DurationAreaEntity;
import buildaspell.entity.RuneMarkerEntity;
import buildaspell.entity.SpellProjectileEntity;
import buildaspell.item.BlankRuneItem;
import buildaspell.mana.ManaHelper;
import buildaspell.mana.PlayerManaData;
import buildaspell.registry.ModAttachments;
import buildaspell.registry.ModEntities;
import buildaspell.spell.*;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpellExecutor {

    public static class EffectGroup {
        /** Non-null for enum-backed built-in effects; null for datapack-only effects. */
        public final SpellEffect effect;
        /** Non-null for datapack-only effects ({@code effect == null}); else null. */
        public final net.minecraft.resources.Identifier dataEffectId;
        public final Map<SpellModifier, Integer> modifiers;

        public EffectGroup(SpellEffect effect) {
            this.effect = effect;
            this.dataEffectId = null;
            this.modifiers = new HashMap<>();
        }

        public EffectGroup(net.minecraft.resources.Identifier dataEffectId) {
            this.effect = null;
            this.dataEffectId = dataEffectId;
            this.modifiers = new HashMap<>();
        }
    }

    private static List<EffectGroup> parseOrderedComponents(Spell spell) {
        List<SpellComponent> components = spell.getComponents();
        List<EffectGroup> groups = new ArrayList<>();
        EffectGroup currentGroup = null;
        // Modifiers placed before the first effect used to be silently dropped (the player paid
        // mana for nothing). Buffer them and fold them into the first effect group instead.
        Map<SpellModifier, Integer> leadingModifiers = new java.util.LinkedHashMap<>();

        for (SpellComponent component : components) {
            if (component instanceof SpellComponent.Effect eff) {
                currentGroup = new EffectGroup(eff.effect());
                groups.add(currentGroup);
            } else if (component instanceof SpellComponent.DataEffect de) {
                currentGroup = new EffectGroup(de.effectId());
                groups.add(currentGroup);
            } else if (component instanceof SpellComponent.Modifier mod) {
                if (currentGroup != null) {
                    currentGroup.modifiers.merge(mod.modifier(), 1, Integer::sum);
                } else {
                    leadingModifiers.merge(mod.modifier(), 1, Integer::sum);
                }
            }
        }

        if (!leadingModifiers.isEmpty() && !groups.isEmpty()) {
            EffectGroup first = groups.get(0);
            leadingModifiers.forEach((modifier, count) -> first.modifiers.merge(modifier, count, Integer::sum));
        }

        return groups;
    }

    public static boolean executeSpell(Player caster, Spell spell) {
        return executeSpell(caster, spell, true);
    }

    /**
     * Casts a spell, optionally bypassing the BaS mana pool.
     *
     * <p>When {@code consumeMana} is false the mana check AND the refund-on-failure path are
     * both skipped entirely (the BaS pool is never read or written) — used by external casters
     * such as NeoOrigins origin powers, which charge their own resource instead. Config
     * enable/disable toggles for the delivery, effects and modifiers are always honored,
     * regardless of {@code consumeMana}.
     */
    public static boolean executeSpell(Player caster, Spell spell, boolean consumeMana) {
        if (spell.getDelivery() == null || !spell.hasAnyEffect()) {
            return false;
        }

        if (!ModConfig.isDeliveryEnabled(spell.getDelivery())) {
            return false;
        }

        for (SpellEffect effect : spell.getEffects()) {
            if (!ModConfig.isEffectEnabled(effect)) {
                return false;
            }
        }

        for (SpellComponent component : spell.getComponents()) {
            if (component instanceof SpellComponent.Modifier mod
                    && !ModConfig.isModifierEnabled(mod.modifier())) {
                return false;
            }
        }

        float manaCost = spell.getManaCost() * buildaspell.item.WandItem.heldDiscountMultiplier(caster);
        float spellPower = ManaHelper.getSpellPower(caster);

        PlayerManaData manaData = caster.getData(ModAttachments.PLAYER_MANA.get());
        boolean chargeMana = consumeMana && !caster.getAbilities().instabuild;
        if (chargeMana) {
            if (!manaData.consumeMana(manaCost)) {
                return false;
            }
        }
        float manaSpent = chargeMana ? manaCost : 0f;

        DeliveryMethod delivery = spell.getDelivery();

        if (delivery == DeliveryMethod.CAST || delivery == DeliveryMethod.TRACKING) {
            caster.level().playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.5f, 1.2f);
            spawnProjectile(caster, spell, delivery == DeliveryMethod.TRACKING);
            awardCastEssence(caster, manaSpent);
            return true;
        }

        if (delivery == DeliveryMethod.RUNE) {
            caster.level().playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.EVOKER_PREPARE_ATTACK, SoundSource.PLAYERS, 0.6f, 0.9f);
            spawnRuneMarker(caster, spell);
            awardCastEssence(caster, manaSpent);
            return true;
        }

        if (delivery == DeliveryMethod.TRAP) {
            caster.level().playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.EVOKER_PREPARE_ATTACK, SoundSource.PLAYERS, 0.6f, 0.7f);
            spawnTrapMarker(caster, spell);
            awardCastEssence(caster, manaSpent);
            return true;
        }

        if (delivery == DeliveryMethod.TOUCH) {
            // Charge the spell into the caster's hands; the next melee hit or interaction discharges it.
            int durationTicks = ModConfig.deliveryInt(DeliveryMethod.TOUCH, "durationTicks", 200);
            long expiry = caster.level().getGameTime() + durationTicks;
            buildaspell.spell.ImbueManager.setImbue(caster.getUUID(), spell, expiry);
            caster.level().playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.EVOKER_PREPARE_ATTACK, SoundSource.PLAYERS, 0.5f, 1.4f);
            awardCastEssence(caster, manaSpent);
            return true;
        }

        Vec3 origin = determineOrigin(caster, delivery);
        if (origin == null) {
            if (chargeMana) {
                manaData.addMana(manaCost);
            }
            return false;
        }

        caster.level().playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.5f, 1.0f);

        int delay = spell.getDelayLevel() * ModConfig.modifierInt(SpellModifier.DELAY, "ticksPerStack", 10);
        if (delay > 0 && caster.level() instanceof ServerLevel serverLevel) {
            scheduleDelayedDirectCast(serverLevel, caster, origin, spell, spellPower, delay);
        } else {
            // Route through the shared location-cast path so SELF/SIGHT delivery honors combos,
            // the DURATION area-over-time branch, and lingering particles identically to the
            // projectile/rune/delayed deliveries (which already call executeSpellAtLocation).
            executeSpellAtLocation(caster, spell, origin);
        }

        awardCastEssence(caster, manaSpent);
        return true;
    }

    /**
     * Grants rune essence for casting a spell, scaled by the mana actually spent. No-op when no
     * mana was consumed (creative casters, or external casters that bypass the BaS mana pool), so
     * progression always reflects real spellcasting effort.
     */
    private static void awardCastEssence(Player caster, float manaSpent) {
        if (manaSpent <= 0f) {
            return;
        }
        int essence = (int) Math.round(manaSpent * ModConfig.getCastEssenceRatio());
        if (essence <= 0) {
            return;
        }
        BlankRuneItem.depositEssence(caster, essence);
    }

    /**
     * Runs a spell's effects at an explicit location, optionally bypassing the BaS mana pool.
     *
     * <p>Honors the same config enable/disable gating and {@code consumeMana} contract as
     * {@link #executeSpell(Player, Spell, boolean)} (the delivery, if present, plus every effect
     * and modifier must be enabled in {@link ModConfig}). Returns false without side effects if
     * gating rejects the spell or — when {@code consumeMana} is true — the caster cannot afford it.
     */
    public static boolean executeSpellAtLocation(Player caster, Spell spell, Vec3 location, boolean consumeMana) {
        if (!spell.hasAnyEffect()) {
            return false;
        }

        DeliveryMethod delivery = spell.getDelivery();
        if (delivery != null && !ModConfig.isDeliveryEnabled(delivery)) {
            return false;
        }

        for (SpellEffect effect : spell.getEffects()) {
            if (!ModConfig.isEffectEnabled(effect)) {
                return false;
            }
        }

        for (SpellComponent component : spell.getComponents()) {
            if (component instanceof SpellComponent.Modifier mod
                    && !ModConfig.isModifierEnabled(mod.modifier())) {
                return false;
            }
        }

        boolean charged = consumeMana && !caster.getAbilities().instabuild;
        float manaCost = spell.getManaCost() * buildaspell.item.WandItem.heldDiscountMultiplier(caster);
        if (charged) {
            PlayerManaData manaData = caster.getData(ModAttachments.PLAYER_MANA.get());
            if (!manaData.consumeMana(manaCost)) {
                return false;
            }
        }

        executeSpellAtLocation(caster, spell, location);
        if (charged) {
            awardCastEssence(caster, manaCost);
        }
        return true;
    }

    public static void executeSpellAtLocation(Player caster, Spell spell, Vec3 location) {
        executeSpellAtLocation(caster, spell, location, ManaHelper.getSpellPower(caster));
    }

    /**
     * Location-cast entry point with an explicit spell power, used when the power was resolved
     * earlier than execution time (delayed casts) or scaled down from the original (ECHO falloff).
     */
    public static void executeSpellAtLocation(Player caster, Spell spell, Vec3 location, float spellPower) {
        if (!spell.hasAnyEffect()) {
            return;
        }

        // Combos manage their own lifetime/duration (a void_rift portal persists on its own,
        // a fortress spawns a timed barrier entity, etc.), so they MUST be detected before the
        // duration-area branch below. A combo that includes the Duration modifier — void_rift and
        // fortress both do — would otherwise be wrapped in a DurationAreaEntity that re-casts the
        // same spell every few ticks, and since that spell still has a Duration level it spawns yet
        // another DurationAreaEntity each pulse, exploding exponentially until the server stalls and
        // the client exhausts its heap.
        buildaspell.spell.data.ComboDefinition combo = buildaspell.spell.data.ComboRegistry.detect(spell);
        if (combo != null) {
            combo.run(new buildaspell.spell.data.SpellContext(caster, caster.level(), location, spell, spellPower));
            if (caster.level() instanceof ServerLevel serverLevel) {
                spawnLingeringParticles(serverLevel, location, spell.getRange());
            }
            scheduleComboEchoes(caster, spell, location, spellPower);
            return;
        }

        // LINGER turns the cast into a persistent area (lingering-potion style); the Duration
        // modifier alone also does. Both are modifiers now and share the same DurationAreaEntity:
        // it pulses the spell's effects on an interval, and its lifetime scales with Duration.
        if (spell.getDurationLevel() > 0 || spell.getModifiers().contains(SpellModifier.LINGER)) {
            Level level = caster.level();
            DurationAreaEntity durationArea = new DurationAreaEntity(
                    level, caster, spell, spellPower, location
            );
            level.addFreshEntity(durationArea);

            if (level instanceof ServerLevel) {
                level.playSound(null, location.x, location.y, location.z,
                        SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.6f, 1.2f);
            }
            return;
        }

        executeEffectsOnce(caster, spell, location, spellPower);

        if (caster.level() instanceof ServerLevel serverLevel) {
            spawnLingeringParticles(serverLevel, location, spell.getRange());
        }
    }

    /**
     * ECHO support for combos: the combo path above returns before per-group modifier handling
     * runs, so echoes on a combo spell are scheduled here instead. Each echo re-casts an
     * ECHO-stripped copy of the spell at falloff-scaled power — the copy still matches the combo
     * recipe (ECHO is never part of one), so the combo re-fires, and because the copy carries no
     * ECHO the re-cast schedules nothing further.
     */
    private static void scheduleComboEchoes(Player caster, Spell spell, Vec3 location, float spellPower) {
        int echoLevel = spell.getModifierCount(SpellModifier.ECHO);
        if (echoLevel <= 0 || !(caster.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Spell echoSpell = stripModifier(spell, SpellModifier.ECHO);
        int echoDelayPer = ModConfig.modifierInt(SpellModifier.ECHO, "delayTicksPerEcho", 10);
        double echoFalloff = ModConfig.modifierDouble(SpellModifier.ECHO, "powerFalloff", 0.8);
        for (int echo = 1; echo <= echoLevel; echo++) {
            float echoPower = spellPower * (float) Math.pow(echoFalloff, echo);
            DelayedSpellEntity echoEntity = new DelayedSpellEntity(
                    serverLevel, caster, echoSpell, echoPower, echo * echoDelayPer, location
            );
            serverLevel.addFreshEntity(echoEntity);
        }
    }

    /**
     * Location-cast entry point that honors the DELAY modifier: with Delay stacks the execution
     * is deferred at the impact point by a scheduled DelayedSpellEntity; otherwise it runs now.
     * Used by the deliveries whose impact moment is only discovered at runtime — projectile hits,
     * rune and trap triggers, touch discharges. The direct SELF/SIGHT branch in
     * {@link #executeSpell} computes its own delay before calling down, and DelayedSpellEntity
     * itself calls the plain location-cast, so a delayed cast never re-delays.
     */
    public static void executeSpellAtLocationWithDelay(Player caster, Spell spell, Vec3 location) {
        int delay = spell.getDelayLevel() * ModConfig.modifierInt(SpellModifier.DELAY, "ticksPerStack", 10);
        if (delay > 0 && caster.level() instanceof ServerLevel serverLevel) {
            scheduleDelayedDirectCast(serverLevel, caster, location, spell, ManaHelper.getSpellPower(caster), delay);
            return;
        }
        executeSpellAtLocation(caster, spell, location);
    }

    /**
     * Execution entry point for rune and trap firings. Rune/trap timing already consumed the
     * Duration modifier (runes charge longer, traps stay armed longer), so Duration is stripped
     * here to stop it from also spawning a lingering area at fire time — double-charging one
     * modifier for two behaviors. Combos are the exception: their recipes may require the
     * Duration stacks to be present, so a combo spell is passed through untouched (the combo
     * manages its own lifetime anyway).
     */
    public static void executeRuneTriggeredSpell(Player caster, Spell spell, Vec3 location) {
        Spell toRun = spell;
        if (spell.getDurationLevel() > 0 && buildaspell.spell.data.ComboRegistry.detect(spell) == null) {
            toRun = stripModifier(spell, SpellModifier.DURATION);
        }
        executeSpellAtLocationWithDelay(caster, toRun, location);
    }

    /** Copy of the spell with every instance of the given modifier removed. */
    private static Spell stripModifier(Spell spell, SpellModifier modifier) {
        List<SpellComponent> filtered = new ArrayList<>();
        for (SpellComponent component : spell.getComponents()) {
            if (component instanceof SpellComponent.Modifier mod && mod.modifier() == modifier) {
                continue;
            }
            filtered.add(component);
        }
        return new Spell(spell.getDelivery(), filtered, spell.getVisual());
    }

    /**
     * Runs the spell's effect groups a single time at a location without spawning a
     * DurationAreaEntity. Used by DurationAreaEntity for its periodic pulses so that a duration
     * spell does not recursively spawn more duration areas.
     */
    public static void executeEffectsOnce(Player caster, Spell spell, Vec3 location, float spellPower) {
        if (!spell.hasAnyEffect()) {
            return;
        }
        List<EffectGroup> effectGroups = parseOrderedComponents(spell);
        for (EffectGroup group : effectGroups) {
            executeEffectWithModifiers(caster, caster.level(), location, group, spell, spellPower);
        }
    }

    private static void spawnLingeringParticles(ServerLevel level, Vec3 origin, float range) {
        net.minecraft.world.entity.AreaEffectCloud cloud = new net.minecraft.world.entity.AreaEffectCloud(level, origin.x, origin.y, origin.z);
        cloud.setCustomParticle(ParticleTypes.ENCHANT);
        cloud.setRadius(Math.max(range, 1.0f));
        cloud.setDuration(30);
        cloud.setWaitTime(0);
        level.addFreshEntity(cloud);
    }

    private static void spawnProjectile(Player caster, Spell spell, boolean isTracking) {
        Level level = caster.level();
        Vec3 lookVec = caster.getLookAngle();
        Vec3 spawnPos = caster.getEyePosition();

        int projectileCount = 1 + spell.getDoubleCount() + spell.getSplitLevel();

        for (int i = 0; i < projectileCount; i++) {
            SpellProjectileEntity projectile = new SpellProjectileEntity(
                    level, caster, spell, isTracking
            );

            projectile.setPos(spawnPos.x, spawnPos.y, spawnPos.z);

            double speed = ModConfig.deliveryDouble(DeliveryMethod.CAST, "projectileBaseSpeed", 1.5)
                    + spell.getAccelerateLevel() * ModConfig.modifierDouble(SpellModifier.ACCELERATE, "speedPerStack", 0.5);
            Vec3 motion = lookVec.scale(speed);

            if (projectileCount > 1) {
                double spread = ModConfig.deliveryDouble(DeliveryMethod.CAST, "projectileSpread", 0.1);
                double offsetX = (i - projectileCount / 2.0) * spread;
                Vec3 right = lookVec.cross(new Vec3(0, 1, 0)).normalize();
                motion = motion.add(right.scale(offsetX));
            }

            projectile.setDeltaMovement(motion);
            level.addFreshEntity(projectile);
        }
    }

    private static void spawnRuneMarker(Player caster, Spell spell) {
        Level level = caster.level();
        double reach = ModConfig.deliveryDouble(DeliveryMethod.RUNE, "reach", 30.0);
        AimedSurface aim = aimSurface(caster, reach);

        Vec3 offset = Vec3.atLowerCornerOf(aim.direction.getUnitVec3i()).scale(0.01);
        Vec3 position = aim.position.add(offset);

        RuneMarkerEntity runeMarker = new RuneMarkerEntity(
                level, caster, spell, position, aim.direction
        );

        level.addFreshEntity(runeMarker);
    }

    private static void spawnTrapMarker(Player caster, Spell spell) {
        Level level = caster.level();
        double reach = ModConfig.deliveryDouble(DeliveryMethod.TRAP, "reach", 30.0);
        AimedSurface aim = aimSurface(caster, reach);

        Vec3 offset = Vec3.atLowerCornerOf(aim.direction.getUnitVec3i()).scale(0.01);
        Vec3 position = aim.position.add(offset);

        RuneMarkerEntity trapMarker = new RuneMarkerEntity(
                level, caster, spell, position, aim.direction, true
        );

        level.addFreshEntity(trapMarker);
    }

    /** The surface point a rune/trap should be placed on, plus the face it sits against. */
    private record AimedSurface(Vec3 position, Direction direction) {}

    /**
     * Raycast from the caster's eyes along their look vector out to {@code reach} blocks and return the
     * surface the crosshair lands on. If the look ray hits nothing within reach (looking out over a gap
     * or at the sky), drop straight down from the ray's end to the first surface below so the marker
     * still lands where the player is aiming instead of stopping short at a fixed distance.
     */
    private static AimedSurface aimSurface(Player caster, double reach) {
        Level level = caster.level();
        HitResult hit = caster.pick(reach, 0.0f, false);
        if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
            return new AimedSurface(hit.getLocation(), blockHit.getDirection());
        }

        // Missed: drop from the aim point to find ground beneath it.
        Vec3 aimEnd = hit.getLocation();
        Vec3 down = aimEnd.subtract(0, reach * 2.0, 0);
        BlockHitResult groundHit = level.clip(new ClipContext(
                aimEnd, down, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, caster));
        if (groundHit.getType() == HitResult.Type.BLOCK) {
            return new AimedSurface(groundHit.getLocation(), groundHit.getDirection());
        }

        // Nothing below either: float it at the aim point lying flat.
        return new AimedSurface(aimEnd, Direction.UP);
    }

    private static void scheduleDelayedDirectCast(ServerLevel level, Player caster, Vec3 origin, Spell spell, float spellPower, int delay) {
        DelayedSpellEntity delayedSpell = new DelayedSpellEntity(
                level, caster, spell, spellPower, delay, origin
        );
        level.addFreshEntity(delayedSpell);
    }

    private static Vec3 determineOrigin(Player caster, DeliveryMethod delivery) {
        return switch (delivery) {
            case SELF -> caster.position();
            case SIGHT -> {
                var hit = caster.pick(20.0, 0.0f, false);
                yield hit.getLocation();
            }
            case RUNE -> caster.position().add(0, -1, 0);
            case CAST, TRACKING -> caster.getEyePosition();
            // TOUCH/TRAP are handled by their own branches in executeSpell before this is reached;
            // fall back to the caster position so the exhaustive switch stays total.
            case TOUCH, TRAP -> caster.position();
        };
    }

    public static void runBuiltinComboBehavior(Player caster, Level level, Vec3 origin, SpellCombo combo, Spell spell, float spellPower) {
        switch (combo) {
            case BLACK_HOLE -> ComboExecutors.executeBlackHole(caster, level, origin, spell, spellPower);
            case TORNADO -> ComboExecutors.executeTornado(caster, level, origin, spell, spellPower);
            case CREATIVE_FLIGHT -> ComboExecutors.executeCreativeFlight(caster, level, origin, spell, spellPower);
            case IRON_GOLEM -> ComboExecutors.executeIronGolem(caster, level, origin, spell, spellPower);
            case VEXES -> ComboExecutors.executeVexes(caster, level, origin, spell, spellPower);
            case SKELETONS -> ComboExecutors.executeSkeletons(caster, level, origin, spell, spellPower);
            case VINDICATORS -> ComboExecutors.executeVindicators(caster, level, origin, spell, spellPower);
            case VOID_RIFT -> ComboExecutors.executeVoidRift(caster, level, origin, spell, spellPower);
            case FORTRESS -> ComboExecutors.executeFortress(caster, level, origin, spell, spellPower);
            case FLOOD -> ComboExecutors.executeFlood(caster, level, origin, spell, spellPower);
            case FLOOD_LAVA -> ComboExecutors.executeFloodLava(caster, level, origin, spell, spellPower);
            case EMERGENCY_ESCAPE -> ComboExecutors.executeEmergencyEscape(caster, level, origin, spell, spellPower);
            case METEOR_STRIKE -> ComboExecutors.executeMeteorStrike(caster, level, origin, spell, spellPower);
            case BLIZZARD -> ComboExecutors.executeBlizzard(caster, level, origin, spell, spellPower);
            case LIGHTNING_STORM -> ComboExecutors.executeLightningStorm(caster, level, origin, spell, spellPower);
            case EARTHQUAKE -> ComboExecutors.executeEarthquake(caster, level, origin, spell, spellPower);
            case SANCTUARY -> ComboExecutors.executeSanctuary(caster, level, origin, spell, spellPower);
            case FIRESTORM -> ComboExecutors.executeFirestorm(caster, level, origin, spell, spellPower);
            case GEYSER -> ComboExecutors.executeGeyser(caster, level, origin, spell, spellPower);
        }
    }

    private static void executeEffectWithModifiers(Player caster, Level level, Vec3 origin, EffectGroup group, Spell originalSpell, float spellPower) {
        // Build a temporary spell with just this group's modifiers + effect
        List<SpellComponent> tempComponents = new ArrayList<>();
        for (Map.Entry<SpellModifier, Integer> entry : group.modifiers.entrySet()) {
            for (int i = 0; i < entry.getValue(); i++) {
                tempComponents.add(new SpellComponent.Modifier(entry.getKey()));
            }
        }
        if (group.effect != null) {
            tempComponents.add(new SpellComponent.Effect(group.effect));
        } else {
            tempComponents.add(new SpellComponent.DataEffect(group.dataEffectId));
        }

        Spell tempSpell = new Spell(originalSpell.getDelivery(), tempComponents);
        if (group.effect != null) {
            executeEffect(caster, level, origin, group.effect, tempSpell, spellPower);
        } else {
            executeDataEffect(caster, level, origin, group.dataEffectId, tempSpell, spellPower);
        }

        // ECHO: repeat the effect execution after a delay per echo level. The echo entity replays
        // an ECHO-stripped copy of the group in effects-only mode: stripping ECHO stops the replay
        // from scheduling further echoes (previously this recursed without bound), and effects-only
        // mode keeps the replay out of the full cast path so the stored falloff power is honored.
        int echoLevel = group.modifiers.getOrDefault(SpellModifier.ECHO, 0);
        if (echoLevel > 0 && level instanceof ServerLevel serverLevel) {
            Spell echoSpell = stripModifier(tempSpell, SpellModifier.ECHO);
            int echoDelayPer = ModConfig.modifierInt(SpellModifier.ECHO, "delayTicksPerEcho", 10);
            double echoFalloff = ModConfig.modifierDouble(SpellModifier.ECHO, "powerFalloff", 0.8);
            for (int echo = 1; echo <= echoLevel; echo++) {
                int delayTicks = echo * echoDelayPer;
                float echoPower = spellPower * (float) Math.pow(echoFalloff, echo);
                DelayedSpellEntity echoEntity = new DelayedSpellEntity(
                        serverLevel, caster, echoSpell, echoPower, delayTicks, origin, true
                );
                serverLevel.addFreshEntity(echoEntity);
            }
        }
    }

    private static void executeEffect(Player caster, Level level, Vec3 origin, SpellEffect effect, Spell spell, float spellPower) {
        // Datapack-defined behaviour wins when present; built-in effects ship as bundled
        // JSON so they normally take this path too. Falls back to hardcoded behaviour only
        // if the registry hasn't loaded (e.g. before the first datapack reload).
        buildaspell.spell.data.EffectDefinition def = buildaspell.spell.data.EffectRegistry.get(effect);
        if (def != null) {
            def.run(new buildaspell.spell.data.SpellContext(caster, level, origin, spell, spellPower));
            return;
        }
        runBuiltinEffectBehavior(caster, level, origin, effect, spell, spellPower);
    }

    /**
     * Runs a datapack-authored effect that has no backing enum constant. Resolved purely from
     * the {@link buildaspell.spell.data.EffectRegistry}; if the id is unknown (e.g. the pack that
     * defined it was removed) the effect is skipped so a saved spell never crashes the cast.
     */
    private static void executeDataEffect(Player caster, Level level, Vec3 origin, net.minecraft.resources.Identifier id, Spell spell, float spellPower) {
        buildaspell.spell.data.EffectDefinition def = buildaspell.spell.data.EffectRegistry.get(id);
        if (def != null) {
            def.run(new buildaspell.spell.data.SpellContext(caster, level, origin, spell, spellPower));
        }
    }

    public static void runBuiltinEffectBehavior(Player caster, Level level, Vec3 origin, SpellEffect effect, Spell spell, float spellPower) {
        switch (effect) {
            case DAMAGE -> EffectExecutors.executeDamage(caster, level, origin, spell, spellPower);
            case IGNITE -> EffectExecutors.executeIgnite(caster, level, origin, spell, spellPower);
            case FREEZE -> EffectExecutors.executeFreeze(caster, level, origin, spell, spellPower);
            case TELEPORT -> EffectExecutors.executeTeleport(caster, level, origin, spell, spellPower);
            case PULL -> EffectExecutors.executePull(caster, level, origin, spell, spellPower);
            case PUSH -> EffectExecutors.executePush(caster, level, origin, spell, spellPower);
            case YEET -> EffectExecutors.executeYeet(caster, level, origin, spell, spellPower);
            case REAP -> EffectExecutors.executeReap(caster, level, origin, spell, spellPower);
            case EXPLOSION -> EffectExecutors.executeExplosion(caster, level, origin, spell, spellPower);
            case HEAL -> EffectExecutors.executeHeal(caster, level, origin, spell, spellPower);
            case LIGHTNING -> EffectExecutors.executeLightning(caster, level, origin, spell, spellPower);
            case POISON -> EffectExecutors.executePoison(caster, level, origin, spell, spellPower);
            case WITHER -> EffectExecutors.executeWither(caster, level, origin, spell, spellPower);
            case SATURATION -> EffectExecutors.executeSaturation(caster, level, origin, spell, spellPower);
            case LAUNCH -> EffectExecutors.executeLaunch(caster, level, origin, spell, spellPower);
            case LIGHT -> EffectExecutors.executeLight(caster, level, origin, spell, spellPower);
            case SLAM -> EffectExecutors.executeSlam(caster, level, origin, spell, spellPower);
            case LEVITATION -> EffectExecutors.executeLevitation(caster, level, origin, spell, spellPower);
            case SLOW_FALL -> EffectExecutors.executeSlowFall(caster, level, origin, spell, spellPower);
            case BREAK -> EffectExecutors.executeBreak(caster, level, origin, spell, spellPower);
            case INVISIBILITY -> EffectExecutors.executeInvisibility(caster, level, origin, spell, spellPower);
            case SPEED -> EffectExecutors.executeSpeed(caster, level, origin, spell, spellPower);
            case HASTE -> EffectExecutors.executeHaste(caster, level, origin, spell, spellPower);
            case BLINK -> EffectExecutors.executeBlink(caster, level, origin, spell, spellPower);
            case SWAP -> EffectExecutors.executeSwap(caster, level, origin, spell, spellPower);
            case SUMMON -> EffectExecutors.executeSummon(caster, level, origin, spell, spellPower);
            case CREATE_WATER -> EffectExecutors.executeCreateWater(caster, level, origin, spell, spellPower);
            case EVAPORATE_WATER -> EffectExecutors.executeEvaporateWater(caster, level, origin, spell, spellPower);
            case MARK -> EffectExecutors.executeMark(caster, level, origin, spell, spellPower);
            case RECALL -> EffectExecutors.executeRecall(caster, level, origin, spell, spellPower);
            case PICKUP -> EffectExecutors.executePickup(caster, level, origin, spell, spellPower);
            case SHIELD -> EffectExecutors.executeShield(caster, level, origin, spell, spellPower);
            case CONJURE -> EffectExecutors.executeConjure(caster, level, origin, spell, spellPower);
            case GROWTH -> EffectExecutors.executeGrowth(caster, level, origin, spell, spellPower);
            case CLEANSE -> EffectExecutors.executeCleanse(caster, level, origin, spell, spellPower);
            case CHARM -> EffectExecutors.executeCharm(caster, level, origin, spell, spellPower);
            case BLIND -> EffectExecutors.executeBlind(caster, level, origin, spell, spellPower);
            case SLOW -> EffectExecutors.executeSlow(caster, level, origin, spell, spellPower);
            case WEAKEN -> EffectExecutors.executeWeaken(caster, level, origin, spell, spellPower);
            case STRENGTHEN -> EffectExecutors.executeStrengthen(caster, level, origin, spell, spellPower);
            case REGENERATE -> EffectExecutors.executeRegenerate(caster, level, origin, spell, spellPower);
            case RESIST -> EffectExecutors.executeResist(caster, level, origin, spell, spellPower);
            case NIGHT_VISION -> EffectExecutors.executeNightVision(caster, level, origin, spell, spellPower);
            case WATER_BREATHING -> EffectExecutors.executeWaterBreathing(caster, level, origin, spell, spellPower);
            case ROOT -> EffectExecutors.executeRoot(caster, level, origin, spell, spellPower);
            case GRAPPLE -> EffectExecutors.executeGrapple(caster, level, origin, spell, spellPower);
            case GUST -> EffectExecutors.executeGust(caster, level, origin, spell, spellPower);
        }
    }
}
