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
        // ... (this helper method is fine as is)
        if (level == null || level.getServer() == null) {
            return null;
        }
        CheckpointData checkpointData = CheckpointData.get(level);
        if (checkpointData != null && checkpointData.getAnchorPlayerUUID() != null) {
            return checkpointData.getWorldData();
        }
        return null;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST) // Keep HIGHEST priority, it's good practice
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

        // --- NEW LOGIC FOR DOUBLE CHESTS ---
        if (blockEntity instanceof ChestBlockEntity) {
            BlockState blockState = level.getBlockState(pos);
            if (blockState.getBlock() instanceof ChestBlock) {
                // Get the direction of the other half of the chest. This returns null if there is no other half.
                Direction connectedDirection = ChestBlock.getConnectedDirection(blockState);

                if (connectedDirection != null) {
                    BlockPos otherChestPos = pos.relative(connectedDirection);
                    BlockEntity otherBlockEntity = level.getBlockEntity(otherChestPos);
                    if (otherBlockEntity instanceof ChestBlockEntity) {
                        // We found the other half. Cache it if it's not already cached.
                        LOGGER_BECL.debug("Detected double chest. Caching other half at {}.", otherChestPos);
                        cacheBlockEntityState(otherChestPos, otherBlockEntity, worldData, level);
                    }
                }
            }
        }
        // --- END OF NEW LOGIC ---

        // Now, cache the state of the block that was actually clicked.
        // The helper function will handle the check and logging.
        cacheBlockEntityState(pos, blockEntity, worldData, level);
    }

    /**
     * Helper function to cache the state of a single BlockEntity if it hasn't been cached yet.
     * This avoids code duplication and makes the main listener cleaner.
     *
     * @param pos The position of the BlockEntity.
     * @param blockEntity The BlockEntity instance.
     * @param worldData The active WorldData.
     * @param level The level the BlockEntity is in.
     */
    private static void cacheBlockEntityState(BlockPos pos, BlockEntity blockEntity, WorldData worldData, ServerLevel level) {
        // Use an immutable position for map keys to ensure safety
        BlockPos immutablePos = pos.immutable();

        // Check if this specific block's state is already cached.
        if (worldData.getBlockEntityData().containsKey(immutablePos)) {
            return; // Already handled, do nothing.
        }

        LOGGER_BECL.info("First interaction with BlockEntity at {} post-checkpoint. Caching its state.", immutablePos);

        // Save the full NBT data of the block entity
        worldData.saveBlockEntity(immutablePos, blockEntity.saveWithFullMetadata());
        LOGGER_BECL.debug("Successfully saved BlockEntity data for {}. NBT: {}", immutablePos, blockEntity.saveWithFullMetadata().toString());

        // Also save its dimension index so it can be restored correctly
        worldData.getInstanceBlockDimensionIndices().put(immutablePos, WorldData.getDimensionIndex(level.dimension()));
    }
}