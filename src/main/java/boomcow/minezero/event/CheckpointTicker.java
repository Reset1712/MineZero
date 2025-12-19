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

    private static long lastCheckpointTick = -1;
    private static int currentRandomIntervalTicks = -1;

    @SubscribeEvent
    public static void onServerTickPost(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        ServerLevel level = server.overworld();
        if (level == null) return;

        if (!level.getGameRules().getBoolean(ModGameRules.AUTO_CHECKPOINT_ENABLED)) {
            return;
        }

        long currentTick = server.getTickCount();

        if (lastCheckpointTick == -1) {
            lastCheckpointTick = currentTick;
            return;
        }

        int requiredIntervalTicks;
        boolean useRandom = level.getGameRules().getBoolean(ModGameRules.USE_RANDOM_INTERVAL);

        if (useRandom) {
            if (currentRandomIntervalTicks == -1) {
                int lowerSeconds = level.getGameRules().getInt(ModGameRules.RANDOM_CHECKPOINT_LOWER_BOUND);
                int upperSeconds = level.getGameRules().getInt(ModGameRules.RANDOM_CHECKPOINT_UPPER_BOUND);

                if (upperSeconds <= lowerSeconds) {
                    upperSeconds = lowerSeconds + 1;
                }

                int randomSeconds = lowerSeconds + ThreadLocalRandom.current().nextInt(upperSeconds - lowerSeconds);
                currentRandomIntervalTicks = randomSeconds * 20;
                LOGGER.debug("Next random checkpoint interval: {} seconds", randomSeconds);
            }
            requiredIntervalTicks = currentRandomIntervalTicks;
        } else {
            currentRandomIntervalTicks = -1;
            requiredIntervalTicks = level.getGameRules().getInt(ModGameRules.CHECKPOINT_FIXED_INTERVAL) * 20;
        }

        if (currentTick - lastCheckpointTick >= requiredIntervalTicks) {
            CheckpointData data = CheckpointData.get(level);

            if (data.getAnchorPlayerUUID() == null) {
                if (!server.getPlayerList().getPlayers().isEmpty()) {
                    ServerPlayer firstPlayer = server.getPlayerList().getPlayers().get(0);
                    data.setAnchorPlayerUUID(firstPlayer.getUUID());
                } else {
                    return;
                }
            }

            ServerPlayer anchorPlayer = server.getPlayerList().getPlayer(data.getAnchorPlayerUUID());
            if (anchorPlayer != null) {
                CheckpointManager.setCheckpoint(anchorPlayer);
                LOGGER.info("Auto-checkpoint created for anchor: {}", anchorPlayer.getName().getString());

                lastCheckpointTick = currentTick;
                currentRandomIntervalTicks = -1;
            } else {
                LOGGER.warn("Anchor player UUID {} not found online. Skipping checkpoint but keeping timer ready.", data.getAnchorPlayerUUID());
            }
        }
    }
}