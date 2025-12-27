package boomcow.minezero.command;

import boomcow.minezero.checkpoint.CheckpointManager;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class SetCheckPointCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, net.minecraft.command.CommandRegistryAccess registryAccess) {
        dispatcher.register(
                CommandManager.literal("setcheckpoint")
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            ServerPlayerEntity player = source.getPlayerOrThrow();

                            CheckpointManager.setCheckpoint(player);
                            source.sendFeedback(() -> Text.literal("Checkpoint set for yourself!"), true);
                            return 1;
                        })
                        .then(CommandManager.argument("target", EntityArgumentType.player())
                                .executes(context -> {
                                    ServerCommandSource source = context.getSource();
                                    ServerPlayerEntity targetPlayer = EntityArgumentType.getPlayer(context, "target");

                                    CheckpointManager.setCheckpoint(targetPlayer);
                                    source.sendFeedback(() -> Text.literal("Checkpoint set for " + targetPlayer.getName().getString() + "!"), true);
                                    return 1;
                                }))
        );
    }
}
