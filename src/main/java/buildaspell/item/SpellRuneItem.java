package buildaspell.item;

import buildaspell.config.ModConfig;
import buildaspell.network.SyncPlayerSpellDataPacket;
import buildaspell.registry.ModAttachments;
import buildaspell.spell.DeliveryMethod;
import buildaspell.spell.PlayerSpellData;
import buildaspell.spell.SpellEffect;
import buildaspell.spell.SpellModifier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class SpellRuneItem extends Item {
    public SpellRuneItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack heldStack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResultHolder.success(heldStack);
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.pass(heldStack);
        }

        PlayerSpellData spellData = serverPlayer.getData(ModAttachments.PLAYER_SPELL_DATA.get());

        // Collect all locked components
        List<Object> locked = new ArrayList<>();

        // Disabled components are never obtainable: skip them so a fill rune can't unlock them.
        for (DeliveryMethod method : DeliveryMethod.values()) {
            if (!spellData.isDeliveryUnlocked(method) && ModConfig.isDeliveryEnabled(method)) {
                locked.add(method);
            }
        }
        for (SpellEffect effect : SpellEffect.values()) {
            if (!spellData.isEffectUnlocked(effect) && ModConfig.isEffectEnabled(effect)) {
                locked.add(effect);
            }
        }
        for (SpellModifier modifier : SpellModifier.values()) {
            if (!spellData.isModifierUnlocked(modifier) && ModConfig.isModifierEnabled(modifier)) {
                locked.add(modifier);
            }
        }

        if (locked.isEmpty()) {
            serverPlayer.sendSystemMessage(
                    Component.literal("You have already unlocked all spell components!").withStyle(ChatFormatting.YELLOW),
                    true
            );
            return InteractionResultHolder.fail(heldStack);
        }

        // Pick a random locked component
        Object chosen = locked.get(level.getRandom().nextInt(locked.size()));

        String componentName;
        String componentType;

        if (chosen instanceof DeliveryMethod method) {
            spellData.unlockDelivery(method);
            componentName = method.getSerializedName();
            componentType = "Delivery Method";
        } else if (chosen instanceof SpellEffect effect) {
            spellData.unlockEffect(effect);
            componentName = effect.getSerializedName();
            componentType = "Spell Effect";
        } else if (chosen instanceof SpellModifier modifier) {
            spellData.unlockModifier(modifier);
            componentName = modifier.getSerializedName();
            componentType = "Spell Modifier";
        } else {
            return InteractionResultHolder.fail(heldStack);
        }

        // Sync to client
        PacketDistributor.sendToPlayer(serverPlayer, SyncPlayerSpellDataPacket.fromPlayerData(spellData));

        // Consume the rune
        player.getItemInHand(hand).shrink(1);

        // Feedback
        String displayName = componentName.replace('_', ' ');
        displayName = displayName.substring(0, 1).toUpperCase() + displayName.substring(1);

        serverPlayer.sendSystemMessage(
                Component.literal("Unlocked ").withStyle(ChatFormatting.GREEN)
                        .append(Component.literal(componentType + ": ").withStyle(ChatFormatting.GOLD))
                        .append(Component.literal(displayName).withStyle(ChatFormatting.AQUA)),
                false
        );

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0f, 1.0f);

        return InteractionResultHolder.success(heldStack);
    }
}
