package buildaspell.registry;

import buildaspell.BuildASpell;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(Registries.ATTRIBUTE, BuildASpell.MOD_ID);

    public static final DeferredHolder<Attribute, RangedAttribute> MANA_POOL =
            ATTRIBUTES.register("mana_pool",
                    () -> new RangedAttribute("attribute.buildaspell.mana_pool", 100.0, 0.0, 10000.0));

    public static final DeferredHolder<Attribute, RangedAttribute> MANA_REGEN =
            ATTRIBUTES.register("mana_regen",
                    () -> new RangedAttribute("attribute.buildaspell.mana_regen", 5.0, 0.0, 1000.0));

    public static final DeferredHolder<Attribute, RangedAttribute> SPELL_POWER =
            ATTRIBUTES.register("spell_power",
                    () -> new RangedAttribute("attribute.buildaspell.spell_power", 10.0, 0.0, 1000.0));

    public static void register(IEventBus eventBus) {
        ATTRIBUTES.register(eventBus);
    }

    public static void onAttributeModification(EntityAttributeModificationEvent event) {
        event.add(EntityTypes.PLAYER, MANA_POOL);
        event.add(EntityTypes.PLAYER, MANA_REGEN);
        event.add(EntityTypes.PLAYER, SPELL_POWER);
    }
}
