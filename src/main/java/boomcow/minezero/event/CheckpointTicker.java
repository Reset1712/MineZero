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

    private static long lastCheckpointTick = -1;
    private static int currentRandomIntervalTicks = -1;

    // Сбрасываем статику при старте сервера, чтобы не переносить состояние из прошлого мира
    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        lastCheckpointTick = -1;
        currentRandomIntervalTicks = -1;
    }

    @SubscribeEvent
    public static void onServerTickPost(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        ServerLevel level = server.overworld();
        if (level == null) return;

        if (!level.getGameRules().getBoolean(ModGameRules.AUTO_CHECKPOINT_ENABLED)) {
            return;
        }

        long currentTick = server.getTickCount();

        // Если это первый тик или произошел рестарт мира (время пошло назад)
        if (lastCheckpointTick == -1 || currentTick < lastCheckpointTick) {
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
                if (isPlayerSafe(anchorPlayer)) {
                    CheckpointManager.setCheckpoint(anchorPlayer);
                    LOGGER.info("Auto-checkpoint created for anchor: {}", anchorPlayer.getName().getString());

                    lastCheckpointTick = currentTick;
                    currentRandomIntervalTicks = -1;
                } else {
                    // Если небезопасно, пробуем в следующем тике (таймер не сбрасываем)
                    LOGGER.debug("Auto-checkpoint postponed: Anchor player is in an unsafe state.");
                }
            } else {
                LOGGER.warn("Anchor player UUID {} not found online. Skipping checkpoint but keeping timer ready.", data.getAnchorPlayerUUID());
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

            // Увеличили дальность проверки до 512, чтобы доставать до земли с гор
            for (int i = 0; i < 512; i++) {
                pos.setY((int) (y - i));
                if (pos.getY() < minHeight) break;

                BlockState state = player.level().getBlockState(pos);
                if (!state.isAir()) {
                    if (!state.getFluidState().isEmpty()) {
                        if (state.getFluidState().is(FluidTags.LAVA)) {
                            return false; // Падение в лаву
                        }
                        groundFound = true;
                        expectedDamage = 0; // Вода гасит урон
                    } else {
                        groundFound = true;
                        double distToGround = y - pos.getY(); // Более точный расчет
                        float totalFallDistance = player.fallDistance + (float) distToGround;
                        expectedDamage = Math.max(0, totalFallDistance - 3.0f);
                    }
                    break;
                }
            }

            if (!groundFound) {
                // Если земля не найдена даже за 512 блоков (например, void), считаем опасным
                return false; 
            }

            if (player.getHealth() - expectedDamage <= 0) {
                return false; // Смертельное падение
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