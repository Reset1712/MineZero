package boomcow.minezero.command;

import boomcow.minezero.checkpoint.CheckpointData;
import boomcow.minezero.checkpoint.CheckpointManager;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class TriggerRBD {
    private static final Logger LOGGER = LoggerFactory.getLogger("MineZeroRBD");

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(
                CommandManager.literal("triggerRBD")
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            ServerWorld world = source.getWorld();
                            CheckpointData data = CheckpointData.get(world);
                            UUID anchorPlayerUUID = data.getAnchorPlayerUUID();

                            if (anchorPlayerUUID == null) {
                                source.sendError(Text.literal("No anchor player (Subaru player) is set. Cannot trigger Return By Death."));
                                LOGGER.warn("Attempted to trigger RBD, but no anchor player is set.");
                                return 0;
                            }

                            ServerPlayerEntity anchorPlayer = world.getServer().getPlayerManager().getPlayer(anchorPlayerUUID);
                            if (anchorPlayer == null) {
                                source.sendError(Text.literal("The anchor player (UUID: " + anchorPlayerUUID.toString() + ") is not currently online. Cannot trigger Return By Death."));
                                LOGGER.warn("Attempted to trigger RBD for anchor {}, but player is not online.", anchorPlayerUUID);
                                return 0;
                            }

                            source.sendFeedback(() -> Text.literal("Manually triggering Return By Death for anchor player: " + anchorPlayer.getName().getString()), true);
                            LOGGER.info("Manually triggering RBD for anchor: {}", anchorPlayer.getName().getString());
                            
                            world.getServer().execute(() -> {
                                CheckpointManager.restoreCheckpoint(anchorPlayer);
                                world.getServer().getPlayerManager().getPlayerList().forEach(p -> {
                                    p.sendMessage(Text.literal("Return By Death has been manually triggered! Resetting to the last checkpoint."), false);
                                });
                            });

                            return 1;
                        })
        );
    }
}
