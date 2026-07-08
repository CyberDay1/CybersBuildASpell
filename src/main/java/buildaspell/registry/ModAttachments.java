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

/**
 * Player data attachments.
 *
 * <p>Every type here is registered with {@code sync(...)}. That matters because the client throws its
 * whole player away and rebuilds it on respawn and on dimension change, losing all attachments with
 * it; NeoForge re-sends synced attachments at exactly those three points (login, respawn, dimension
 * change), so the spell palette and the saved-spell slots come back instead of reading empty until
 * the world is reloaded. The stream codecs are derived from the same {@code Codec} used for disk
 * serialization, so the wire form and the saved form cannot drift apart.
 *
 * <p>Attachment sync only covers the initial re-seed: the mod mutates these objects in place rather
 * than through {@code setData}, so NeoForge never sees an update to broadcast. Incremental updates
 * (unlocking a component, saving a spell, spending mana) still go over the mod's own sync packets.
 */
public class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, BuildASpell.MOD_ID);

    /**
     * Deliberately not {@code copyOnDeath()}: the no-arg constructor starts at
     * {@code DEFAULT_MAX_MANA}, so dropping the attachment on death is what gives a respawning player
     * a full bar. Copying it forward would respawn them with whatever they died on instead.
     */
    public static final Supplier<AttachmentType<PlayerManaData>> PLAYER_MANA =
            ATTACHMENT_TYPES.register("player_mana",
                    () -> AttachmentType.builder((Supplier<PlayerManaData>) PlayerManaData::new)
                            .serialize(PlayerManaData.CODEC)
                            .sync(PlayerManaData.STREAM_CODEC)
                            .build());

    public static final Supplier<AttachmentType<PlayerSpellData>> PLAYER_SPELL_DATA =
            ATTACHMENT_TYPES.register("player_spell_data",
                    () -> AttachmentType.builder((Supplier<PlayerSpellData>) PlayerSpellData::new)
                            .serialize(PlayerSpellData.CODEC)
                            .sync(PlayerSpellData.STREAM_CODEC)
                            .copyOnDeath()
                            .build());

    public static final Supplier<AttachmentType<PlayerSpellSlots>> PLAYER_SPELL_SLOTS =
            ATTACHMENT_TYPES.register("player_spell_slots",
                    () -> AttachmentType.builder((Supplier<PlayerSpellSlots>) PlayerSpellSlots::new)
                            .serialize(PlayerSpellSlots.CODEC)
                            .sync(PlayerSpellSlots.STREAM_CODEC)
                            .copyOnDeath()
                            .build());

    public static void register(IEventBus eventBus) {
        ATTACHMENT_TYPES.register(eventBus);
    }
}
