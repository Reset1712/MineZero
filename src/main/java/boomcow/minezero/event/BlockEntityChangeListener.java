package boomcow.minezero.event;

import boomcow.minezero.checkpoint.CheckpointData;
import boomcow.minezero.checkpoint.WorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod.EventBusSubscriber
public class BlockEntityChangeListener {

    private static final Logger LOGGER_BECL = LogManager.getLogger("MineZeroBECL");

    private static WorldData getActiveWorldData(ServerLevel level) {
        if (level == null || level.getServer() == null) {
            return null;
        }
        CheckpointData checkpointData = CheckpointData.get(level);
        if (checkpointData != null && checkpointData.getAnchorPlayerUUID() != null) {
            return checkpointData.getWorldData();
        }
        return null;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerInteractWithBlockEntity(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ServerLevel level = player.serverLevel();
        WorldData worldData = getActiveWorldData(level);

        if (worldData == null) {
            return;
        }

        BlockPos pos = event.getPos();
        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (blockEntity == null) {
            return;
        }

        if (blockEntity instanceof ChestBlockEntity) {
            BlockState blockState = level.getBlockState(pos);
            if (blockState.getBlock() instanceof ChestBlock) {
                Direction connectedDirection = ChestBlock.getConnectedDirection(blockState);

                if (connectedDirection != null) {
                    BlockPos otherChestPos = pos.relative(connectedDirection);
                    BlockEntity otherBlockEntity = level.getBlockEntity(otherChestPos);
                    if (otherBlockEntity instanceof ChestBlockEntity) {
                        LOGGER_BECL.debug("Detected double chest. Caching other half at {}.", otherChestPos);
                        cacheBlockEntityState(otherChestPos, otherBlockEntity, worldData, level);
                    }
                }
            }
        }
        cacheBlockEntityState(pos, blockEntity, worldData, level);
    }

    private static void cacheBlockEntityState(BlockPos pos, BlockEntity blockEntity, WorldData worldData,
            ServerLevel level) {

        BlockPos immutablePos = pos.immutable();

        if (worldData.getBlockEntityData().containsKey(immutablePos)) {
            return;
        }

        LOGGER_BECL.info("First interaction with BlockEntity at {} post-checkpoint. Caching its state.", immutablePos);

        worldData.saveBlockEntity(immutablePos, blockEntity.saveWithFullMetadata());
        LOGGER_BECL.debug("Successfully saved BlockEntity data for {}. NBT: {}", immutablePos,
                blockEntity.saveWithFullMetadata().toString());

        worldData.getInstanceBlockDimensionIndices().put(immutablePos, WorldData.getDimensionIndex(level.dimension()));
    }
}