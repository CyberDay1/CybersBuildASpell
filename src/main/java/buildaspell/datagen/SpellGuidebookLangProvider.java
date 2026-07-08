package buildaspell.datagen;

import com.klikli_dev.modonomicon.api.datagen.AbstractModonomiconLanguageProvider;
import com.klikli_dev.modonomicon.api.datagen.ModonomiconLanguageProvider;
import buildaspell.spell.DeliveryMethod;
import buildaspell.spell.SpellEffect;
import buildaspell.spell.SpellModifier;
import net.minecraft.data.PackOutput;

import static buildaspell.BuildASpell.MOD_ID;

public class SpellGuidebookLangProvider extends AbstractModonomiconLanguageProvider {

    public SpellGuidebookLangProvider(PackOutput output, ModonomiconLanguageProvider cachedProvider) {
        super(output, MOD_ID, "en_us", cachedProvider);
    }

    @Override
    protected void addTranslations() {
        // Keybinds
        add("key.categories.buildaspell", "Build a Spell");
        add("key.buildaspell.ability_ring", "Open Ability Ring");
        add("key.buildaspell.cast_spell", "Cast Spell");
        add("key.buildaspell.spell_builder", "Open Spell Builder");
        for (int i = 1; i <= 5; i++) {
            add("key.buildaspell.spell_slot_" + i, "Cast Spell " + i);
        }
        add("key.buildaspell.next_spell_slot", "Next Spell Slot");
        add("key.buildaspell.prev_spell_slot", "Previous Spell Slot");
        add("key.buildaspell.import_spell", "Import Spell");
        add("key.buildaspell.toggle_mana_bar", "Toggle Mana Bar");

        // Enchantments
        add("enchantment.buildaspell.mana_pool", "Mana Pool");
        add("enchantment.buildaspell.mana_regeneration", "Mana Regeneration");
        add("enchantment.buildaspell.spell_power", "Spell Power");

        // Attributes
        add("attribute.buildaspell.mana_pool", "Mana Pool");
        add("attribute.buildaspell.mana_regen", "Mana Regen");
        add("attribute.buildaspell.spell_power", "Spell Power");

        // Items
        add("item.buildaspell.spell_rune", "Spell Rune");
        add("item.buildaspell.blank_rune", "Blank Rune");
        add("item.buildaspell.worn_wand", "Worn Wand");
        add("item.buildaspell.carved_wand", "Carved Wand");
        add("item.buildaspell.runic_wand", "Runic Wand");
        add("item.buildaspell.wand.no_active_spell", "No spell selected — pick one in the Ability Ring first.");
        add("item.buildaspell.wand.tooltip.cast", "Right-click to cast your selected spell");
        add("item.buildaspell.wand.tooltip.mana", "Reduces spell mana cost by %s%%");
        add("item.buildaspell.wand.tooltip.power", "+%s Spell Power while held");

        // Blocks
        add("block.buildaspell.arcane_altar", "Arcane Altar");
        add("container.buildaspell.arcane_altar", "Arcane Altar");

        // Creative tab
        add("itemGroup.buildaspell", "Build a Spell");

        // --- GUI: Spell Builder ---
        add("gui.buildaspell.spell_builder.title", "Spell Builder");
        add("gui.buildaspell.spell_builder.spell_name", "Spell Name");
        add("gui.buildaspell.spell_builder.name_hint", "Spell name...");
        add("gui.buildaspell.spell_builder.search", "Search");
        add("gui.buildaspell.spell_builder.search_hint", "Search components...");
        add("gui.buildaspell.save", "Save");
        add("gui.buildaspell.clear", "Clear");
        add("gui.buildaspell.load", "Load");
        add("gui.buildaspell.export", "Export");
        add("gui.buildaspell.import", "Import");
        add("gui.buildaspell.copy", "Copy");
        add("gui.buildaspell.spell_builder.no_results", "No results");
        add("gui.buildaspell.spell_builder.unnamed_spell", "Unnamed");
        add("gui.buildaspell.spell_builder.empty_slot", "Empty");
        add("gui.buildaspell.spell_builder.mana_format", "%s mana");
        add("gui.buildaspell.spell_builder.mana_cost", "Mana Cost: %s");
        add("gui.buildaspell.spell_builder.range", "Range: %s blocks");
        add("gui.buildaspell.spell_builder.loading_slot", "Loading spell from slot %s...");
        add("gui.buildaspell.spell_builder.spell_loaded", "Spell loaded successfully!");
        add("gui.buildaspell.spell_builder.slot_empty", "Slot %s is empty!");
        add("gui.buildaspell.spell_builder.require_delivery", "Please add a delivery method!");
        add("gui.buildaspell.spell_builder.require_effect", "Please add at least one effect!");
        add("gui.buildaspell.spell_builder.modifier_no_effect", "%s has no effect on %s — not added.");
        add("gui.buildaspell.spell_builder.spell_saved", "Spell saved to slot %s!");
        add("gui.buildaspell.spell_builder.no_export", "No spell to export!");
        add("gui.buildaspell.spell_builder.exported", "Spell exported:");
        add("gui.buildaspell.spell_builder.invalid_code", "Invalid spell code!");
        add("gui.buildaspell.spell_builder.imported", "Spell imported successfully!");
        add("gui.buildaspell.spell_builder.no_copy", "No spell to copy!");
        add("gui.buildaspell.spell_builder.copied", "Spell code copied to clipboard!");
        add("gui.buildaspell.spell_builder.to_power", "Power");
        add("gui.buildaspell.spell_builder.no_power", "No castable spell to export!");
        add("gui.buildaspell.spell_builder.power_copied", "NeoOrigins power copied to clipboard!");
        add("gui.buildaspell.spell_builder.delivery", "Delivery");
        add("gui.buildaspell.spell_builder.add_effect", "Add Effect");
        add("gui.buildaspell.spell_builder.slots_header", "Saved Spells");
        add("gui.buildaspell.spell_builder.palette_delivery", "Delivery Methods");
        add("gui.buildaspell.spell_builder.palette_effects", "Effects");
        add("gui.buildaspell.spell_builder.palette_modifiers", "Modifiers");

        // --- GUI: Projectile Visuals ---
        add("gui.buildaspell.spell_builder.visuals", "Visuals");
        add("gui.buildaspell.spell_builder.visuals_done", "Done");
        add("gui.buildaspell.spell_builder.visuals_reset", "Reset");
        add("gui.buildaspell.spell_builder.visuals_note", "Applies to projectile spells (Cast / Tracking)");
        add("gui.buildaspell.spell_builder.visuals_color", "Color: %s");
        add("gui.buildaspell.spell_builder.visuals_color_effect", "From Effect");
        add("gui.buildaspell.spell_builder.visuals_color_custom", "Custom");
        add("gui.buildaspell.spell_builder.visuals_shape", "Shape: %s");
        add("gui.buildaspell.spell_builder.visuals_trail", "Trail: %s");
        add("gui.buildaspell.shape.cross", "Cross");
        add("gui.buildaspell.shape.cube", "Cube");
        add("gui.buildaspell.shape.ring", "Ring");
        add("gui.buildaspell.shape.sphere", "Sphere");
        add("gui.buildaspell.trail.witch", "Witch");
        add("gui.buildaspell.trail.flame", "Flame");
        add("gui.buildaspell.trail.soul_fire_flame", "Soul Flame");
        add("gui.buildaspell.trail.end_rod", "End Rod");
        add("gui.buildaspell.trail.crit", "Crit");
        add("gui.buildaspell.trail.enchanted_hit", "Enchanted");
        add("gui.buildaspell.trail.electric_spark", "Electric Spark");
        add("gui.buildaspell.trail.dragon_breath", "Dragon Breath");
        add("gui.buildaspell.trail.glow", "Glow");
        add("gui.buildaspell.trail.cherry", "Cherry");
        add("gui.buildaspell.trail.smoke", "Smoke");
        add("gui.buildaspell.trail.snowflake", "Snowflake");
        add("gui.buildaspell.trail.happy_villager", "Sparkle");

        // --- GUI: Component Types ---
        add("gui.buildaspell.component_type.delivery", "Delivery");
        add("gui.buildaspell.component_type.effect", "Effect");
        add("gui.buildaspell.component_type.modifier", "Modifier");
        add("gui.buildaspell.component_type.component", "Component");

        // --- Delivery Methods ---
        addDelivery(DeliveryMethod.RUNE, "Rune", "Place on surfaces");
        addDelivery(DeliveryMethod.SIGHT, "Sight", "Line of sight on crosshair");
        addDelivery(DeliveryMethod.SELF, "Self", "At your location");
        addDelivery(DeliveryMethod.CAST, "Cast", "Projectile spell");
        addDelivery(DeliveryMethod.TRACKING, "Tracking", "Tracking projectile");
        addDelivery(DeliveryMethod.TOUCH, "Touch", "Next hit applies the spell");
        addDelivery(DeliveryMethod.TRAP, "Trap", "Armed rune triggers on approach");

        // --- Spell Effects ---
        addEffect(SpellEffect.DAMAGE, "Damage", "Deals damage");
        addEffect(SpellEffect.IGNITE, "Ignite", "Sets on fire");
        addEffect(SpellEffect.FREEZE, "Freeze", "Freezes target");
        addEffect(SpellEffect.TELEPORT, "Teleport", "Teleports you");
        addEffect(SpellEffect.PULL, "Pull", "Pulls entities");
        addEffect(SpellEffect.PUSH, "Push", "Pushes entities");
        addEffect(SpellEffect.YEET, "Yeet", "Launches in look direction");
        addEffect(SpellEffect.REAP, "Reap", "Harvest crops");
        addEffect(SpellEffect.EXPLOSION, "Explosion", "Creates explosion");
        addEffect(SpellEffect.HEAL, "Heal", "Restores health");
        addEffect(SpellEffect.LIGHTNING, "Lightning", "Summons lightning");
        addEffect(SpellEffect.POISON, "Poison", "Applies poison");
        addEffect(SpellEffect.WITHER, "Wither", "Applies wither");
        addEffect(SpellEffect.SATURATION, "Saturation", "Restores hunger");
        addEffect(SpellEffect.LAUNCH, "Launch", "Launches entity up");
        addEffect(SpellEffect.LIGHT, "Light", "Creates light");
        addEffect(SpellEffect.SLAM, "Slam", "Slams down");
        addEffect(SpellEffect.LEVITATION, "Levitation", "Applies levitation");
        addEffect(SpellEffect.SLOW_FALL, "Slow Fall", "Slow falling");
        addEffect(SpellEffect.BREAK, "Break", "Breaks blocks");
        addEffect(SpellEffect.INVISIBILITY, "Invisibility", "Makes invisible");
        addEffect(SpellEffect.SPEED, "Speed", "Increases speed");
        addEffect(SpellEffect.HASTE, "Haste", "Mining speed up");
        addEffect(SpellEffect.BLINK, "Blink", "Short teleport");
        addEffect(SpellEffect.SWAP, "Swap", "Swap positions");
        addEffect(SpellEffect.SUMMON, "Summon", "Summons entity");
        addEffect(SpellEffect.CREATE_WATER, "Create Water", "Creates water");
        addEffect(SpellEffect.EVAPORATE_WATER, "Evaporate", "Removes water");
        addEffect(SpellEffect.MARK, "Mark", "Sets a mark");
        addEffect(SpellEffect.RECALL, "Recall", "Return to mark");
        addEffect(SpellEffect.PICKUP, "Pickup", "Pickup items");
        addEffect(SpellEffect.SHIELD, "Shield", "Creates barrier");
        addEffect(SpellEffect.CONJURE, "Conjure", "Conjures blocks");
        addEffect(SpellEffect.GROWTH, "Growth", "Grows crops/trees");
        addEffect(SpellEffect.CLEANSE, "Cleanse", "Removes potion effects");
        addEffect(SpellEffect.CHARM, "Charm", "Pacifies enemies so they stop attacking");
        addEffect(SpellEffect.BLIND, "Blind", "Reduces vision");
        addEffect(SpellEffect.SLOW, "Slow", "Slows the target");
        addEffect(SpellEffect.WEAKEN, "Weaken", "Weakens attacks");
        addEffect(SpellEffect.STRENGTHEN, "Strengthen", "Boosts melee damage");
        addEffect(SpellEffect.REGENERATE, "Regenerate", "Heals over time");
        addEffect(SpellEffect.RESIST, "Resist", "Reduces damage taken");
        addEffect(SpellEffect.NIGHT_VISION, "Night Vision", "See in the dark");
        addEffect(SpellEffect.WATER_BREATHING, "Water Breathing", "Breathe underwater");
        addEffect(SpellEffect.ROOT, "Root", "Snares the target in place");
        addEffect(SpellEffect.GRAPPLE, "Grapple", "Pull yourself to the target");
        addEffect(SpellEffect.GUST, "Gust", "Cone of wind in your gaze");

        // --- Spell Modifiers ---
        addModifier(SpellModifier.INCREASED_AREA, "Increased Area", "Larger radius");
        addModifier(SpellModifier.INCREASED_POWER, "Increased Power", "More powerful");
        addModifier(SpellModifier.NULLIFY, "Nullify", "Prevents entity damage");
        addModifier(SpellModifier.GENTLENESS, "Gentleness", "Softer effect");
        addModifier(SpellModifier.FORTUNATE_SON, "Fortunate Son", "Better drops");
        addModifier(SpellModifier.DOUBLE, "Double", "Double cast");
        addModifier(SpellModifier.ECHO, "Echo", "Re-casts at reduced power");
        addModifier(SpellModifier.PROLONGED, "Prolonged", "Status effects last longer");
        addModifier(SpellModifier.DELAY, "Delay", "Delayed cast");
        addModifier(SpellModifier.CHAIN, "Chain", "Chains to targets");
        addModifier(SpellModifier.PIERCE, "Pierce", "Pierces through");
        addModifier(SpellModifier.BOUNCE, "Bounce", "Bounces off walls");
        addModifier(SpellModifier.ACCELERATE, "Accelerate", "Faster projectile");
        addModifier(SpellModifier.DURATION, "Duration", "Zones & summons last longer");
        addModifier(SpellModifier.LINGER, "Linger", "Effects persist as a lingering area");
        addModifier(SpellModifier.SPLIT, "Split", "Split projectiles");
        addModifier(SpellModifier.WALL, "Wall", "Vertical structure");
        addModifier(SpellModifier.FLOOR, "Floor", "Horizontal platform");
        addModifier(SpellModifier.FILL, "Fill", "Fills holes");
        addModifier(SpellModifier.LEECH, "Leech", "Heal from damage dealt");
        addModifier(SpellModifier.SUNDER, "Sunder", "Bonus damage vs armor");
        addModifier(SpellModifier.RETURN, "Return", "Projectile boomerangs back");
    }

    private void addDelivery(DeliveryMethod delivery, String name, String desc) {
        add("delivery." + MOD_ID + "." + delivery.getSerializedName() + ".name", name);
        add("delivery." + MOD_ID + "." + delivery.getSerializedName() + ".desc", desc);
    }

    private void addEffect(SpellEffect effect, String name, String desc) {
        add("effect." + MOD_ID + "." + effect.getSerializedName() + ".name", name);
        add("effect." + MOD_ID + "." + effect.getSerializedName() + ".desc", desc);
    }

    private void addModifier(SpellModifier modifier, String name, String desc) {
        add("modifier." + MOD_ID + "." + modifier.getSerializedName() + ".name", name);
        add("modifier." + MOD_ID + "." + modifier.getSerializedName() + ".desc", desc);
    }
}
