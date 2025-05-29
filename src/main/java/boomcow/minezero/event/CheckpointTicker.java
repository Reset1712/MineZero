package boomcow.minezero.event;

import boomcow.minezero.ModGameRules;
import boomcow.minezero.checkpoint.CheckpointData;
import boomcow.minezero.checkpoint.CheckpointManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ThreadLocalRandom;

public class CheckpointTicker {

    private static final Logger LOGGER = LogManager.getLogger(CheckpointTicker.class);

    // Track when the last checkpoint was set (in server ticks)
    public static long lastCheckpointTick = 0;
    // How many ticks until the next checkpoint trigger
    private static long nextCheckpointInterval = 0;

    private static boolean randomIntervalSelected = false;

    private static int intervalTicks = 0;

    @SubscribeEvent
    public static void onServerTickPost(ServerTickEvent.Post event) { // <--- THE KEY CHANGE HERE
        MinecraftServer server = event.getServer();
        ServerLevel level = server.overworld();
        if (level == null) return;

        // Get the auto checkpoint game rule. If it's not present, log a warning and exit.
        var autoRule = level.getGameRules().getRule(ModGameRules.AUTO_CHECKPOINT_ENABLED);
        if (autoRule == null) {
            LOGGER.warn("Auto checkpoint game rule not found in world. Skipping checkpoint ticker.");
            return;
        }
        boolean autoEnabled = autoRule.get();
        if (!autoEnabled) {
            return;
        }

        // Determine the interval based on whether a random interval is used.
        var useRandomRule = level.getGameRules().getRule(ModGameRules.USE_RANDOM_INTERVAL);
        boolean useRandom = useRandomRule != null && useRandomRule.get();

        if (useRandom && !randomIntervalSelected) {
            // Get the lower and upper bounds from gamerules (in seconds), then convert to ticks.
            var lowerRule = level.getGameRules().getRule(ModGameRules.RANDOM_CHECKPOINT_LOWER_BOUND);
            var upperRule = level.getGameRules().getRule(ModGameRules.RANDOM_CHECKPOINT_UPPER_BOUND);
            int lowerSeconds = lowerRule != null ? lowerRule.get() : 600;
            int upperSeconds = upperRule != null ? upperRule.get() : 1200;
            int lowerTicks = lowerSeconds * 20;
            int upperTicks = upperSeconds * 20;
            if (upperTicks <= lowerTicks) {
                LOGGER.warn("Random checkpoint interval upper bound must be greater than lower bound. Using lower bound as fixed interval.");
                intervalTicks = lowerTicks;
            } else {
                intervalTicks = lowerTicks + ThreadLocalRandom.current().nextInt(upperTicks - lowerTicks);
                randomIntervalSelected = true;
            }
            LOGGER.debug("Using random interval: {} ticks (lower bound: {} ticks, upper bound: {} ticks)", intervalTicks, lowerTicks, upperTicks);
        } else if (!useRandom) {
            // Fixed interval from gamerule (value in seconds)
            var fixedRule = level.getGameRules().getRule(ModGameRules.CHECKPOINT_FIXED_INTERVAL);
            int fixedSeconds = fixedRule != null ? fixedRule.get() : 600;
            intervalTicks = fixedSeconds * 20;
            // LOGGER.debug("Using fixed interval: {} seconds -> {} ticks", fixedSeconds, intervalTicks);
        }

        long currentTick = server.getTickCount();


        // Update the next checkpoint interval if interval has changed.
        if (intervalTicks != nextCheckpointInterval) {

            nextCheckpointInterval = intervalTicks;
            lastCheckpointTick = currentTick;
            return;
        }

        // Initialize the next interval if not set.
        if (nextCheckpointInterval == 0) {
            nextCheckpointInterval = intervalTicks;
            lastCheckpointTick = currentTick;
            return;
        }


        // Log elapsed time every second.
//        if ((currentTick - lastCheckpointTick) % 20 == 0) {
//            LOGGER.info("Time Elapsed: {} seconds", (currentTick - lastCheckpointTick) / 20);
//        }

        // Check if enough ticks have passed since the last checkpoint.
        if (currentTick - lastCheckpointTick >= nextCheckpointInterval) {
            // Retrieve the checkpoint data.
            CheckpointData data = CheckpointData.get(level);
            // If no anchor player is set, assign the first online player.
            if (data.getAnchorPlayerUUID() == null) {
                if (!server.getPlayerList().getPlayers().isEmpty()) {
                    ServerPlayer firstPlayer = server.getPlayerList().getPlayers().get(0);
                    data.setAnchorPlayerUUID(firstPlayer.getUUID());

                } else {
                    LOGGER.warn("No players online to set as anchor.");
                    return;
                }
            }
            // Retrieve the anchor player using the (now set) anchor UUID.
            ServerPlayer anchorPlayer = server.getPlayerList().getPlayer(data.getAnchorPlayerUUID());
            if (anchorPlayer != null) {

                CheckpointManager.setCheckpoint(anchorPlayer);
                randomIntervalSelected = false;
            } else {
                LOGGER.warn("Anchor player not found for UUID: {}", data.getAnchorPlayerUUID());
            }
            // Reset the timer for the next checkpoint.
            lastCheckpointTick = currentTick;
            nextCheckpointInterval = intervalTicks;
            LOGGER.debug("Reset checkpoint timer: next interval {} ticks, new last tick {}", nextCheckpointInterval, lastCheckpointTick);
        }
    }
}
