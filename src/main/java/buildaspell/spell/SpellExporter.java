package buildaspell.spell;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Handles import/export of spells as Base64-encoded strings.
 * Format: delivery|effect1,effect2,...|modifier1:count,modifier2:count
 */
public class SpellExporter {

    // --- NeoOrigins "Export to power" contract -------------------------------------------------
    // These field names / type ids are a VERSIONED CONTRACT shared with NeoOrigins' cast_spell
    // action codec. If NeoOrigins ever renames them, this generator updates in lockstep. The
    // export is pure JSON-string generation and does NOT require NeoOrigins on the classpath.
    private static final String CAST_SPELL_TYPE = "neoorigins:cast_spell";
    private static final String ACTIVE_ABILITY_TYPE = "neoorigins:active_ability";
    private static final String COST_TODO =
            "REQUIRED: set a cost before this power will load. Fill resource_cost + "
            + "resource_cost_amount (and/or hunger_cost / cooldown_ticks).";

    /**
     * Emits a ready-to-paste NeoOrigins {@code active_ability} power JSON that casts this spell.
     * The full spell is baked inline as a delivery id + the ordered component-id list (the same
     * shape {@code BuildASpellAPI.createSpell} consumes), with the cost left blank on purpose so
     * the author must set one before the power will load. Caller should gate on
     * {@link Spell#hasSpell()} before invoking.
     */
    public static String toNeoOriginsPower(Spell spell) {
        JsonObject action = new JsonObject();
        action.addProperty("type", CAST_SPELL_TYPE);
        action.addProperty("delivery",
                spell.getDelivery() != null ? spell.getDelivery().getSerializedName() : "");

        JsonArray components = new JsonArray();
        for (SpellComponent component : spell.getComponents()) {
            components.add(component.id());
        }
        action.add("components", components);

        JsonObject power = new JsonObject();
        power.addProperty("type", ACTIVE_ABILITY_TYPE);
        // The "comment" key is ignored by the NeoOrigins codec (it only reads known fields),
        // so it is a safe place to carry the cost TODO. Do NOT pre-fill a working cost.
        power.addProperty("comment", COST_TODO);
        power.addProperty("resource_cost", "");
        power.addProperty("resource_cost_amount", 0);
        power.addProperty("hunger_cost", 0);
        power.addProperty("cooldown_ticks", 0);
        power.add("entity_action", action);

        return new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(power);
    }

    public static String encode(Spell spell) {
        StringBuilder sb = new StringBuilder();

        // Delivery
        sb.append(spell.getDelivery() != null ? spell.getDelivery().getSerializedName() : "none");
        sb.append('|');

        // Components in order, preserving interleaved effect/modifier structure
        List<String> componentEntries = new ArrayList<>();
        for (SpellComponent component : spell.getComponents()) {
            if (component instanceof SpellComponent.Effect e) {
                componentEntries.add("e:" + e.effect().getSerializedName());
            } else if (component instanceof SpellComponent.Modifier m) {
                componentEntries.add("m:" + m.modifier().getSerializedName());
            } else if (component instanceof SpellComponent.CompatEffect c) {
                componentEntries.add("c:" + c.effectId());
            }
        }
        sb.append(String.join(",", componentEntries));

        // Visual section (optional; older decoders simply ignore the extra part)
        SpellVisual visual = spell.getVisual();
        if (visual != null && !visual.equals(SpellVisual.DEFAULT)) {
            sb.append('|');
            sb.append("v:").append(visual.color())
              .append(',').append(visual.shape().getSerializedName())
              .append(',').append(visual.trail());
        }

        return Base64.getEncoder().encodeToString(sb.toString().getBytes());
    }

    @Nullable
    public static Spell decode(String encoded) {
        try {
            String plaintext = new String(Base64.getDecoder().decode(encoded));
            return fromPlaintext(plaintext);
        } catch (Exception e) {
            return null;
        }
    }

    public static String getPlaintext(String encoded) {
        try {
            return new String(Base64.getDecoder().decode(encoded));
        } catch (Exception e) {
            return "";
        }
    }

    @Nullable
    private static Spell fromPlaintext(String plaintext) {
        String[] parts = plaintext.split("\\|", -1);
        if (parts.length < 2) return null;

        Spell spell = new Spell();

        // Parse delivery
        if (!parts[0].equals("none")) {
            DeliveryMethod delivery = DeliveryMethod.fromId(parts[0]);
            if (delivery == null) return null;
            spell.setDelivery(delivery);
        }

        // Parse ordered components (new format: e:id, m:id, c:id)
        if (!parts[1].isEmpty()) {
            for (String entry : parts[1].split(",")) {
                if (entry.startsWith("e:")) {
                    SpellEffect effect = SpellEffect.fromId(entry.substring(2));
                    if (effect != null) spell.addComponent(new SpellComponent.Effect(effect));
                } else if (entry.startsWith("m:")) {
                    SpellModifier modifier = SpellModifier.fromId(entry.substring(2));
                    if (modifier != null) spell.addComponent(new SpellComponent.Modifier(modifier));
                } else if (entry.startsWith("c:")) {
                    spell.addComponent(new SpellComponent.CompatEffect(entry.substring(2)));
                } else {
                    // Legacy format fallback: bare effect IDs
                    SpellEffect effect = SpellEffect.fromId(entry);
                    if (effect != null) spell.addComponent(new SpellComponent.Effect(effect));
                }
            }
        }

        // Trailing visual section (new format): "v:color,shape,trail" — may appear as the 3rd part.
        // Legacy 3-part codes used the 3rd part for a separate modifier section instead.
        if (parts.length == 3 && !parts[2].isEmpty()) {
            if (parts[2].startsWith("v:")) {
                parseVisual(spell, parts[2].substring(2));
            } else {
                for (String modEntry : parts[2].split(",")) {
                    String[] modParts = modEntry.split(":");
                    if (modParts.length == 2) {
                        SpellModifier modifier = SpellModifier.fromId(modParts[0]);
                        if (modifier != null) {
                            int count = Integer.parseInt(modParts[1]);
                            for (int i = 0; i < count; i++) {
                                spell.addComponent(new SpellComponent.Modifier(modifier));
                            }
                        }
                    }
                }
            }
        }

        // Enforce component limit on imported spells
        if (spell.getComponents().size() > Spell.MAX_COMPONENTS) {
            List<SpellComponent> trimmed = spell.getComponents().subList(0, Spell.MAX_COMPONENTS);
            spell.setComponents(new ArrayList<>(trimmed));
        }

        return spell;
    }

    /** Parses "color,shape,trail" into the spell's visual; tolerant of malformed input. */
    private static void parseVisual(Spell spell, String body) {
        String[] v = body.split(",", -1);
        if (v.length < 3) return;
        try {
            int color = Integer.parseInt(v[0]);
            ProjectileShape shape = ProjectileShape.fromId(v[1]);
            String trail = v[2].isEmpty() ? SpellVisual.DEFAULT_TRAIL : v[2];
            spell.setVisual(new SpellVisual(color, shape, trail));
        } catch (NumberFormatException ignored) {
            // leave default visual
        }
    }
}
