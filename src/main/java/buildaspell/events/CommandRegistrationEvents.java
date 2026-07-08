package buildaspell.events;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import buildaspell.BuildASpell;
import buildaspell.commands.ManaCommand;
import buildaspell.commands.SpellCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = BuildASpell.MOD_ID)
public class CommandRegistrationEvents {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        LiteralCommandNode<CommandSourceStack> root = dispatcher.register(
                Commands.literal(BuildASpell.MOD_ID)
                        .then(SpellCommand.build())
                        .then(ManaCommand.build())
        );

        // Short alias: /cbas -> /buildaspell
        dispatcher.register(Commands.literal("cbas").redirect(root));

        BuildASpell.LOGGER.debug("Registered commands");
    }
}
