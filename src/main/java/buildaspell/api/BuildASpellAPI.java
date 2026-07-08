package buildaspell.api;

import buildaspell.BuildASpell;
import buildaspell.spell.DeliveryMethod;
import buildaspell.spell.Spell;
import buildaspell.spell.SpellComponent;
import buildaspell.spell.SpellEffect;
import buildaspell.spell.SpellModifier;
import buildaspell.spell.execution.SpellExecutor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

/**
 * The stable, public integration contract for Build A Spell ({@code buildaspell}).
 *
 * <p>External mods (e.g. NeoOrigins) bind to this class at compile time, so the package,
 * class name and method signatures here are treated as a versioned public API surface and
 * will not be renamed across versions. Everything in the rest of the mod
 * ({@code SpellExecutor}, attachments, the component enums) is internal and must never be
 * touched by integrators directly.
 *
 * <h2>Contract</h2>
 * <ul>
 *   <li>All methods are server-side and safe to call with a {@link ServerPlayer}.</li>
 *   <li>Bad or unavailable input returns {@code false}/{@code null} and logs — never throws
 *       across the API boundary. This holds even if the integration is not wired, so an
 *       ungated power that calls these on a server without the intended setup degrades safely.</li>
 *   <li>The cast entry points can run a spell <b>without</b> consuming the BaS mana pool
 *       ({@code consumeMana = false}). Config enable/disable toggles for the delivery, effects
 *       and modifiers are always honored regardless of {@code consumeMana} — only the mana
 *       check (and its refund-on-failure) is bypassed.</li>
 * </ul>
 */
public final class BuildASpellAPI {

    private BuildASpellAPI() {}

    /**
     * Casts a spell as the given player, using the spell's own delivery method.
     *
     * @param caster      the server-side caster
     * @param spell       the spell to cast (typically built via {@link #createSpell})
     * @param consumeMana when false, skip the BaS mana check and the refund-on-failure path
     *                    entirely (cost is expected to be charged by the caller instead)
     * @return true if the cast was dispatched; false if input was invalid, a component was
     *         disabled in config, or (when {@code consumeMana}) the caster could not afford it
     */
    public static boolean cast(@Nullable ServerPlayer caster, @Nullable Spell spell, boolean consumeMana) {
        if (caster == null || spell == null) {
            BuildASpell.LOGGER.warn("BuildASpellAPI.cast called with null {}",
                    caster == null ? "caster" : "spell");
            return false;
        }
        try {
            return SpellExecutor.executeSpell(caster, spell, consumeMana);
        } catch (Exception e) {
            BuildASpell.LOGGER.error("BuildASpellAPI.cast failed for spell {}", spell, e);
            return false;
        }
    }

    /**
     * Casts a spell's effects at an explicit world position, bypassing the delivery's own
     * targeting (mirrors the in-game location-cast path used by rune/projectile/duration
     * deliveries). Honors the same config-gating and {@code consumeMana} contract as
     * {@link #cast}.
     *
     * @param caster      the server-side caster
     * @param spell       the spell whose effects should run at {@code pos}
     * @param pos         the world location to run the effects at
     * @param consumeMana when false, skip the BaS mana check and refund path entirely
     * @return true if the cast ran; false on invalid input, disabled component, or unaffordable
     */
    public static boolean castAtLocation(@Nullable ServerPlayer caster, @Nullable Spell spell,
                                         @Nullable Vec3 pos, boolean consumeMana) {
        if (caster == null || spell == null || pos == null) {
            BuildASpell.LOGGER.warn("BuildASpellAPI.castAtLocation called with null argument");
            return false;
        }
        try {
            return SpellExecutor.executeSpellAtLocation(caster, spell, pos, consumeMana);
        } catch (Exception e) {
            BuildASpell.LOGGER.error("BuildASpellAPI.castAtLocation failed for spell {}", spell, e);
            return false;
        }
    }

    /**
     * Builds a {@link Spell} from author-supplied string ids: a delivery id plus a single,
     * <b>ordered</b> list of component ids. Order is meaningful — a modifier binds to the effect
     * that follows it — so the components must be one interleaved list, not a split of
     * effects/modifiers.
     *
     * <p>Each component id is resolved in priority order:
     * {@link SpellModifier#fromId} &rarr; {@link SpellEffect#fromId} &rarr; if it starts with
     * {@code "compat:"} it is wrapped verbatim as a {@link SpellComponent.CompatEffect}. A
     * non-{@code compat:} id that resolves to none of the enums causes the whole build to fail
     * ({@code null} + log).
     *
     * <p>The same rules the in-game builder enforces apply: the list is capped at
     * {@link Spell#MAX_COMPONENTS} and a non-stackable modifier already present is silently
     * dropped (both handled by {@link Spell#addComponent}), so an origin-built spell can never
     * exceed what a player could assemble by hand.
     *
     * @return the assembled spell, or {@code null} if the delivery id or any component id is invalid
     */
    @Nullable
    public static Spell createSpell(@Nullable String deliveryId, @Nullable List<String> componentIds) {
        if (deliveryId == null || componentIds == null) {
            BuildASpell.LOGGER.warn("BuildASpellAPI.createSpell called with null argument");
            return null;
        }

        DeliveryMethod delivery = DeliveryMethod.fromId(deliveryId);
        if (delivery == null) {
            BuildASpell.LOGGER.warn("BuildASpellAPI.createSpell: unknown delivery id '{}'", deliveryId);
            return null;
        }

        Spell spell = new Spell();
        spell.setDelivery(delivery);

        for (String id : componentIds) {
            if (id == null) {
                BuildASpell.LOGGER.warn("BuildASpellAPI.createSpell: null component id");
                return null;
            }

            SpellModifier modifier = SpellModifier.fromId(id);
            if (modifier != null) {
                spell.addComponent(new SpellComponent.Modifier(modifier));
                continue;
            }

            SpellEffect effect = SpellEffect.fromId(id);
            if (effect != null) {
                spell.addComponent(new SpellComponent.Effect(effect));
                continue;
            }

            if (id.startsWith("compat:")) {
                spell.addComponent(new SpellComponent.CompatEffect(id));
                continue;
            }

            BuildASpell.LOGGER.warn("BuildASpellAPI.createSpell: unknown component id '{}'", id);
            return null;
        }

        return spell;
    }

    /** The live delivery-method ids (rune, sight, self, cast, tracking). */
    public static List<String> deliveryIds() {
        return Arrays.stream(DeliveryMethod.values()).map(DeliveryMethod::getSerializedName).toList();
    }

    /** The live effect ids (all {@link SpellEffect} values). */
    public static List<String> effectIds() {
        return Arrays.stream(SpellEffect.values()).map(SpellEffect::getSerializedName).toList();
    }

    /** The live modifier ids (all {@link SpellModifier} values). */
    public static List<String> modifierIds() {
        return Arrays.stream(SpellModifier.values()).map(SpellModifier::getSerializedName).toList();
    }
}
