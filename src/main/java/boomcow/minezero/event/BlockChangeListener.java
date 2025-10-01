package boomcow.minezero.event;

import boomcow.minezero.checkpoint.CheckpointData;
import boomcow.minezero.checkpoint.WorldData;
import boomcow.minezero.MineZeroMain;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;

import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BucketItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class BlockChangeListener {
    private static final Logger LOGGER_BCL = LogManager.getLogger("MineZeroBCL");
    private static WorldData getActiveWorldData(ServerWorld world) {
        if (world == null || world.getServer() == null) return null;
        CheckpointData checkpointData = CheckpointData.get(world);
        return checkpointData.getWorldDataFor(world);
    }

    public static void register() {
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!world.isClient() && player instanceof ServerPlayerEntity serverPlayer) {
                onSolidBlockBreak((ServerWorld) world, serverPlayer, pos.toImmutable(), state);
            }
        });
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (!world.isClient() && player instanceof ServerPlayerEntity serverPlayer && stack.getItem() instanceof BucketItem bucketItem) {
                onBucketInteraction(serverPlayer, (ServerWorld) world, hand, stack, bucketItem);
            }
            return ActionResult.pass(stack);
        });
    }
    private static void onSolidBlockBreak(ServerWorld world, ServerPlayerEntity player, BlockPos pos, BlockState brokenState) {
        WorldData worldDataInstance = getActiveWorldData(world);
        if (worldDataInstance == null) {
            LOGGER_BCL.warn("Could not get active WorldData for block break tracking.");
            return;
        }

        if (brokenState.getBlock() instanceof FluidBlock) return;

        List<BlockPos> brokenBlockPositions = new ArrayList<>();
        brokenBlockPositions.add(pos);
        Block block = brokenState.getBlock();

        if (block instanceof DoorBlock) {
            if (brokenState.get(DoorBlock.HALF) == DoubleBlockHalf.LOWER) {
                brokenBlockPositions.add(pos.up());
            } else {
                brokenBlockPositions.add(pos.down());
            }
        } else if (block instanceof BedBlock) {
            Direction facing = brokenState.get(BedBlock.FACING);
            if (brokenState.get(BedBlock.PART) == BedPart.FOOT) {
                brokenBlockPositions.add(pos.offset(facing));
            } else {
                brokenBlockPositions.add(pos.offset(facing.getOpposite()));
            }
        }

        for (BlockPos currentPos : brokenBlockPositions) {
            if (worldDataInstance.getModifiedBlocks().contains(currentPos)) {
                worldDataInstance.getModifiedBlocks().remove(currentPos);
            } else {
                BlockState stateToStore = currentPos.equals(pos) ? brokenState : world.getBlockState(currentPos);
                worldDataInstance.getMinedBlocks().put(currentPos, stateToStore);
                worldDataInstance.getInstanceBlockDimensionIndices().put(currentPos, WorldData.getDimensionIndex(world.getRegistryKey()));
            }
        }
    }
    public static void onSolidBlockPlace(ServerWorld world, ServerPlayerEntity player, BlockPos pos, BlockState newState) {
        WorldData worldDataInstance = getActiveWorldData(world);
        if (worldDataInstance == null) {
            LOGGER_BCL.warn("Could not get active WorldData for block placement tracking.");
            return;
        }

        if (newState.getBlock() == Blocks.END_PORTAL_FRAME && newState.contains(EndPortalFrameBlock.EYE) && newState.get(EndPortalFrameBlock.EYE)) {
            worldDataInstance.getAddedEyes().add(pos);
            return;
        }

        LOGGER_BCL.info("Block placed at: " + pos + " tracking in instance WorldData (called from mixin).");
        worldDataInstance.getModifiedBlocks().add(pos);
        worldDataInstance.getInstanceBlockDimensionIndices().put(pos, WorldData.getDimensionIndex(world.getRegistryKey()));
    }
    private static void onBucketInteraction(ServerPlayerEntity player, ServerWorld world, Hand hand, ItemStack stack, BucketItem bucketItem) {
        WorldData worldDataInstance = getActiveWorldData(world);
        if (worldDataInstance == null) {
            LOGGER_BCL.warn("Could not get active WorldData for bucket interaction tracking.");
            return;
        }

        BlockPos targetPos;

        /*
        if (bucketItem.fluid == Fluids.LAVA || bucketItem.fluid == Fluids.WATER) {
        } else if (bucketItem.getFluid() == Fluids.EMPTY) {
        }
        */
        LOGGER_BCL.warn("Bucket interaction via UseItemCallback is highly simplified and likely inaccurate. A Mixin into BucketItem is recommended for proper fluid placement/pickup detection.");
    }
}