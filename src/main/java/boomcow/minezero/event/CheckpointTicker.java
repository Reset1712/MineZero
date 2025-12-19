package boomcow.minezero.event;

import boomcow.minezero.MineZero;
import boomcow.minezero.ModGameRules;
import boomcow.minezero.checkpoint.CheckpointData;
import boomcow.minezero.checkpoint.CheckpointManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@EventBusSubscriber(modid = MineZero.MODID, bus = EventBusSubscriber.Bus.GAME)
public class CheckpointTicker {

    private static final Logger LOGGER = LogManager.getLogger(CheckpointTicker.class);

    public static long lastCheckpointTick = 0;
    private static long nextCheckpointInterval = 0;
    private static boolean randomIntervalSelected = false;
    private static int intervalTicks = 0;

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        lastCheckpointTick = 0;
        nextCheckpointInterval = 0;
        randomIntervalSelected = false;
        intervalTicks = 0;
    }

    @SubscribeEvent
    public static void onServerTickPost(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        ServerLevel level = server.overworld();
        if (level == null) return;

        var autoRule = level.getGameRules().getRule(ModGameRules.AUTO_CHECKPOINT_ENABLED);
        if (autoRule == null || !autoRule.get()) {
            return;
        }

        var useRandomRule = level.getGameRules().getRule(ModGameRules.USE_RANDOM_INTERVAL);
        boolean useRandom = useRandomRule != null && useRandomRule.get();

        if (useRandom && !randomIntervalSelected) {
            var lowerRule = level.getGameRules().getRule(ModGameRules.RANDOM_CHECKPOINT_LOWER_BOUND);
            var upperRule = level.getGameRules().getRule(ModGameRules.RANDOM_CHECKPOINT_UPPER_BOUND);
            int lowerSeconds = lowerRule != null ? lowerRule.get() : 600;
            int upperSeconds = upperRule != null ? upperRule.get() : 1200;

            if (upperSeconds <= lowerSeconds) upperSeconds = lowerSeconds + 1;

            int lowerTicks = lowerSeconds * 20;
            int upperTicks = upperSeconds * 20;

            intervalTicks = lowerTicks + ThreadLocalRandom.current().nextInt(upperTicks - lowerTicks);
            randomIntervalSelected = true;
        } else if (!useRandom) {
            var fixedRule = level.getGameRules().getRule(ModGameRules.CHECKPOINT_FIXED_INTERVAL);
            int fixedSeconds = fixedRule != null ? fixedRule.get() : 600;
            intervalTicks = fixedSeconds * 20;
        }

        long currentTick = server.getTickCount();

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
                if (isPlayerSafe(anchorPlayer)) {
                    CheckpointManager.setCheckpoint(anchorPlayer);
                    randomIntervalSelected = false;
                    lastCheckpointTick = currentTick;
                    nextCheckpointInterval = intervalTicks;
                }
            } else {
                LOGGER.warn("Anchor player not found for UUID: {}", data.getAnchorPlayerUUID());
            }
        }
    }

    private static boolean isPlayerSafe(ServerPlayer player) {
        if (player.isInLava()) {
            return false;
        }

        if (player.fallDistance > 0) {
            double y = player.getY();
            int minHeight = player.level().getMinBuildHeight();
            boolean groundFound = false;
            float expectedDamage = 0;

            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(player.getX(), y, player.getZ());

            for (int i = 0; i < 320; i++) {
                pos.setY((int) (y - i));
                if (pos.getY() < minHeight) break;

                BlockState state = player.level().getBlockState(pos);
                if (!state.isAir()) {
                    if (state.getFluidState().is(FluidTags.LAVA)) {
                        return false;
                    }
                    groundFound = true;
                    if (!state.getFluidState().isEmpty()) {
                        expectedDamage = 0;
                    } else {
                        double distToGround = y - pos.getY();
                        float totalFallDistance = player.fallDistance + (float) distToGround;
                        expectedDamage = Math.max(0, totalFallDistance - 3.0f);
                    }
                    break;
                }
            }

            if (!groundFound) {
                return false;
            }

            if (player.getHealth() - expectedDamage <= 0) {
                return false;
            }
        }

        AABB searchArea = player.getBoundingBox().inflate(10.0);
        List<Entity> dangerousEntities = player.level().getEntities(player, searchArea, e -> {
            if (e instanceof PrimedTnt) return true;
            if (e instanceof Creeper creeper) {
                return creeper.getSwellDir() > 0 || creeper.isIgnited();
            }
            return false;
        });

        if (!dangerousEntities.isEmpty()) {
            return false;
        }

        return true;
    }
}