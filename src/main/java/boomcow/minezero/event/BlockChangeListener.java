package boomcow.minezero.event;

import boomcow.minezero.checkpoint.WorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod.EventBusSubscriber
public class BlockChangeListener {



    // 1. Solid block placement
    @SubscribeEvent
    public static void onSolidBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BlockState state = event.getState();
            // Only handle non-fluid placements.
            if (!(state.getBlock() instanceof LiquidBlock)) {
                BlockPos pos = event.getPos();
                ServerLevel level = (ServerLevel) player.level();
                int dimensionIndex = WorldData.getDimensionIndex(level.dimension());
                WorldData.modifiedBlocks.add(pos);
                WorldData.blockDimensionIndices.put(pos, dimensionIndex);
            }
        }
    }

    // 2. Solid block break
    @SubscribeEvent
    public static void onSolidBlockBreak(BlockEvent.BreakEvent event) {
        Logger logger = LogManager.getLogger();
        if (event.getPlayer() instanceof ServerPlayer player) {
            BlockState state = event.getState();
            // Only handle non-fluid breaks.
            if (!(state.getBlock() instanceof LiquidBlock)) {
                BlockPos pos = event.getPos();
                ServerLevel level = (ServerLevel) player.level();
                int dimensionIndex = WorldData.getDimensionIndex(level.dimension());
                if (!WorldData.blockPositions.contains(pos)) {
                    WorldData.minedBlocks.put(pos, state);
                    WorldData.blockDimensionIndices.put(pos, dimensionIndex);
                    logger.debug("broke block...");
                    logger.debug(WorldData.minedBlocks.size());
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBucketRightClick(PlayerInteractEvent.RightClickBlock event) {
        // Use getEntity() instead of getPlayer()
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // Check if the held item is a bucket.
        ItemStack stack = event.getItemStack();
        if (stack.getItem() instanceof BucketItem bucketItem) {
            // Check if the bucket contains water or lava.
            if (bucketItem.getFluid() == Fluids.LAVA || bucketItem.getFluid() == Fluids.WATER) {
                // Fluid will be placed at the adjacent block in the direction of the clicked face.
                BlockPos targetPos = event.getPos().relative(event.getFace());

                // Get the server level using getLevel()
                ServerLevel level = (ServerLevel) event.getLevel();
                int dimensionIndex = WorldData.getDimensionIndex(level.dimension());

                // Retrieve the target block state and then its material.
                BlockState targetState = level.getBlockState(targetPos);

                // Record this fluid placement in a dedicated fluid set.
                WorldData.modifiedFluidBlocks.add(targetPos);
                WorldData.blockDimensionIndices.put(targetPos, dimensionIndex);
            }
        }

    }
}
