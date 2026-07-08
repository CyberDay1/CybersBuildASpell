package buildaspell.config;

import buildaspell.item.WandTier;
import buildaspell.spell.DeliveryMethod;
import buildaspell.spell.SpellEffect;
import buildaspell.spell.SpellModifier;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.*;

/**
 * Config is split across a {@code buildaspell/} folder so that client-only
 * settings live apart from the server-authoritative spell tuning, and so server
 * owners / modpack makers can drop category overrides into
 * {@code defaultconfigs/buildaspell/}.
 *
 * All SERVER specs auto-sync to clients on login (NeoForge ConfigSync), so the
 * builder palette and cost displays reflect the server's values after sync.
 */
public class ModConfig {

    // ---- SERVER: general tuning -> buildaspell/general.toml ----
    public static class General {
        public final ModConfigSpec.DoubleValue globalManaCostMultiplier;
        public final ModConfigSpec.DoubleValue spellPowerPerLevel;
        public final ModConfigSpec.IntValue manaPoolMaxLevel;
        public final ModConfigSpec.IntValue manaRegenMaxLevel;
        public final ModConfigSpec.IntValue spellPowerMaxLevel;
        public final ModConfigSpec.DoubleValue spellBaseRange;
        public final ModConfigSpec.IntValue maxPortalsPerPlayer;
        public final ModConfigSpec.DoubleValue portalMinSize;
        public final ModConfigSpec.DoubleValue portalMaxSize;
        public final ModConfigSpec.ConfigValue<List<? extends String>> conjureAllowedBlocks;
        public final ModConfigSpec.DoubleValue particleDensity;
        public final ModConfigSpec.BooleanValue lightningTransmutesBlocks;
        public final ModConfigSpec.IntValue essenceRequired;
        public final ModConfigSpec.IntValue killEssence;
        public final ModConfigSpec.IntValue bossEssence;
        public final ModConfigSpec.DoubleValue castEssenceRatio;
        public final ModConfigSpec.ConfigValue<List<? extends String>> starterDeliveries;
        public final ModConfigSpec.ConfigValue<List<? extends String>> starterEffects;
        public final ModConfigSpec.BooleanValue giveStarterGuidebook;

        public General(ModConfigSpec.Builder builder) {
            builder.push("general");
            globalManaCostMultiplier = builder.comment("Global mana cost multiplier")
                    .defineInRange("globalManaCostMultiplier", 1.0, 0.0, 10.0);
            spellPowerPerLevel = builder.comment("Spell power gained per enchantment level")
                    .defineInRange("spellPowerPerLevel", 5.0, 0.0, 50.0);
            manaPoolMaxLevel = builder.comment(
                            "Cap on the effective level of the Mana Pool enchantment, per item.",
                            "Levels above this are ignored when computing the mana bonus (the enchantment can",
                            "still appear at a higher level, it just stops scaling). Mostly for mod pack devs",
                            "who want to rein in the buildaspell enchantments. 255 = no cap (vanilla max level).")
                    .defineInRange("manaPoolMaxLevel", 255, 0, 255);
            manaRegenMaxLevel = builder.comment(
                            "Cap on the effective level of the Mana Regeneration enchantment, per item.",
                            "255 = no cap (vanilla max level).")
                    .defineInRange("manaRegenMaxLevel", 255, 0, 255);
            spellPowerMaxLevel = builder.comment(
                            "Cap on the effective level of the Spell Power enchantment, per item.",
                            "255 = no cap (vanilla max level).")
                    .defineInRange("spellPowerMaxLevel", 255, 0, 255);
            spellBaseRange = builder.comment("Base spell range/area radius before Increased Area modifiers")
                    .defineInRange("spellBaseRange", 5.0, 0.0, 256.0);
            maxPortalsPerPlayer = builder.comment("Max portals per player (0 = unlimited)")
                    .defineInRange("maxPortalsPerPlayer", 0, 0, 1000);
            portalMinSize = builder.comment("Minimum portal width/height")
                    .defineInRange("portalMinSize", 1.0, 0.5, 10.0);
            portalMaxSize = builder.comment("Maximum portal width/height")
                    .defineInRange("portalMaxSize", 10.0, 1.0, 20.0);
            conjureAllowedBlocks = builder.comment("Blocks allowed for Conjure effect")
                    .defineList("conjureAllowedBlocks",
                            List.of("stone", "cobblestone", "blackstone", "dirt", "grass_block"),
                            obj -> obj instanceof String);
            particleDensity = builder.comment(
                            "Density of the big combo-spell particle effects (tornado, blizzard, black hole).",
                            "1.0 = full visuals (default look); lower values thin the particles to reduce client",
                            "FPS cost and network traffic on busy servers; 0.0 disables those particles entirely.")
                    .defineInRange("particleDensity", 1.0, 0.0, 1.0);
            lightningTransmutesBlocks = builder.comment(
                            "Whether spell-cast lightning (the Lightning effect and the Lightning Storm combo)",
                            "runs buildaspell's custom block transmutations where a bolt lands: sand and red sand",
                            "fuse into glass (fulgurite). Vanilla bolt side effects (fire, mob conversions, copper",
                            "de-oxidising) always apply regardless. Nullified strikes never transmute blocks.")
                    .define("lightningTransmutesBlocks", true);
            builder.pop();

            builder.comment("Rune progression: how Blank Runes gather essence into Spell Runes,",
                            "and the components every player starts with already unlocked.")
                    .push("progression");
            essenceRequired = builder.comment("Essence a Blank Rune must gather before it becomes a Spell Rune")
                    .defineInRange("essenceRequired", 200, 1, 100000);
            killEssence = builder.comment("Essence granted per regular hostile mob killed")
                    .defineInRange("killEssence", 1, 0, 100000);
            bossEssence = builder.comment("Essence granted per boss killed (Ender Dragon, Wither, rare monsters)")
                    .defineInRange("bossEssence", 50, 0, 100000);
            castEssenceRatio = builder.comment("Essence granted per point of mana actually spent casting a spell (0 disables cast progression)")
                    .defineInRange("castEssenceRatio", 0.25, 0.0, 100.0);
            starterDeliveries = builder.comment("Delivery method ids every player begins with unlocked")
                    .defineList("starterDeliveries", List.of("cast"), obj -> obj instanceof String);
            starterEffects = builder.comment("Effect ids every player begins with unlocked")
                    .defineList("starterEffects", List.of("damage", "break"), obj -> obj instanceof String);
            giveStarterGuidebook = builder.comment("Give new players the Arcane Codex guidebook on first join (requires Modonomicon installed)")
                    .define("giveStarterGuidebook", true);
            builder.pop();
        }
    }

    // ---- SERVER: delivery methods -> buildaspell/deliveries.toml ----
    public static class Deliveries {
        public final Map<DeliveryMethod, ModConfigSpec.BooleanValue> enabled = new EnumMap<>(DeliveryMethod.class);
        public final Map<DeliveryMethod, ModConfigSpec.DoubleValue> baseManaCost = new EnumMap<>(DeliveryMethod.class);
        public final Map<DeliveryMethod, ModConfigSpec.DoubleValue> costMultiplier = new EnumMap<>(DeliveryMethod.class);
        // Free-form per-delivery gameplay tuning (see Tuning.* keys).
        public final Map<String, ModConfigSpec.DoubleValue> tuneD = new HashMap<>();
        public final Map<String, ModConfigSpec.IntValue> tuneI = new HashMap<>();

        public Deliveries(ModConfigSpec.Builder builder) {
            builder.comment("Disabling a delivery method hides it everywhere: builder palette, fill runes, and casting.");
            builder.push("delivery_methods");
            for (DeliveryMethod method : DeliveryMethod.values()) {
                builder.push(method.getSerializedName());
                enabled.put(method, builder.define("enabled", true));
                baseManaCost.put(method, builder.comment("Base mana cost before multipliers")
                        .defineInRange("baseManaCost", method.getBaseCost(), 0.0, 100000.0));
                costMultiplier.put(method, builder.defineInRange("costMultiplier", 1.0, 0.0, 10.0));
                Tuning.delivery(builder, method, tuneD, tuneI);
                builder.pop();
            }
            builder.pop();
        }
    }

    // ---- SERVER: effects -> buildaspell/effects.toml ----
    public static class Effects {
        public final Map<SpellEffect, ModConfigSpec.BooleanValue> enabled = new EnumMap<>(SpellEffect.class);
        public final Map<SpellEffect, ModConfigSpec.DoubleValue> baseManaCost = new EnumMap<>(SpellEffect.class);
        public final Map<SpellEffect, ModConfigSpec.DoubleValue> costMultiplier = new EnumMap<>(SpellEffect.class);
        public final Map<SpellEffect, ModConfigSpec.DoubleValue> damageMultiplier = new EnumMap<>(SpellEffect.class);
        // Free-form per-effect gameplay tuning, keyed "<effectid>.<name>" (see Tuning.effect).
        public final Map<String, ModConfigSpec.DoubleValue> tuneD = new HashMap<>();
        public final Map<String, ModConfigSpec.IntValue> tuneI = new HashMap<>();

        public Effects(ModConfigSpec.Builder builder) {
            builder.comment("Disabling an effect hides it everywhere: builder palette, fill runes, and casting.");
            builder.push("effects");
            for (SpellEffect effect : SpellEffect.values()) {
                builder.push(effect.getSerializedName());
                enabled.put(effect, builder.define("enabled", true));
                baseManaCost.put(effect, builder.comment("Base mana cost before multipliers")
                        .defineInRange("baseManaCost", effect.getBaseCost(), 0.0, 100000.0));
                costMultiplier.put(effect, builder.defineInRange("costMultiplier", 1.0, 0.0, 10.0));
                damageMultiplier.put(effect, builder.defineInRange("damageMultiplier", 1.0, 0.0, 10.0));
                Tuning.effect(builder, effect, tuneD, tuneI);
                builder.pop();
            }
            builder.pop();

            builder.comment("Shared knobs used by several area buff/debuff effects.").push("shared");
            Tuning.sharedEffects(builder, tuneD, tuneI);
            builder.pop();

            builder.comment("Combo spells (multi-part behaviours: meteor, tornado, blizzard, fortress, summons, etc.).").push("combos");
            Tuning.combos(builder, tuneD, tuneI);
            builder.pop();
        }
    }

    // ---- SERVER: modifiers -> buildaspell/modifiers.toml ----
    public static class Modifiers {
        public final Map<SpellModifier, ModConfigSpec.BooleanValue> enabled = new EnumMap<>(SpellModifier.class);
        public final Map<SpellModifier, ModConfigSpec.DoubleValue> baseManaCost = new EnumMap<>(SpellModifier.class);
        public final Map<SpellModifier, ModConfigSpec.DoubleValue> costMultiplier = new EnumMap<>(SpellModifier.class);
        // Free-form per-modifier gameplay tuning, keyed "<modifierid>.<name>" (see Tuning.modifier).
        public final Map<String, ModConfigSpec.DoubleValue> tuneD = new HashMap<>();
        public final Map<String, ModConfigSpec.IntValue> tuneI = new HashMap<>();

        public Modifiers(ModConfigSpec.Builder builder) {
            builder.comment("Disabling a modifier hides it everywhere: builder palette, fill runes, and casting.");
            builder.push("modifiers");
            for (SpellModifier modifier : SpellModifier.values()) {
                builder.push(modifier.getSerializedName());
                enabled.put(modifier, builder.define("enabled", true));
                baseManaCost.put(modifier, builder.comment("Base mana cost (per stack) before multipliers")
                        .defineInRange("baseManaCost", modifier.getBaseCost(), 0.0, 100000.0));
                costMultiplier.put(modifier, builder.defineInRange("costMultiplier", 1.0, 0.0, 10.0));
                Tuning.modifier(builder, modifier, tuneD, tuneI);
                builder.pop();
            }
            builder.pop();
        }
    }

    // ---- SERVER: wands -> buildaspell/wands.toml ----
    public static class Wands {
        public final Map<WandTier, ModConfigSpec.DoubleValue> manaReduction = new EnumMap<>(WandTier.class);
        public final Map<WandTier, ModConfigSpec.DoubleValue> spellPower = new EnumMap<>(WandTier.class);

        public Wands(ModConfigSpec.Builder builder) {
            builder.comment("Three wand tiers. While a wand is held it discounts the mana cost of every spell",
                            "you cast and grants bonus Spell Power; right-clicking casts your selected spell.",
                            "Worn is the cheap starter; Carved and Runic grant progressively more Spell Power.",
                            "Holding two wands never stacks — only the strongest tier applies.")
                    .push("wands");
            define(builder, WandTier.WORN, 0.10, 10.0);
            define(builder, WandTier.CARVED, 0.25, 25.0);
            define(builder, WandTier.RUNIC, 0.40, 75.0);
            builder.pop();
        }

        private void define(ModConfigSpec.Builder b, WandTier tier, double reductionDef, double powerDef) {
            b.push(tier.getSerializedName());
            manaReduction.put(tier, b.comment("Fraction of mana cost removed while holding this wand (0.25 = 25% cheaper)")
                    .defineInRange("manaReduction", reductionDef, 0.0, 1.0));
            spellPower.put(tier, b.comment("Bonus Spell Power granted while holding this wand")
                    .defineInRange("spellPower", powerDef, 0.0, 10000.0));
            b.pop();
        }
    }

    // ---- CLIENT -> buildaspell/client.toml ----
    public static class Client {
        public final ModConfigSpec.DoubleValue abilityRingTransparency;

        public Client(ModConfigSpec.Builder builder) {
            builder.push("gui");
            abilityRingTransparency = builder.comment("Ability ring background transparency")
                    .defineInRange("abilityRingTransparency", 0.7, 0.0, 1.0);
            builder.pop();
        }
    }

    // Static config instances
    private static General GENERAL;
    private static Deliveries DELIVERIES;
    private static Effects EFFECTS;
    private static Modifiers MODIFIERS;
    private static Wands WANDS;
    private static Client CLIENT;

    private static ModConfigSpec GENERAL_SPEC;
    private static ModConfigSpec DELIVERIES_SPEC;
    private static ModConfigSpec EFFECTS_SPEC;
    private static ModConfigSpec MODIFIERS_SPEC;
    private static ModConfigSpec WANDS_SPEC;
    private static ModConfigSpec CLIENT_SPEC;

    public static void init() {
        var generalPair = new ModConfigSpec.Builder().configure(General::new);
        GENERAL_SPEC = generalPair.getRight();
        GENERAL = generalPair.getLeft();

        var deliveriesPair = new ModConfigSpec.Builder().configure(Deliveries::new);
        DELIVERIES_SPEC = deliveriesPair.getRight();
        DELIVERIES = deliveriesPair.getLeft();

        var effectsPair = new ModConfigSpec.Builder().configure(Effects::new);
        EFFECTS_SPEC = effectsPair.getRight();
        EFFECTS = effectsPair.getLeft();

        var modifiersPair = new ModConfigSpec.Builder().configure(Modifiers::new);
        MODIFIERS_SPEC = modifiersPair.getRight();
        MODIFIERS = modifiersPair.getLeft();

        var wandsPair = new ModConfigSpec.Builder().configure(Wands::new);
        WANDS_SPEC = wandsPair.getRight();
        WANDS = wandsPair.getLeft();

        var clientPair = new ModConfigSpec.Builder().configure(Client::new);
        CLIENT_SPEC = clientPair.getRight();
        CLIENT = clientPair.getLeft();
    }

    public static ModConfigSpec getGeneralSpec() { return GENERAL_SPEC; }
    public static ModConfigSpec getDeliveriesSpec() { return DELIVERIES_SPEC; }
    public static ModConfigSpec getEffectsSpec() { return EFFECTS_SPEC; }
    public static ModConfigSpec getModifiersSpec() { return MODIFIERS_SPEC; }
    public static ModConfigSpec getWandsSpec() { return WANDS_SPEC; }
    public static ModConfigSpec getClientSpec() { return CLIENT_SPEC; }
    public static Client client() { return CLIENT; }

    // Static helper methods
    public static boolean isDeliveryEnabled(DeliveryMethod method) {
        return DELIVERIES != null && DELIVERIES.enabled.get(method).get();
    }

    public static boolean isEffectEnabled(SpellEffect effect) {
        return EFFECTS != null && EFFECTS.enabled.get(effect).get();
    }

    public static boolean isModifierEnabled(SpellModifier modifier) {
        return MODIFIERS != null && MODIFIERS.enabled.get(modifier).get();
    }

    public static float getDeliveryCost(DeliveryMethod method) {
        if (DELIVERIES == null || GENERAL == null) return method.getBaseCost();
        return (float) (DELIVERIES.baseManaCost.get(method).get()
                * DELIVERIES.costMultiplier.get(method).get()
                * GENERAL.globalManaCostMultiplier.get());
    }

    public static float getEffectCost(SpellEffect effect) {
        if (EFFECTS == null || GENERAL == null) return effect.getBaseCost();
        return (float) (EFFECTS.baseManaCost.get(effect).get()
                * EFFECTS.costMultiplier.get(effect).get()
                * GENERAL.globalManaCostMultiplier.get());
    }

    public static float getModifierCost(SpellModifier modifier, int count) {
        if (MODIFIERS == null || GENERAL == null) return modifier.getBaseCost() * count;
        return (float) (MODIFIERS.baseManaCost.get(modifier).get() * count
                * MODIFIERS.costMultiplier.get(modifier).get()
                * GENERAL.globalManaCostMultiplier.get());
    }

    // ---- Gameplay tuning accessors ----
    // Call sites pass the original literal as the fallback (used only before config load,
    // which never happens at spell-cast time). The configured value wins once loaded.
    public static int effectInt(SpellEffect effect, String name, int fallback) {
        if (EFFECTS == null) return fallback;
        var v = EFFECTS.tuneI.get(effect.getSerializedName() + "." + name);
        return v != null ? v.get() : fallback;
    }

    public static double effectDouble(SpellEffect effect, String name, double fallback) {
        if (EFFECTS == null) return fallback;
        var v = EFFECTS.tuneD.get(effect.getSerializedName() + "." + name);
        return v != null ? v.get() : fallback;
    }

    public static int sharedEffectInt(String name, int fallback) {
        if (EFFECTS == null) return fallback;
        var v = EFFECTS.tuneI.get("shared." + name);
        return v != null ? v.get() : fallback;
    }

    public static double sharedEffectDouble(String name, double fallback) {
        if (EFFECTS == null) return fallback;
        var v = EFFECTS.tuneD.get("shared." + name);
        return v != null ? v.get() : fallback;
    }

    public static int deliveryInt(DeliveryMethod method, String name, int fallback) {
        if (DELIVERIES == null) return fallback;
        var v = DELIVERIES.tuneI.get(method.getSerializedName() + "." + name);
        return v != null ? v.get() : fallback;
    }

    public static double deliveryDouble(DeliveryMethod method, String name, double fallback) {
        if (DELIVERIES == null) return fallback;
        var v = DELIVERIES.tuneD.get(method.getSerializedName() + "." + name);
        return v != null ? v.get() : fallback;
    }

    public static int modifierInt(SpellModifier modifier, String name, int fallback) {
        if (MODIFIERS == null) return fallback;
        var v = MODIFIERS.tuneI.get(modifier.getSerializedName() + "." + name);
        return v != null ? v.get() : fallback;
    }

    public static double modifierDouble(SpellModifier modifier, String name, double fallback) {
        if (MODIFIERS == null) return fallback;
        var v = MODIFIERS.tuneD.get(modifier.getSerializedName() + "." + name);
        return v != null ? v.get() : fallback;
    }

    // Combo spells live in the Effects spec under the "combos" section, keyed "combo.<combo>.<name>".
    public static int comboInt(String combo, String name, int fallback) {
        if (EFFECTS == null) return fallback;
        var v = EFFECTS.tuneI.get("combo." + combo + "." + name);
        return v != null ? v.get() : fallback;
    }

    public static double comboDouble(String combo, String name, double fallback) {
        if (EFFECTS == null) return fallback;
        var v = EFFECTS.tuneD.get("combo." + combo + "." + name);
        return v != null ? v.get() : fallback;
    }

    public static float getEffectDamageMultiplier(SpellEffect effect) {
        if (EFFECTS == null) return 1.0f;
        return EFFECTS.damageMultiplier.get(effect).get().floatValue();
    }

    public static double getSpellPowerPerLevel() {
        return GENERAL != null ? GENERAL.spellPowerPerLevel.get() : 5.0;
    }

    public static boolean lightningTransmutesBlocks() {
        return GENERAL == null || GENERAL.lightningTransmutesBlocks.get();
    }

    public static int getManaPoolMaxLevel() {
        return GENERAL != null ? GENERAL.manaPoolMaxLevel.get() : 255;
    }

    public static int getManaRegenMaxLevel() {
        return GENERAL != null ? GENERAL.manaRegenMaxLevel.get() : 255;
    }

    public static int getSpellPowerMaxLevel() {
        return GENERAL != null ? GENERAL.spellPowerMaxLevel.get() : 255;
    }

    public static double getSpellBaseRange() {
        return GENERAL != null ? GENERAL.spellBaseRange.get() : 5.0;
    }

    public static int getMaxPortalsPerPlayer() {
        return GENERAL != null ? GENERAL.maxPortalsPerPlayer.get() : 0;
    }

    public static List<? extends String> getConjureAllowedBlocks() {
        return GENERAL != null ? GENERAL.conjureAllowedBlocks.get()
                : List.of("stone", "cobblestone", "blackstone", "dirt", "grass_block");
    }

    public static double getPortalMinSize() {
        return GENERAL != null ? GENERAL.portalMinSize.get() : 1.0;
    }

    public static double getPortalMaxSize() {
        return GENERAL != null ? GENERAL.portalMaxSize.get() : 10.0;
    }

    public static double getParticleDensity() {
        return GENERAL != null ? GENERAL.particleDensity.get() : 1.0;
    }

    /**
     * Scale a particle batch count by the server's particleDensity. Default 1.0 returns the
     * base unchanged (current look). A positive-but-reduced density never thins a positive batch
     * all the way to nothing — it keeps at least one particle so the effect stays visible; only
     * density 0.0 turns the batch off entirely.
     */
    public static int scaledParticleCount(int base) {
        if (GENERAL == null) return base;
        double d = GENERAL.particleDensity.get();
        if (d >= 1.0) return base;
        if (d <= 0.0) return 0;
        int scaled = (int) Math.round(base * d);
        return (base > 0 && scaled < 1) ? 1 : scaled;
    }

    // ---- Rune progression accessors ----
    public static int getEssenceRequired() {
        return GENERAL != null ? GENERAL.essenceRequired.get() : 200;
    }

    public static int getKillEssence() {
        return GENERAL != null ? GENERAL.killEssence.get() : 1;
    }

    public static int getBossEssence() {
        return GENERAL != null ? GENERAL.bossEssence.get() : 50;
    }

    public static double getCastEssenceRatio() {
        return GENERAL != null ? GENERAL.castEssenceRatio.get() : 0.25;
    }

    public static List<? extends String> getStarterDeliveries() {
        return GENERAL != null ? GENERAL.starterDeliveries.get() : List.of("cast");
    }

    public static List<? extends String> getStarterEffects() {
        return GENERAL != null ? GENERAL.starterEffects.get() : List.of("damage", "break");
    }

    public static boolean giveStarterGuidebook() {
        return GENERAL == null || GENERAL.giveStarterGuidebook.get();
    }

    // ---- Wand accessors ----
    // Fallbacks mirror the spec defaults so held-wand bonuses still apply before config load.
    public static double wandManaReduction(WandTier tier) {
        if (WANDS != null) {
            var v = WANDS.manaReduction.get(tier);
            if (v != null) return v.get();
        }
        return switch (tier) {
            case WORN -> 0.10;
            case CARVED -> 0.25;
            case RUNIC -> 0.40;
        };
    }

    public static double wandSpellPower(WandTier tier) {
        if (WANDS != null) {
            var v = WANDS.spellPower.get(tier);
            if (v != null) return v.get();
        }
        return switch (tier) {
            case WORN -> 10.0;
            case CARVED -> 25.0;
            case RUNIC -> 75.0;
        };
    }
}
