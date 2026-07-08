package buildaspell.network;

import buildaspell.registry.ModAttachments;
import buildaspell.spell.PlayerSpellSlots;
import buildaspell.spell.Spell;
import buildaspell.spell.execution.SpellExecutor;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CastSpellPacket() implements CustomPacketPayload {
    public static final Type<CastSpellPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath("buildaspell", "cast_spell"));

    public static final StreamCodec<ByteBuf, CastSpellPacket> STREAM_CODEC = StreamCodec.unit(new CastSpellPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CastSpellPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                PlayerSpellSlots spellSlots = serverPlayer.getData(ModAttachments.PLAYER_SPELL_SLOTS.get());
                Spell activeSpell = spellSlots.getActiveSpell();

                if (activeSpell != null) {
                    SpellExecutor.executeSpell(serverPlayer, activeSpell);
                }
            }
        });
    }
}
