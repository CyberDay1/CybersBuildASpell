package buildaspell.spell.data;

import buildaspell.spell.Spell;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Everything a {@link SpellPrimitive} needs to run. Mirrors the legacy
 * {@code executeX(caster, level, origin, spell, spellPower)} signature so built-in
 * behaviors can be wrapped as primitives with no change in semantics.
 */
public record SpellContext(Player caster, Level level, Vec3 origin, Spell spell, float spellPower, int stacks) {

    /**
     * How many identical copies of the effect this one run stands for. Repeating an effect in a spell
     * normally just runs it that many times; Damage instead runs once with {@code stacks} set, so the
     * whole stack lands as a single blow and armor and Resistance are measured against the total. A
     * pack replacing Damage's behavior should multiply by this, or repeats of it will do nothing.
     */
    public SpellContext(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
        this(caster, level, origin, spell, spellPower, 1);
    }
}
