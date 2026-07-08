package buildaspell.registry;

import buildaspell.BuildASpell;
import buildaspell.mana.PlayerManaData;
import buildaspell.spell.PlayerSpellData;
import buildaspell.spell.PlayerSpellSlots;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, BuildASpell.MOD_ID);

    public static final Supplier<AttachmentType<PlayerManaData>> PLAYER_MANA =
            ATTACHMENT_TYPES.register("player_mana",
                    () -> AttachmentType.serializable((Supplier<PlayerManaData>) PlayerManaData::new)
                            .build());

    public static final Supplier<AttachmentType<PlayerSpellData>> PLAYER_SPELL_DATA =
            ATTACHMENT_TYPES.register("player_spell_data",
                    () -> AttachmentType.serializable((Supplier<PlayerSpellData>) PlayerSpellData::new)
                            .copyOnDeath()
                            .build());

    public static final Supplier<AttachmentType<PlayerSpellSlots>> PLAYER_SPELL_SLOTS =
            ATTACHMENT_TYPES.register("player_spell_slots",
                    () -> AttachmentType.serializable((Supplier<PlayerSpellSlots>) PlayerSpellSlots::new)
                            .copyOnDeath()
                            .build());

    public static void register(IEventBus eventBus) {
        ATTACHMENT_TYPES.register(eventBus);
    }
}
