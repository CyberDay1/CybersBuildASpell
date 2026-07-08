package buildaspell.commands;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import buildaspell.mana.ManaHelper;
import buildaspell.mana.PlayerManaData;
import buildaspell.network.SyncPlayerManaPacket;
import buildaspell.registry.ModAttachments;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public class ManaCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("mana")
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("get")
                        .executes(context -> getMana(context, context.getSource().getPlayerOrException()))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> getMana(context, EntityArgument.getPlayer(context, "target")))
                        )
                )
                .then(Commands.literal("set")
                        .then(Commands.argument("amount", FloatArgumentType.floatArg(0))
                                .executes(context -> setMana(context, context.getSource().getPlayerOrException(), FloatArgumentType.getFloat(context, "amount")))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> setMana(context, EntityArgument.getPlayer(context, "target"), FloatArgumentType.getFloat(context, "amount")))
                                )
                        )
                )
                .then(Commands.literal("add")
                        .then(Commands.argument("amount", FloatArgumentType.floatArg())
                                .executes(context -> addMana(context, context.getSource().getPlayerOrException(), FloatArgumentType.getFloat(context, "amount")))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> addMana(context, EntityArgument.getPlayer(context, "target"), FloatArgumentType.getFloat(context, "amount")))
                                )
                        )
                )
                .then(Commands.literal("stats")
                        .executes(context -> getStats(context, context.getSource().getPlayerOrException()))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> getStats(context, EntityArgument.getPlayer(context, "target")))
                        )
                )
                ;
    }

    private static int getMana(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        PlayerManaData manaData = player.getData(ModAttachments.PLAYER_MANA.get());
        float currentMana = manaData.getCurrentMana();
        float maxMana = ManaHelper.getMaxMana(player);

        context.getSource().sendSuccess(
                () -> Component.literal(player.getName().getString() + " has " + String.format("%.1f", currentMana) + "/" + String.format("%.1f", maxMana) + " mana"),
                false
        );
        return 1;
    }

    private static int setMana(CommandContext<CommandSourceStack> context, ServerPlayer player, float amount) {
        PlayerManaData manaData = player.getData(ModAttachments.PLAYER_MANA.get());
        manaData.setCurrentMana(amount);
        PacketDistributor.sendToPlayer(player, new SyncPlayerManaPacket(manaData.getCurrentMana()));

        context.getSource().sendSuccess(
                () -> Component.literal("Set " + player.getName().getString() + "'s mana to " + String.format("%.1f", amount)),
                true
        );
        return 1;
    }

    private static int addMana(CommandContext<CommandSourceStack> context, ServerPlayer player, float amount) {
        PlayerManaData manaData = player.getData(ModAttachments.PLAYER_MANA.get());
        manaData.addMana(amount);
        float newMana = manaData.getCurrentMana();
        PacketDistributor.sendToPlayer(player, new SyncPlayerManaPacket(newMana));

        context.getSource().sendSuccess(
                () -> Component.literal("Added " + String.format("%.1f", amount) + " mana to " + player.getName().getString() + " (now at " + String.format("%.1f", newMana) + ")"),
                true
        );
        return 1;
    }

    private static int getStats(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        float maxMana = ManaHelper.getMaxMana(player);
        float manaRegen = ManaHelper.getManaRegen(player);
        float spellPower = ManaHelper.getSpellPower(player);

        PlayerManaData manaData = player.getData(ModAttachments.PLAYER_MANA.get());
        float currentMana = manaData.getCurrentMana();

        context.getSource().sendSuccess(
                () -> Component.literal(player.getName().getString() + "'s Stats:\n" +
                        "Current Mana: " + String.format("%.1f", currentMana) + "\n" +
                        "Max Mana: " + String.format("%.1f", maxMana) + "\n" +
                        "Mana Regen: " + String.format("%.1f", manaRegen) + "/s\n" +
                        "Spell Power: " + String.format("%.1f", spellPower)),
                false
        );
        return 1;
    }
}
