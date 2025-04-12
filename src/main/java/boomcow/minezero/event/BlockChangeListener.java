package boomcow.minezero.event;

import boomcow.minezero.checkpoint.WorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LiquidBlock;
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
                ServerLevel level = (ServerLevel) player.level();
                int dimensionIndex = WorldData.getDimensionIndex(level.dimension());

                // Create a list to hold the positions of the block being broken and its related parts.
                List<BlockPos> brokenBlockPositions = new ArrayList<>();
                BlockPos pos = event.getPos();
                brokenBlockPositions.add(pos);
                Block block = state.getBlock();

                // Check if the block is part of a multiblock structure.
                // For doors (which consist of an upper and lower half):
                if (block instanceof DoorBlock) {
                    // DoorBlock uses the "HALF" property from DoubleBlockHalf (LOWER/UPPER)
                    if (state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER) {
                        // If this is the lower half, add the block above (upper half).
                        brokenBlockPositions.add(pos.above());
                    } else {
                        // If this is the upper half, add the block below (lower half).
                        brokenBlockPositions.add(pos.below());
                    }
                }
                // For beds (which have a head and a foot):
                else if (block instanceof BedBlock) {
                    // BedBlock uses the "PART" property (HEAD/FOOT) and the "FACING" property to determine layout.
                    Direction facing = state.getValue(BedBlock.FACING);
                    if (state.getValue(BedBlock.PART) == BedPart.FOOT) {
                        // If this is the foot, then the head is in the facing direction.
                        brokenBlockPositions.add(pos.relative(facing));
                    } else {
                        // If this is the head, the foot is in the opposite direction.
                        brokenBlockPositions.add(pos.relative(facing.getOpposite()));
                    }
                }

                // Process each block position (the original block and any additional parts).
                for (BlockPos currentPos : brokenBlockPositions) {
                    if (WorldData.modifiedBlocks.contains(currentPos)) {
                        // If the block was placed after the checkpoint, remove it from the modified set.
                        WorldData.modifiedBlocks.remove(currentPos);
                    } else {
                        // Otherwise, record the block break normally.
                        // Get the current state at that position from the level.
                        BlockState currentState = level.getBlockState(currentPos);
                        WorldData.minedBlocks.put(currentPos, currentState);
                        WorldData.blockDimensionIndices.put(currentPos, dimensionIndex);
                    }
                }
            }
        }
    }






    @SubscribeEvent
    public static void onBucketRightClick(PlayerInteractEvent.RightClickBlock event) {
        // Use getEntity() instead of getPlayer()
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Logger logger = LogManager.getLogger();
        ItemStack stack = event.getItemStack();
        if (stack.getItem() instanceof BucketItem bucketItem) {
            ServerLevel level = (ServerLevel) event.getLevel();
            int dimensionIndex = WorldData.getDimensionIndex(level.dimension());

            // Check if the bucket contains a fluid (for placement)
            if (bucketItem.getFluid() == Fluids.LAVA || bucketItem.getFluid() == Fluids.WATER) {
                // Fluid will be placed at the adjacent block in the direction of the clicked face.
                BlockPos targetPos = event.getPos().relative(event.getFace());
                WorldData.modifiedFluidBlocks.add(targetPos);
                WorldData.blockDimensionIndices.put(targetPos, dimensionIndex);
            }
            // Otherwise, if the bucket is empty, check if we are picking up a fluid.
            else if (bucketItem.getFluid() == Fluids.EMPTY) {
                // For fluid pickup, check the clicked block.

                BlockPos targetPos = event.getPos().relative(event.getFace());
                BlockState state = level.getBlockState(targetPos);
                // Check if the fluid state of the block is either water or lava.
                if (state.getFluidState().is(Fluids.WATER) || state.getFluidState().is(Fluids.LAVA)) {
                    if (WorldData.modifiedFluidBlocks.contains(targetPos)) {
                        WorldData.modifiedFluidBlocks.remove(targetPos);
                    } else {
                        // Record this fluid pickup as a mined fluid block.
                        WorldData.minedFluidBlocks.put(targetPos, state);
                        WorldData.blockDimensionIndices.put(targetPos, dimensionIndex);
                    }
                }
            }
        }
    }

}
