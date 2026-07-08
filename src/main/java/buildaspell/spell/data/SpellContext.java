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
public record SpellContext(Player caster, Level level, Vec3 origin, Spell spell, float spellPower) {
}
