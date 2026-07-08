package buildaspell.registry;

import buildaspell.BuildASpell;
import buildaspell.mana.PlayerManaData;
import buildaspell.spell.PlayerSpellData;
import buildaspell.spell.PlayerSpellSlots;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.util.ValueIOSerializable;
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
 * the world is reloaded.
 *
 * <p>Attachment sync only covers the initial re-seed: the mod mutates these objects in place rather
 * than through {@code setData}, so NeoForge never sees an update to broadcast. Incremental updates
 * (unlocking a component, saving a spell, spending mana) still go over the mod's own sync packets.
 */
public class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, BuildASpell.MOD_ID);

    /**
     * Builds the network form of an attachment out of the very same {@link ValueIOSerializable}
     * methods that write it to disk, so the wire form and the saved form cannot drift apart. Going
     * through each class's {@code Codec} instead would be the drift: {@code PlayerSpellData.CODEC}
     * carries the three unlock sets but not the starter-kit / guidebook flags its {@code serialize}
     * writes.
     *
     * <p>Decoding uses {@link ProblemReporter#DISCARDING}: a malformed field on the wire is dropped
     * and the receiving object keeps whatever its constructor gave that field. Encoding collects and
     * warns instead, because a problem on the sending side is this mod's own bug and would otherwise
     * leave no trace at all. Neither direction throws: this runs inside a network encode on login,
     * respawn and dimension change, so an exception here would drop the player's connection, which is
     * a worse outcome than the incomplete palette this sync exists to fix.
     */
    private static <T extends ValueIOSerializable> StreamCodec<RegistryFriendlyByteBuf, T> valueIoStreamCodec(
            Supplier<T> factory) {
        return new StreamCodec<>() {
            @Override
            public T decode(RegistryFriendlyByteBuf buf) {
                CompoundTag tag = ByteBufCodecs.COMPOUND_TAG.decode(buf);
                T value = factory.get();
                value.deserialize(TagValueInput.create(ProblemReporter.DISCARDING, buf.registryAccess(), tag));
                return value;
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, T value) {
                ProblemReporter.Collector reporter = new ProblemReporter.Collector();
                TagValueOutput output = TagValueOutput.createWithContext(reporter, buf.registryAccess());
                value.serialize(output);
                if (!reporter.isEmpty()) {
                    BuildASpell.LOGGER.warn("Attachment did not fully serialize for sync, sending anyway: {}",
                            reporter.getReport());
                }
                ByteBufCodecs.COMPOUND_TAG.encode(buf, output.buildResult());
            }
        };
    }

    /**
     * Deliberately not {@code copyOnDeath()}: the no-arg constructor starts at
     * {@code DEFAULT_MAX_MANA}, so dropping the attachment on death is what gives a respawning player
     * a full bar. Copying it forward would respawn them with whatever they died on instead.
     */
    public static final Supplier<AttachmentType<PlayerManaData>> PLAYER_MANA =
            ATTACHMENT_TYPES.register("player_mana",
                    () -> AttachmentType.serializable((Supplier<PlayerManaData>) PlayerManaData::new)
                            .sync(valueIoStreamCodec(PlayerManaData::new))
                            .build());

    public static final Supplier<AttachmentType<PlayerSpellData>> PLAYER_SPELL_DATA =
            ATTACHMENT_TYPES.register("player_spell_data",
                    () -> AttachmentType.serializable((Supplier<PlayerSpellData>) PlayerSpellData::new)
                            .sync(valueIoStreamCodec(PlayerSpellData::new))
                            .copyOnDeath()
                            .build());

    public static final Supplier<AttachmentType<PlayerSpellSlots>> PLAYER_SPELL_SLOTS =
            ATTACHMENT_TYPES.register("player_spell_slots",
                    () -> AttachmentType.serializable((Supplier<PlayerSpellSlots>) PlayerSpellSlots::new)
                            .sync(valueIoStreamCodec(PlayerSpellSlots::new))
                            .copyOnDeath()
                            .build());

    public static void register(IEventBus eventBus) {
        ATTACHMENT_TYPES.register(eventBus);
    }
}
