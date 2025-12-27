package boomcow.minezero.event;

import boomcow.minezero.ModGameRules;
import boomcow.minezero.checkpoint.CheckpointData;
import boomcow.minezero.checkpoint.CheckpointManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadLocalRandom;

public class CheckpointTicker {

    private static final Logger LOGGER = LoggerFactory.getLogger(CheckpointTicker.class);
    public static long lastCheckpointTick = 0;
    private static long nextCheckpointInterval = 0;
    private static boolean randomIntervalSelected = false;
    private static int intervalTicks = 0;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            onServerTickPost(server);
        });
    }

    private static void onServerTickPost(MinecraftServer server) {
        ServerWorld world = server.getOverworld();
        if (world == null) return;

        GameRules gameRules = world.getGameRules();
        boolean autoEnabled = gameRules.getBoolean(ModGameRules.AUTO_CHECKPOINT_ENABLED);
        
        if (!autoEnabled) {
            return;
        }

        boolean useRandom = gameRules.getBoolean(ModGameRules.USE_RANDOM_INTERVAL);

        if (useRandom && !randomIntervalSelected) {
            int lowerSeconds = gameRules.getInt(ModGameRules.RANDOM_CHECKPOINT_LOWER_BOUND);
            int upperSeconds = gameRules.getInt(ModGameRules.RANDOM_CHECKPOINT_UPPER_BOUND);
            
            if (lowerSeconds <= 0) lowerSeconds = 600;
            if (upperSeconds <= 0) upperSeconds = 1200;

            int lowerTicks = lowerSeconds * 20;
            int upperTicks = upperSeconds * 20;

            if (upperTicks <= lowerTicks) {
                intervalTicks = lowerTicks;
            } else {
                intervalTicks = lowerTicks + ThreadLocalRandom.current().nextInt(upperTicks - lowerTicks);
                randomIntervalSelected = true;
            }
        } else if (!useRandom) {
            int fixedSeconds = gameRules.getInt(ModGameRules.CHECKPOINT_FIXED_INTERVAL);
            if (fixedSeconds <= 0) fixedSeconds = 600;
            intervalTicks = fixedSeconds * 20;
        }

        long currentTick = server.getTicks();

        if (intervalTicks != nextCheckpointInterval) {
            nextCheckpointInterval = intervalTicks;
            if (lastCheckpointTick == 0) {
                lastCheckpointTick = currentTick;
            }
            return;
        }

        if (nextCheckpointInterval == 0) {
            nextCheckpointInterval = intervalTicks;
            lastCheckpointTick = currentTick;
            return;
        }

        if (currentTick - lastCheckpointTick >= nextCheckpointInterval) {
            CheckpointData data = CheckpointData.get(world);
            
            if (data.getAnchorPlayerUUID() == null) {
                if (!server.getPlayerManager().getPlayerList().isEmpty()) {
                    ServerPlayerEntity firstPlayer = server.getPlayerManager().getPlayerList().get(0);
                    data.setAnchorPlayerUUID(firstPlayer.getUuid());
                } else {
                    return;
                }
            }

            ServerPlayerEntity anchorPlayer = server.getPlayerManager().getPlayer(data.getAnchorPlayerUUID());
            if (anchorPlayer != null) {
                CheckpointManager.setCheckpoint(anchorPlayer);
                randomIntervalSelected = false;
                lastCheckpointTick = currentTick;
            } else {
                LOGGER.warn("Anchor player not found for UUID: {}", data.getAnchorPlayerUUID());
            }
        }
    }
}
