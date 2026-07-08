package buildaspell.spell;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class SpellSlot {
    public static final Codec<SpellSlot> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.fieldOf("name").forGetter(SpellSlot::getName),
            Spell.CODEC.fieldOf("spell").forGetter(SpellSlot::getSpell)
    ).apply(inst, SpellSlot::new));

    private String name;
    private Spell spell;

    public SpellSlot() {
        this("", new Spell());
    }

    public SpellSlot(String name, Spell spell) {
        this.name = name;
        this.spell = spell;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Spell getSpell() { return spell; }
    public void setSpell(Spell spell) { this.spell = spell; }
    public boolean hasSpell() { return spell.hasSpell(); }
}
