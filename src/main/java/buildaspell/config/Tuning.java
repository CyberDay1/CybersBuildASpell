package buildaspell.config;

import buildaspell.spell.DeliveryMethod;
import buildaspell.spell.SpellEffect;
import buildaspell.spell.SpellModifier;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Map;

/**
 * Per-part gameplay tuning definitions. Each value is defined inside its owner's
 * config section (the builder is already {@code push()}ed to e.g. effects.ignite)
 * and stored in the owner spec's tuning maps under "<id>.<name>". Read them back
 * via {@link ModConfig#effectInt}/{@link ModConfig#effectDouble} etc.
 *
 * Only gameplay-meaningful numbers live here; pure cosmetic particle counts and
 * sound pitches stay hardcoded in the effect/entity code.
 */
final class Tuning {
    private Tuning() {}

    private static void di(ModConfigSpec.Builder b, Map<String, ModConfigSpec.IntValue> store,
                           String prefix, String name, int def, int min, int max) {
        store.put(prefix + "." + name, b.defineInRange(name, def, min, max));
    }

    private static void dd(ModConfigSpec.Builder b, Map<String, ModConfigSpec.DoubleValue> store,
                           String prefix, String name, double def, double min, double max) {
        store.put(prefix + "." + name, b.defineInRange(name, def, min, max));
    }

    // ───────────────────────────── EFFECTS ─────────────────────────────
    static void effect(ModConfigSpec.Builder b, SpellEffect e,
                       Map<String, ModConfigSpec.DoubleValue> d,
                       Map<String, ModConfigSpec.IntValue> i) {
        String p = e.getSerializedName();
        switch (e) {
            case DAMAGE -> {
                dd(b, d, p, "spellPowerScale", 0.20, 0.0, 100.0);
                dd(b, d, p, "powerBonusPerLevel", 0.15, 0.0, 100.0);
                dd(b, d, p, "chainFalloff", 0.85, 0.0, 1.0);
                di(b, i, p, "chainMaxPerLevel", 2, 0, 64);
                dd(b, d, p, "chainSearchRadius", 5.0, 0.0, 64.0);
                di(b, i, p, "invulnTicks", 5, 0, 1000);
            }
            case IGNITE -> {
                di(b, i, p, "durationBase", 100, 0, 100000);
                di(b, i, p, "durationPerPower", 20, 0, 100000);
                di(b, i, p, "durationPerProlonged", 40, 0, 100000);
            }
            case FREEZE -> {
                di(b, i, p, "durationBase", 100, 0, 100000);
                di(b, i, p, "durationPerPower", 20, 0, 100000);
                di(b, i, p, "durationPerProlonged", 40, 0, 100000);
                di(b, i, p, "freezeOverflowCap", 100, 0, 100000);
            }
            case PULL -> {
                dd(b, d, p, "strengthBase", 0.5, 0.0, 1000.0);
                dd(b, d, p, "strengthPerPower", 0.1, 0.0, 1000.0);
            }
            case PUSH -> {
                dd(b, d, p, "strengthBase", 0.5, 0.0, 1000.0);
                dd(b, d, p, "strengthPerPower", 0.1, 0.0, 1000.0);
            }
            case YEET -> {
                dd(b, d, p, "strengthBase", 1.0, 0.0, 1000.0);
                dd(b, d, p, "strengthPerPower", 0.2, 0.0, 1000.0);
                dd(b, d, p, "spellPowerDivisor", 50.0, 0.01, 100000.0);
            }
            case EXPLOSION -> {
                dd(b, d, p, "powerBase", 2.0, 0.0, 1000.0);
                dd(b, d, p, "powerPerLevel", 0.75, 0.0, 1000.0);
                dd(b, d, p, "powerPerArea", 0.5, 0.0, 1000.0);
                di(b, i, p, "tntThreshold", 4, 0, 100);
                di(b, i, p, "blockThreshold", 2, 0, 100);
            }
            case HEAL -> {
                dd(b, d, p, "healScale", 0.3, 0.0, 100.0);
                dd(b, d, p, "undeadScale", 0.6, 0.0, 100.0);
                dd(b, d, p, "powerBonusPerLevel", 0.15, 0.0, 100.0);
                dd(b, d, p, "chainFalloff", 0.85, 0.0, 1.0);
                di(b, i, p, "chainMaxPerLevel", 2, 0, 64);
                dd(b, d, p, "chainSearchRadius", 5.0, 0.0, 64.0);
                di(b, i, p, "invulnTicks", 5, 0, 1000);
            }
            case LIGHTNING -> {
                di(b, i, p, "chainMaxPerLevel", 2, 0, 64);
                di(b, i, p, "strikeBase", 1, 0, 1000);
                di(b, i, p, "powerPerStrike", 10, 1, 1000);
                dd(b, d, p, "chainSearchRadius", 5.0, 0.0, 64.0);
            }
            case POISON -> {
                di(b, i, p, "durationBase", 80, 0, 100000);
                di(b, i, p, "durationPerPower", 40, 0, 100000);
                di(b, i, p, "durationPerProlonged", 60, 0, 100000);
                dd(b, d, p, "amplifierDivisor", 15.0, 0.01, 100000.0);
            }
            case WITHER -> {
                di(b, i, p, "durationBase", 80, 0, 100000);
                di(b, i, p, "durationPerPower", 30, 0, 100000);
                di(b, i, p, "durationPerProlonged", 50, 0, 100000);
                dd(b, d, p, "amplifierDivisor", 15.0, 0.01, 100000.0);
            }
            case SATURATION -> {
                di(b, i, p, "durationBase", 200, 0, 100000);
                di(b, i, p, "durationPerPower", 100, 0, 100000);
                di(b, i, p, "durationPerProlonged", 100, 0, 100000);
            }
            case LAUNCH -> {
                dd(b, d, p, "strengthBase", 1.0, 0.0, 1000.0);
                dd(b, d, p, "strengthPerPower", 0.3, 0.0, 1000.0);
                dd(b, d, p, "spellPowerDivisor", 50.0, 0.01, 100000.0);
            }
            case LIGHT -> {
                di(b, i, p, "lightLevelBase", 12, 0, 15);
                di(b, i, p, "lightLevelCap", 15, 0, 15);
                di(b, i, p, "durationTicks", 1200, 20, 1728000);
            }
            case SLAM -> {
                dd(b, d, p, "strengthBase", -1.5, -1000.0, 0.0);
                dd(b, d, p, "strengthPerPower", 0.4, 0.0, 1000.0);
                dd(b, d, p, "spellPowerDivisor", 50.0, 0.01, 100000.0);
            }
            case BLINK -> {
                dd(b, d, p, "distanceBase", 5.0, 0.0, 1000.0);
                dd(b, d, p, "distancePerPower", 2.0, 0.0, 1000.0);
                dd(b, d, p, "spellPowerScale", 0.1, 0.0, 100.0);
                dd(b, d, p, "distanceCap", 20.0, 0.0, 1000.0);
            }
            case SUMMON -> {
                di(b, i, p, "countBase", 1, 0, 100);
                di(b, i, p, "countCap", 5, 1, 100);
                dd(b, d, p, "spawnRadius", 2.0, 0.0, 100.0);
            }
            case CREATE_WATER -> {
                di(b, i, p, "fillRadiusBase", 25, 0, 256);
                di(b, i, p, "fillRadiusPerFill", 5, 0, 256);
                // How many blocks one Fill is allowed to touch. The radius says how far the search
                // may reach; this says how much of what it reaches the spell will actually do.
                di(b, i, p, "fillBudget", 4096, 1, 1000000);
                di(b, i, p, "chainRadiusPerChain", 3, 0, 256);
            }
            case EVAPORATE_WATER -> {
                di(b, i, p, "fillRadiusBase", 25, 0, 256);
                di(b, i, p, "fillRadiusPerFill", 5, 0, 256);
                di(b, i, p, "fillBudget", 4096, 1, 1000000);
                di(b, i, p, "chainRadiusPerChain", 3, 0, 256);
            }
            case PICKUP -> dd(b, d, p, "range", 5.0, 0.0, 256.0);
            case SHIELD -> {
                dd(b, d, p, "absorptionBase", 2.0, 0.0, 1000.0);
                dd(b, d, p, "absorptionScale", 0.15, 0.0, 100.0);
                dd(b, d, p, "absorptionCap", 20.0, 0.0, 1000.0);
            }
            case CONJURE -> {
                di(b, i, p, "expandRadiusBase", 1, 0, 256);
                di(b, i, p, "fillRadiusBase", 25, 0, 256);
                di(b, i, p, "fillRadiusPerFill", 5, 0, 256);
                di(b, i, p, "fillBudget", 4096, 1, 1000000);
            }
            case GROWTH -> {
                di(b, i, p, "ageBase", 1, 0, 100);
                di(b, i, p, "agePerPower", 1, 0, 100);
            }
            case SPEED -> di(b, i, p, "amplifierCap", 3, 0, 100);
            case HASTE -> di(b, i, p, "amplifierCap", 3, 0, 100);
            case CHARM -> {
                di(b, i, p, "durationBase", 100, 0, 100000);
                di(b, i, p, "durationPerProlonged", 40, 0, 100000);
                di(b, i, p, "durationPerPower", 40, 0, 100000);
            }
            case BLIND -> {
                di(b, i, p, "durationBase", 100, 0, 100000);
                di(b, i, p, "durationPerPower", 20, 0, 100000);
                di(b, i, p, "durationPerProlonged", 40, 0, 100000);
            }
            case BREAK -> {
                dd(b, d, p, "hardnessBase", 3.0, 0.0, 1000.0);
                dd(b, d, p, "hardnessPerPower", 2.0, 0.0, 1000.0);
                di(b, i, p, "fillRadiusBase", 25, 0, 256);
                di(b, i, p, "fillRadiusPerFill", 5, 0, 256);
                di(b, i, p, "fillBudget", 4096, 1, 1000000);
            }
            case SLOW -> {
                di(b, i, p, "durationBase", 100, 0, 100000);
                di(b, i, p, "durationPerPower", 20, 0, 100000);
                di(b, i, p, "durationPerProlonged", 40, 0, 100000);
                di(b, i, p, "amplifierCap", 3, 0, 100);
            }
            case WEAKEN -> {
                di(b, i, p, "durationBase", 120, 0, 100000);
                di(b, i, p, "durationPerPower", 20, 0, 100000);
                di(b, i, p, "durationPerProlonged", 40, 0, 100000);
                di(b, i, p, "amplifierCap", 3, 0, 100);
            }
            case STRENGTHEN -> di(b, i, p, "amplifierCap", 3, 0, 100);
            case REGENERATE -> di(b, i, p, "amplifierCap", 2, 0, 100);
            case RESIST -> di(b, i, p, "amplifierCap", 3, 0, 100);
            case ROOT -> {
                di(b, i, p, "durationBase", 80, 0, 100000);
                di(b, i, p, "durationPerPower", 20, 0, 100000);
                di(b, i, p, "durationPerProlonged", 40, 0, 100000);
                di(b, i, p, "slownessAmplifier", 6, 0, 255);
                di(b, i, p, "jumpPreventAmplifier", 128, 0, 255);
            }
            case GRAPPLE -> {
                dd(b, d, p, "strengthBase", 1.4, 0.0, 1000.0);
                dd(b, d, p, "strengthPerPower", 0.3, 0.0, 1000.0);
                dd(b, d, p, "liftBonus", 0.35, 0.0, 100.0);
            }
            case GUST -> {
                dd(b, d, p, "strengthBase", 1.2, 0.0, 1000.0);
                dd(b, d, p, "strengthPerPower", 0.3, 0.0, 1000.0);
                dd(b, d, p, "liftBonus", 0.4, 0.0, 100.0);
                dd(b, d, p, "coneMinDot", 0.3, -1.0, 1.0);
            }
            default -> { /* TELEPORT, REAP, SWAP, MARK, RECALL, CLEANSE, LEVITATION, SLOW_FALL,
                            INVISIBILITY, NIGHT_VISION, WATER_BREATHING: no per-effect knobs
                            (shared duration / cosmetic only). */ }
        }
    }

    static void sharedEffects(ModConfigSpec.Builder b,
                              Map<String, ModConfigSpec.DoubleValue> d,
                              Map<String, ModConfigSpec.IntValue> i) {
        b.comment("Duration (ticks) for area buff/debuff effects: levitation, slow_fall, invisibility, speed, haste.");
        di(b, i, "shared", "areaDurationBase", 100, 0, 100000);
        di(b, i, "shared", "areaDurationPerPower", 20, 0, 100000);
        di(b, i, "shared", "areaDurationPerProlonged", 40, 0, 100000);

        b.comment("Escalating mana price for repeating the SAME effect inside one spell. The k-th copy",
                        "of an effect costs its normal price multiplied by repeatCostGrowth^(k-1), so a wall of",
                        "identical effects gets expensive fast while ordinary mixed spells are untouched.",
                        "Each effect keeps its own count: three Damage plus three Explosion escalate as two",
                        "separate series, not one run of six. Modifiers escalate the same way under their own",
                        "key in modifiers.toml. 1.0 turns the escalation off and restores a flat per-copy price.");
        dd(b, d, "shared", "repeatCostGrowth", 1.5, 1.0, 10.0);
    }

    static void sharedModifiers(ModConfigSpec.Builder b,
                                Map<String, ModConfigSpec.DoubleValue> d,
                                Map<String, ModConfigSpec.IntValue> i) {
        b.comment("Escalating mana price for stacking the SAME modifier on one effect. The k-th stack on",
                        "a given effect costs its normal price multiplied by repeatCostGrowth^(k-1), so piling",
                        "one modifier onto one effect gets expensive fast while spreading it across a spell",
                        "does not. Each effect counts separately: Increased Power twice on Damage and twice on",
                        "Explosion is two runs of two, not one run of four. Modifiers attached to the delivery",
                        "rather than to an effect form one further group of their own. 1.0 turns the escalation",
                        "off and restores a flat per-stack price.");
        dd(b, d, "shared", "repeatCostGrowth", 1.5, 1.0, 10.0);
    }

    // ───────────────────────────── COMBOS ─────────────────────────────
    // Keys are stored as "combo.<combo>.<name>" so they don't collide with effect tuning.
    static void combos(ModConfigSpec.Builder b,
                       Map<String, ModConfigSpec.DoubleValue> d,
                       Map<String, ModConfigSpec.IntValue> i) {
        combo(b, "black_hole", () -> {
            dd(b, d, "combo.black_hole", "rangeMultiplier", 2.5, 0.0, 100.0);
            dd(b, d, "combo.black_hole", "pullStrength", 0.5, 0.0, 100.0);
            dd(b, d, "combo.black_hole", "damagePerTickScale", 0.20, 0.0, 100.0);
            dd(b, d, "combo.black_hole", "damagePerTickPerPower", 0.05, 0.0, 100.0);
            di(b, i, "combo.black_hole", "durationTicks", 90, 0, 100000);
            di(b, i, "combo.black_hole", "durationPerDuration", 30, 0, 100000);
            dd(b, d, "combo.black_hole", "pullTickScale", 0.1, 0.0, 100.0);
            di(b, i, "combo.black_hole", "damageIntervalTicks", 10, 1, 100000);
            dd(b, d, "combo.black_hole", "damageRadiusFraction", 0.35, 0.0, 100.0);
        });
        combo(b, "tornado", () -> {
            dd(b, d, "combo.tornado", "rangeMultiplier", 2.5, 0.0, 100.0);
            dd(b, d, "combo.tornado", "pullBase", 1.2, 0.0, 1000.0);
            dd(b, d, "combo.tornado", "pullPerPower", 0.3, 0.0, 1000.0);
            dd(b, d, "combo.tornado", "liftBase", 2.5, 0.0, 1000.0);
            dd(b, d, "combo.tornado", "liftPerPower", 0.5, 0.0, 1000.0);
            dd(b, d, "combo.tornado", "spinBase", 0.8, 0.0, 1000.0);
            dd(b, d, "combo.tornado", "spinPerPower", 0.2, 0.0, 1000.0);
            di(b, i, "combo.tornado", "durationBase", 100, 0, 100000);
            di(b, i, "combo.tornado", "durationPerPower", 20, 0, 100000);
            di(b, i, "combo.tornado", "durationPerDuration", 40, 0, 100000);
            di(b, i, "combo.tornado", "catchTicks", 20, 1, 100000);
            di(b, i, "combo.tornado", "damageIntervalTicks", 10, 1, 100000);
            dd(b, d, "combo.tornado", "damageScale", 0.2, 0.0, 100.0);
            dd(b, d, "combo.tornado", "damageRadiusFraction", 0.5, 0.0, 100.0);
            dd(b, d, "combo.tornado", "moveSpeed", 0.15, 0.0, 10.0);
            dd(b, d, "combo.tornado", "climbSpeed", 0.5, 0.0, 10.0);
        });
        combo(b, "creative_flight", () -> {
            di(b, i, "combo.creative_flight", "durationBase", 200, 0, 100000);
            di(b, i, "combo.creative_flight", "durationPerPower", 40, 0, 100000);
            di(b, i, "combo.creative_flight", "durationPerProlonged", 80, 0, 100000);
        });
        combo(b, "iron_golem", () -> {
            di(b, i, "combo.iron_golem", "countBase", 1, 0, 100);
            di(b, i, "combo.iron_golem", "countPerPowerDivisor", 3, 1, 1000);
            di(b, i, "combo.iron_golem", "countBonusCap", 1, 0, 100);
            dd(b, d, "combo.iron_golem", "spawnOffset", 2.5, 0.0, 100.0);
        });
        combo(b, "vexes", () -> {
            di(b, i, "combo.vexes", "countBase", 2, 0, 100);
            di(b, i, "combo.vexes", "countCap", 4, 1, 100);
            dd(b, d, "combo.vexes", "spawnOffset", 2.0, 0.0, 100.0);
            di(b, i, "combo.vexes", "lifeSeconds", 60, 0, 100000);
            di(b, i, "combo.vexes", "lifePerDuration", 30, 0, 100000);
        });
        combo(b, "skeletons", () -> {
            di(b, i, "combo.skeletons", "countBase", 2, 0, 100);
            di(b, i, "combo.skeletons", "countPerPowerDivisor", 2, 1, 1000);
            di(b, i, "combo.skeletons", "countBonusCap", 1, 0, 100);
            dd(b, d, "combo.skeletons", "spawnOffset", 2.0, 0.0, 100.0);
            di(b, i, "combo.skeletons", "lifeSeconds", 60, 0, 100000);
            di(b, i, "combo.skeletons", "lifePerDuration", 30, 0, 100000);
            // Rolled per skeleton, so a summoned host comes out mixed rather than all one kind.
            // 1.0 for an all-archer host, 0.0 for an all-swordsman one.
            dd(b, d, "combo.skeletons", "bowChance", 0.5, 0.0, 1.0);
        });
        combo(b, "vindicators", () -> {
            di(b, i, "combo.vindicators", "countBase", 2, 0, 100);
            di(b, i, "combo.vindicators", "countPerPowerDivisor", 2, 1, 1000);
            di(b, i, "combo.vindicators", "countBonusCap", 1, 0, 100);
            dd(b, d, "combo.vindicators", "spawnOffset", 2.0, 0.0, 100.0);
            di(b, i, "combo.vindicators", "lifeSeconds", 60, 0, 100000);
            di(b, i, "combo.vindicators", "lifePerDuration", 30, 0, 100000);
        });
        combo(b, "fortress", () -> {
            dd(b, d, "combo.fortress", "radiusBase", 5.0, 0.0, 256.0);
            dd(b, d, "combo.fortress", "radiusPerArea", 1.0, 0.0, 256.0);
            di(b, i, "combo.fortress", "durationBase", 100, 0, 100000);
            di(b, i, "combo.fortress", "durationPerDuration", 50, 0, 100000);
        });
        combo(b, "flood", () -> {
            di(b, i, "combo.flood", "radiusMin", 10, 0, 256);
            di(b, i, "combo.flood", "radiusMax", 15, 0, 256);
            di(b, i, "combo.flood", "blockCap", 2000, 0, 1000000);
        });
        combo(b, "flood_lava", () -> {
            di(b, i, "combo.flood_lava", "radiusMin", 10, 0, 256);
            di(b, i, "combo.flood_lava", "radiusMax", 15, 0, 256);
            di(b, i, "combo.flood_lava", "blockCap", 2000, 0, 1000000);
        });
        combo(b, "emergency_escape", () -> {
            di(b, i, "combo.emergency_escape", "maxAttempts", 50, 1, 100000);
            di(b, i, "combo.emergency_escape", "searchRadius", 1000, 1, 1000000);
        });
        combo(b, "meteor_strike", () -> {
            di(b, i, "combo.meteor_strike", "countBase", 6, 1, 1000);
            di(b, i, "combo.meteor_strike", "countRandom", 5, 0, 1000);
            di(b, i, "combo.meteor_strike", "explosionPowerBase", 4, 0, 1000);
            di(b, i, "combo.meteor_strike", "explosionPowerPerPowerCap", 2, 0, 1000);
            dd(b, d, "combo.meteor_strike", "spreadMin", 6.0, 0.0, 1000.0);
            di(b, i, "combo.meteor_strike", "spawnHeightBase", 35, 0, 1000);
            di(b, i, "combo.meteor_strike", "spawnHeightStagger", 6, 0, 1000);
            dd(b, d, "combo.meteor_strike", "fireballSpeed", 0.8, 0.0, 100.0);
        });
        combo(b, "blizzard", () -> {
            dd(b, d, "combo.blizzard", "rangeMultiplier", 2.0, 0.0, 100.0);
            di(b, i, "combo.blizzard", "durationBase", 200, 0, 100000);
            di(b, i, "combo.blizzard", "durationPerDuration", 60, 0, 100000);
            di(b, i, "combo.blizzard", "damageIntervalTicks", 20, 1, 100000);
            dd(b, d, "combo.blizzard", "damageScale", 0.15, 0.0, 100.0);
            di(b, i, "combo.blizzard", "freezeIntervalTicks", 5, 1, 100000);
            di(b, i, "combo.blizzard", "freezeCapBonus", 80, 0, 100000);
            di(b, i, "combo.blizzard", "freezePerApply", 30, 0, 100000);
            di(b, i, "combo.blizzard", "waterFreezeIntervalTicks", 10, 1, 100000);
            di(b, i, "combo.blizzard", "waterFreezeBlocks", 40, 0, 100000);
        });
        combo(b, "lightning_storm", () -> {
            dd(b, d, "combo.lightning_storm", "spreadMin", 10.0, 0.0, 1000.0);
            di(b, i, "combo.lightning_storm", "cloudBase", 2, 1, 1000);
            di(b, i, "combo.lightning_storm", "cloudsPerArea", 1, 0, 1000);
            di(b, i, "combo.lightning_storm", "durationBase", 120, 1, 100000);
            di(b, i, "combo.lightning_storm", "durationPerDuration", 80, 0, 100000);
            di(b, i, "combo.lightning_storm", "strikeIntervalTicks", 20, 1, 100000);
            dd(b, d, "combo.lightning_storm", "strikeRadius", 4.0, 0.0, 256.0);
            dd(b, d, "combo.lightning_storm", "driftSpeed", 0.03, 0.0, 10.0);
            dd(b, d, "combo.lightning_storm", "cloudHeight", 14.0, 0.0, 256.0);
        });
        combo(b, "earthquake", () -> {
            dd(b, d, "combo.earthquake", "rangeMultiplier", 1.5, 0.0, 100.0);
            dd(b, d, "combo.earthquake", "damageScale", 0.5, 0.0, 100.0);
            dd(b, d, "combo.earthquake", "launchStrength", 1.4, 0.0, 100.0);
            dd(b, d, "combo.earthquake", "pushStrength", 0.75, 0.0, 100.0);
            di(b, i, "combo.earthquake", "particleCount", 70, 0, 10000);
            dd(b, d, "combo.earthquake", "terrainRadiusFraction", 0.85, 0.0, 10.0);
            di(b, i, "combo.earthquake", "terrainBlocks", 44, 0, 10000);
            dd(b, d, "combo.earthquake", "terrainLaunch", 0.75, 0.0, 100.0);
        });
        combo(b, "sanctuary", () -> {
            dd(b, d, "combo.sanctuary", "rangeMultiplier", 1.5, 0.0, 100.0);
            di(b, i, "combo.sanctuary", "durationBase", 200, 0, 100000);
            di(b, i, "combo.sanctuary", "durationPerDuration", 100, 0, 100000);
            di(b, i, "combo.sanctuary", "amplifierCap", 2, 0, 100);
            di(b, i, "combo.sanctuary", "particleCount", 24, 0, 10000);
        });
        combo(b, "firestorm", () -> {
            dd(b, d, "combo.firestorm", "spreadMin", 9.0, 0.0, 1000.0);
            di(b, i, "combo.firestorm", "countBase", 18, 1, 1000);
            di(b, i, "combo.firestorm", "countPerArea", 8, 0, 1000);
            di(b, i, "combo.firestorm", "spawnHeight", 20, 1, 1000);
            dd(b, d, "combo.firestorm", "fireballSpeed", 0.5, 0.0, 100.0);
        });
        combo(b, "geyser", () -> {
            dd(b, d, "combo.geyser", "jetRadiusMultiplier", 0.4, 0.0, 100.0);
            dd(b, d, "combo.geyser", "jetHeight", 7.0, 0.0, 100.0);
            di(b, i, "combo.geyser", "columnParticles", 60, 0, 10000);
            di(b, i, "combo.geyser", "crownParticles", 40, 0, 10000);
            dd(b, d, "combo.geyser", "launchStrength", 2.2, 0.0, 100.0);
            dd(b, d, "combo.geyser", "launchPerPower", 0.45, 0.0, 100.0);
            dd(b, d, "combo.geyser", "damageScale", 0.3, 0.0, 100.0);
        });
    }

    private static void combo(ModConfigSpec.Builder b, String name, Runnable body) {
        b.push(name);
        body.run();
        b.pop();
    }

    // ───────────────────────────── DELIVERIES ─────────────────────────────
    static void delivery(ModConfigSpec.Builder b, DeliveryMethod m,
                         Map<String, ModConfigSpec.DoubleValue> d,
                         Map<String, ModConfigSpec.IntValue> i) {
        String p = m.getSerializedName();
        switch (m) {
            case RUNE -> {
                di(b, i, p, "castDelayTicks", 20, 1, 100000);
                di(b, i, p, "ticksPerDuration", 40, 0, 100000);
                dd(b, d, p, "reach", 30.0, 1.0, 256.0);
            }
            case CAST -> {
                di(b, i, p, "projectileLifetimeTicks", 200, 1, 100000);
                dd(b, d, p, "projectileBaseSpeed", 1.5, 0.0, 100.0);
                dd(b, d, p, "projectileSpread", 0.1, 0.0, 10.0);
            }
            case TRACKING -> {
                di(b, i, p, "projectileLifetimeTicks", 200, 1, 100000);
                dd(b, d, p, "homingRange", 16.0, 0.0, 256.0);
                // How much of the projectile's heading is given over to the target each tick: 0
                // flies straight past, 1 turns on it instantly. A fraction, so the old ceiling of
                // 100 no longer means anything and any setting above 1 is read as 1.
                dd(b, d, p, "homingStrength", 0.3, 0.0, 1.0);
            }
            case TOUCH -> di(b, i, p, "durationTicks", 200, 1, 100000);
            case TRAP -> {
                di(b, i, p, "armingTicks", 20, 1, 100000);
                di(b, i, p, "lifetimeTicks", 1200, 1, 100000);
                di(b, i, p, "lifetimePerDuration", 600, 0, 100000);
                dd(b, d, p, "triggerRadius", 2.5, 0.5, 64.0);
                dd(b, d, p, "reach", 30.0, 1.0, 256.0);
            }
            default -> { /* RUNE, SIGHT, SELF: no projectile-flight knobs. */ }
        }
    }

    // ───────────────────────────── MODIFIERS ─────────────────────────────
    static void modifier(ModConfigSpec.Builder b, SpellModifier m,
                         Map<String, ModConfigSpec.DoubleValue> d,
                         Map<String, ModConfigSpec.IntValue> i) {
        String p = m.getSerializedName();
        switch (m) {
            case BOUNCE -> di(b, i, p, "bounceCount", 3, 0, 1000);
            case CHAIN -> {
                // Chain is now a projectile-level block-hop: on a terminal block hit the projectile
                // hops to nearby blocks of the same Block type, resolving the full effect group at
                // each. hopsPerLevel = extra hops granted per Chain stack; searchRadius = block-search
                // radius per hop. No damage falloff — each hop resolves the full spell.
                di(b, i, p, "hopsPerLevel", 1, 0, 1000);
                di(b, i, p, "searchRadius", 4, 1, 64);
            }
            case INCREASED_AREA -> dd(b, d, p, "rangePerStack", 1.0, 0.0, 256.0);
            // Caps the effective Fortune/Looting level a stack of Fortunate Son can reach. This is a
            // backstop, not the balance lever: the escalating repeat cost is what actually limits a
            // stack. Each copy costs growth^(k-1) times the last, so against the Mana Pool
            // attribute's own ceiling of 10000 a caster tops out around 13 stacks bare-handed and 14
            // with a Runic wand - well inside this cap, which exists only so the number stays finite.
            case FORTUNATE_SON -> di(b, i, p, "maxLevel", 25, 0, 100);
            case DELAY -> di(b, i, p, "ticksPerStack", 10, 0, 100000);
            case ACCELERATE -> dd(b, d, p, "speedPerStack", 0.5, 0.0, 100.0);
            case ECHO -> {
                di(b, i, p, "delayTicksPerEcho", 10, 0, 100000);
                dd(b, d, p, "powerFalloff", 0.8, 0.0, 1.0);
                // An echo re-casts the whole spell, so it is charged as a share of the whole spell
                // rather than a flat price: the shipped 1.8 is 80% more. Stacks compound, so two
                // echoes cost 1.8 x 1.8. 1.0 charges nothing for it, in which case set the
                // modifier's own baseManaCost to put it back on a fixed price.
                dd(b, d, p, "totalCostMultiplier", m.getTotalCostMultiplier(), 1.0, 100.0);
            }
            // Double sends a second projectile, doubling everything the spell does, so like Echo it
            // is priced as a share of the whole spell. See ECHO above for how the number behaves.
            case DOUBLE -> dd(b, d, p, "totalCostMultiplier", m.getTotalCostMultiplier(), 1.0, 100.0);
            case LEECH -> dd(b, d, p, "healFractionPerLevel", 0.25, 0.0, 100.0);
            case SUNDER -> {
                // Sunder multiplies the spell's damage rather than adding a figure of its own, so it
                // rises and falls with Spell Power like everything else. A target in full diamond is
                // "fully armored" and gets the whole fraction; half that armor gets half of it; an
                // unarmored target gets none. maxMultiplier is what several copies on one effect run
                // into, and is the reason stacking cannot run away.
                dd(b, d, p, "bonusFractionPerLevel", 0.35, 0.0, 100.0);
                dd(b, d, p, "maxMultiplier", 3.0, 1.0, 100.0);
                dd(b, d, p, "fullArmorValue", 20.0, 1.0, 1000.0);
            }
            case RETURN -> {
                dd(b, d, p, "catchRadius", 2.0, 0.1, 100.0);
                dd(b, d, p, "minSpeed", 0.4, 0.0, 100.0);
            }
            case LINGER -> {
                di(b, i, p, "durationBase", 100, 0, 100000);
                di(b, i, p, "durationPerDuration", 60, 0, 100000);
                di(b, i, p, "pulseIntervalTicks", 5, 1, 100000);
            }
            default -> { /* remaining modifiers scale per-stack; magnitudes live in Spell.java getters. */ }
        }
    }
}
