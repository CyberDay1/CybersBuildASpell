package buildaspell.events;

import buildaspell.BuildASpell;
import buildaspell.mana.ManaHelper;
import buildaspell.mana.PlayerManaData;
import buildaspell.network.SyncPlayerManaPacket;
import buildaspell.registry.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = BuildASpell.MOD_ID)
public class PlayerManaEvents {
    private static final int TICKS_PER_SECOND = 20;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();

        if (player.level().isClientSide()) {
            return;
        }

        PlayerManaData manaData = player.getData(ModAttachments.PLAYER_MANA.get());

        float maxMana = ManaHelper.getMaxMana(player);
        float manaRegen = ManaHelper.getManaRegen(player);

        float regenPerTick = manaRegen / TICKS_PER_SECOND;

        float previousMana = manaData.getCurrentMana();
        if (previousMana < maxMana) {
            float newMana = Math.min(previousMana + regenPerTick, maxMana);
            manaData.setCurrentMana(newMana);
        } else if (previousMana > maxMana) {
            manaData.setCurrentMana(maxMana);
        }

        // Sync to client once per second when mana changed
        if (player.tickCount % TICKS_PER_SECOND == 0 && player instanceof ServerPlayer serverPlayer) {
            float current = manaData.getCurrentMana();
            if (current != previousMana || current != maxMana) {
                PacketDistributor.sendToPlayer(serverPlayer, new SyncPlayerManaPacket(current));
            }
        }
    }
}
