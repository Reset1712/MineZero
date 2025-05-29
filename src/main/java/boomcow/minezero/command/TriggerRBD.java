package boomcow.minezero.command;

import boomcow.minezero.checkpoint.CheckpointData;
import boomcow.minezero.checkpoint.CheckpointManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.util.UUID;

public class TriggerRBD {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("triggerRBD")
                        .requires(cs -> cs.hasPermission(2)) // Requires OP_LEVEL 2
                        .executes(context -> {
                            ServerLevel level = context.getSource().getLevel();
                            CheckpointData data = CheckpointData.get(level);
                            UUID anchorPlayerUUID = data.getAnchorPlayerUUID();

                            if (anchorPlayerUUID == null) {
                                context.getSource().sendFailure(Component.literal("No anchor player (Subaru player) is set. Cannot trigger Return By Death."));
                                LOGGER.warn("Attempted to trigger RBD, but no anchor player is set.");
                                return 0;
                            }

                            ServerPlayer anchorPlayer = level.getServer().getPlayerList().getPlayer(anchorPlayerUUID);
                            if (anchorPlayer == null) {
                                // If the anchor player is offline, we might still want to proceed if CheckpointManager.restoreCheckpoint
                                // can handle it based on stored data. However, the current CheckpointManager.restoreCheckpoint(ServerPlayer)
                                // heavily relies on the ServerPlayer object for teleportation, stats, etc.
                                // For now, we require the anchor player to be online or we find another way to get a reference.
                                // A more robust solution might involve refactoring restoreCheckpoint to take a UUID
                                // and fetch/reconstruct necessary player data.
                                //
                                // As a temporary measure, or if this is acceptable, inform the user.
                                // Alternatively, if restoreCheckpoint is robust enough or you have a way to get an offline ServerPlayer representation,
                                // you could proceed. Given current restoreCheckpoint, online is better.
                                context.getSource().sendFailure(Component.literal("The anchor player (UUID: " + anchorPlayerUUID.toString() + ") is not currently online. Cannot trigger Return By Death with current implementation."));
                                LOGGER.warn("Attempted to trigger RBD for anchor {}, but player is not online.", anchorPlayerUUID);
                                return 0;
                            }

                            context.getSource().sendSuccess(() -> Component.literal("Manually triggering Return By Death for anchor player: " + anchorPlayer.getName().getString() + ". World will reset to checkpoint."), true);
                            LOGGER.info("Manually triggering Return By Death for anchor player: {} (UUID: {}) by command sender: {}", anchorPlayer.getName().getString(), anchorPlayerUUID, context.getSource().getDisplayName().getString());


                            // Execute the restore logic on the server thread
                            level.getServer().execute(() -> {
                                CheckpointManager.restoreCheckpoint(anchorPlayer); // This is the key call
                                // Notify all players that a manual reset has occurred
                                level.getServer().getPlayerList().getPlayers().forEach(p -> {
                                    p.displayClientMessage(Component.literal("Return By Death has been manually triggered! Resetting to the last checkpoint."), false); // false for chat
                                });
                                LOGGER.info("Return By Death manually triggered and checkpoint restored.");
                            });

                            return 1;
                        })
        );
    }
}
