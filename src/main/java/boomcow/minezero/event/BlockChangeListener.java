package boomcow.minezero.event;

import boomcow.minezero.checkpoint.CheckpointData;
import boomcow.minezero.checkpoint.WorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber
public class BlockChangeListener {
    private static final Logger LOGGER_BCL = LogManager.getLogger("MineZeroBCL");
    private static WorldData getActiveWorldData(ServerLevel level) {
        if (level == null || level.getServer() == null) return null;
        CheckpointData checkpointData = CheckpointData.get(level); // Get the global checkpoint data
        return checkpointData.getWorldData(); // Get the WorldData instance from it
    }

    // 1. Solid block placement
    @SubscribeEvent
    public static void onSolidBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerLevel level = (ServerLevel) player.level();
            WorldData worldDataInstance = getActiveWorldData(level); // Get the instance

            if (worldDataInstance == null) {
                LOGGER_BCL.warn("Could not get active WorldData for block placement tracking.");
                return;
            }

            BlockState newState = event.getState();
            BlockPos pos = event.getPos().immutable(); // Ensure immutable
            // int dimensionIndex = WorldData.getGlobalDimensionId(level.dimension()); // Use static utility if needed globally

            if (newState.getBlock() instanceof LiquidBlock) return;

            if (newState.getBlock() == Blocks.END_PORTAL_FRAME && newState.getValue(EndPortalFrameBlock.HAS_EYE)) {
                worldDataInstance.getAddedEyes().add(pos); // Access via instance
                return;
            }

            LOGGER_BCL.info("Block placed at: " + pos + " tracking in instance WorldData.");
            worldDataInstance.getModifiedBlocks().add(pos); // Access via instance
            worldDataInstance.getInstanceBlockDimensionIndices().put(pos, WorldData.getDimensionIndex(level.dimension())); // Access via instance
        }
    }

    // 2. Solid block break
    @SubscribeEvent
    public static void onSolidBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            ServerLevel level = (ServerLevel) player.level();
            WorldData worldDataInstance = getActiveWorldData(level); // Get the instance

            if (worldDataInstance == null) {
                LOGGER_BCL.warn("Could not get active WorldData for block break tracking.");
                return;
            }

            BlockState state = event.getState();
            if (!(state.getBlock() instanceof LiquidBlock)) {
                // int dimensionIndex = WorldData.getGlobalDimensionId(level.dimension());

                List<BlockPos> brokenBlockPositions = new ArrayList<>();
                BlockPos pos = event.getPos().immutable();
                brokenBlockPositions.add(pos);
                Block block = state.getBlock();

                if (block instanceof DoorBlock) {
                    if (state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER) {
                        brokenBlockPositions.add(pos.above());
                    } else {
                        brokenBlockPositions.add(pos.below());
                    }
                } else if (block instanceof BedBlock) {
                    Direction facing = state.getValue(BedBlock.FACING);
                    if (state.getValue(BedBlock.PART) == BedPart.FOOT) {
                        brokenBlockPositions.add(pos.relative(facing));
                    } else {
                        brokenBlockPositions.add(pos.relative(facing.getOpposite()));
                    }
                }

                for (BlockPos currentPos : brokenBlockPositions) {
                    if (worldDataInstance.getModifiedBlocks().contains(currentPos)) { // Access via instance
                        worldDataInstance.getModifiedBlocks().remove(currentPos); // Access via instance
                    } else {
                        BlockState currentState = level.getBlockState(currentPos); // Get fresh state
                        worldDataInstance.getMinedBlocks().put(currentPos, currentState); // Access via instance
                        worldDataInstance.getInstanceBlockDimensionIndices().put(currentPos, WorldData.getDimensionIndex(level.dimension())); // Access via instance
                    }
                }
            }
        }
    }






    @SubscribeEvent
    public static void onBucketRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ServerLevel level = (ServerLevel) event.getLevel(); // Use event.getLevel()
        WorldData worldDataInstance = getActiveWorldData(level); // Get the instance

        if (worldDataInstance == null) {
            LOGGER_BCL.warn("Could not get active WorldData for bucket interaction tracking.");
            return;
        }

        ItemStack stack = event.getItemStack();
        if (stack.getItem() instanceof BucketItem bucketItem) {
            // int dimensionIndex = WorldData.getGlobalDimensionId(level.dimension());
            BlockPos clickedPos = event.getPos().immutable(); // The block that was clicked
            BlockPos targetPos = clickedPos.relative(event.getFace()).immutable(); // The block where fluid might be placed/removed

            if (bucketItem.getFluid() == Fluids.LAVA || bucketItem.getFluid() == Fluids.WATER) { // Placing fluid
                worldDataInstance.getModifiedFluidBlocks().add(targetPos); // Access via instance
                worldDataInstance.getInstanceBlockDimensionIndices().put(targetPos, WorldData.getDimensionIndex(level.dimension()));// Access via instance
                LOGGER_BCL.info("Fluid placed at: " + targetPos + " tracking in instance WorldData.");
            } else if (bucketItem.getFluid() == Fluids.EMPTY) { // Picking up fluid
                // When picking up, the fluid is AT the clickedPos (if it's a source block)
                // or at targetPos if you clicked an adjacent solid block.
                // Minecraft's BucketItem logic targets the block clicked OR the space next to it.
                // Let's check the block directly clicked first, then the adjacent one if the clicked one isn't fluid.
                BlockState fluidStateSource = level.getBlockState(clickedPos);
                BlockPos actualFluidPos = null;

                if (fluidStateSource.getFluidState().isSourceOfType(Fluids.WATER) || fluidStateSource.getFluidState().isSourceOfType(Fluids.LAVA)) {
                    actualFluidPos = clickedPos;
                } else {
                    // If clicked block is not a fluid source, check the adjacent block where fluid would be placed from an empty bucket
                    // This logic might need refinement based on vanilla bucket behavior.
                    // Vanilla bucket pickup primarily targets the block *at* event.getPos()
                    BlockState adjacentState = level.getBlockState(targetPos);
                    if (adjacentState.getFluidState().isSourceOfType(Fluids.WATER) || adjacentState.getFluidState().isSourceOfType(Fluids.LAVA)) {
                        actualFluidPos = targetPos;
                    }
                }

                // If we are trying to pick up from event.getPos() directly
                if(actualFluidPos == null && (level.getBlockState(event.getPos()).getFluidState().is(Fluids.WATER) || level.getBlockState(event.getPos()).getFluidState().is(Fluids.LAVA))){
                    actualFluidPos = event.getPos();
                }


                if (actualFluidPos != null) {
                    BlockState stateToMine = level.getBlockState(actualFluidPos); // Get the actual state
                    if (worldDataInstance.getModifiedFluidBlocks().contains(actualFluidPos)) { // Access via instance
                        worldDataInstance.getModifiedFluidBlocks().remove(actualFluidPos); // Access via instance
                    } else {
                        worldDataInstance.getMinedFluidBlocks().put(actualFluidPos, stateToMine); // Access via instance
                        worldDataInstance.getInstanceBlockDimensionIndices().put(actualFluidPos, WorldData.getDimensionIndex(level.dimension())); // Access via instance
                    }
                    LOGGER_BCL.info("Fluid picked up from: " + actualFluidPos + " tracking in instance WorldData.");
                }
            }
        }
    }
}