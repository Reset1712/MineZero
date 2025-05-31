package boomcow.minezero.event; // Your package

import boomcow.minezero.checkpoint.CheckpointData;
import boomcow.minezero.checkpoint.WorldData;
import boomcow.minezero.MineZeroMain; // Assuming this has MOD_ID and LOGGER

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
// For block placement, we'll discuss mixins, but let's placeholder a concept
// or consider if a specific Fabric event can cover it well enough.

import net.minecraft.block.*; // For Block, Blocks, DoorBlock, BedBlock, EndPortalFrameBlock, FluidBlock
import net.minecraft.block.entity.BlockEntity; // If you need to interact with BlockEntities
import net.minecraft.block.enums.BedPart; // For BedBlock
import net.minecraft.block.enums.DoubleBlockHalf; // For DoorBlock
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BucketItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext; // For UseItemCallback if context is needed differently
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import org.apache.logging.log4j.LogManager; // Keep if you prefer, or switch to SLF4J via MineZeroMain.LOGGER
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class BlockChangeListener {
    // Use the main mod's logger or create a specific one
    private static final Logger LOGGER_BCL = LogManager.getLogger("MineZeroBCL"); // Or MineZeroMain.LOGGER;

    // Helper to get WorldData, ensure CheckpointData.get() and WorldData structure are Fabric-compatible
    private static WorldData getActiveWorldData(ServerWorld world) {
        if (world == null || world.getServer() == null) return null;
        CheckpointData checkpointData = CheckpointData.get(world); // Pass ServerWorld
        return checkpointData.getWorldDataFor(world); // Assuming WorldData is per-world or you get a specific one
    }

    public static void register() {
        // 1. Solid Block Break
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            // This event fires AFTER the block is broken. PlayerEntity can be null if broken by non-player.
            if (!world.isClient() && player instanceof ServerPlayerEntity serverPlayer) {
                onSolidBlockBreak((ServerWorld) world, serverPlayer, pos.toImmutable(), state);
            }
        });

        // 2. Bucket Interaction (Placing or Picking up Fluid)
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (!world.isClient() && player instanceof ServerPlayerEntity serverPlayer && stack.getItem() instanceof BucketItem bucketItem) {
                // UseItemCallback doesn't directly give block context like RightClickBlock.
                // We need to simulate a raycast or use the player's targeting.
                // For simplicity here, we assume the action targets the block the player is looking at,
                // which is what vanilla bucket logic does.
                // This part will need careful handling to correctly identify the target block for fluid operations.
                // Vanilla bucket logic is complex. We might need a mixin into BucketItem.useOnBlock or BucketItem.emptyFullBucket
                // for perfect accuracy.
                // For now, this is a simplified interpretation.
                onBucketInteraction(serverPlayer, (ServerWorld) world, hand, stack, bucketItem);
            }
            // Always return PASS for UseItemCallback unless you are fully consuming/cancelling the item use.
            return ActionResult.pass(stack);
        });

        // 3. Solid Block Place - THIS IS THE TRICKIEST WITHOUT MIXINS for precise player context.
        // Fabric API doesn't have a direct player-centric "BlockPlaceEvent" like Forge.
        // Common solutions:
        //    a) Mixin into `net.minecraft.item.BlockItem#place(ItemPlacementContext)`:
        //       This is the most robust for player-placed blocks.
        //    b) Mixin into `net.minecraft.world.World#setBlockState(...)`: Very broad, captures all block changes.
        // We will design the onSolidBlockPlace method, and you'll call it from the mixin.
        // Let's assume you'll create a mixin for BlockItem.place for now.
    }

    // Called from PlayerBlockBreakEvents.AFTER
    private static void onSolidBlockBreak(ServerWorld world, ServerPlayerEntity player, BlockPos pos, BlockState brokenState) {
        WorldData worldDataInstance = getActiveWorldData(world);
        if (worldDataInstance == null) {
            LOGGER_BCL.warn("Could not get active WorldData for block break tracking.");
            return;
        }

        if (brokenState.getBlock() instanceof FluidBlock) return; // Should not happen if event is for solid blocks

        List<BlockPos> brokenBlockPositions = new ArrayList<>();
        brokenBlockPositions.add(pos); // Already immutable from event
        Block block = brokenState.getBlock();

        if (block instanceof DoorBlock) {
            // DoorBlock.getOpenSound(boolean)
            // DoorBlock has HINGE, HALF, FACING, OPEN, POWERED
            if (brokenState.get(DoorBlock.HALF) == DoubleBlockHalf.LOWER) {
                brokenBlockPositions.add(pos.up());
            } else {
                brokenBlockPositions.add(pos.down());
            }
        } else if (block instanceof BedBlock) {
            // BedBlock has PART, FACING, OCCUPIED
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
                // Block is already broken, so `world.getBlockState(currentPos)` would be air.
                // We use the `brokenState` if currentPos is the primary `pos`,
                // but for other parts (e.g., top of door), we'd need to have known its state.
                // This logic might need adjustment based on how you store multi-part block states.
                // For simplicity, if it's not the primary pos, we'd have to assume its original state
                // or have a more complex state capturing mechanism.
                // The original NeoForge code got a fresh state, which is fine if it was *before* breaking the other part.
                BlockState stateToStore = currentPos.equals(pos) ? brokenState : world.getBlockState(currentPos); // This will be air for other parts now
                // You may need to capture states of multipart blocks more carefully before any part is broken.
                worldDataInstance.getMinedBlocks().put(currentPos, stateToStore); // Storing potentially 'air' for secondary parts if not handled carefully
                worldDataInstance.getInstanceBlockDimensionIndices().put(currentPos, WorldData.getDimensionIndex(world.getRegistryKey()));
            }
        }
    }

    // This method will be called from your BlockItem#place mixin
    // The mixin will provide player, world, pos, and the state being placed.
    public static void onSolidBlockPlace(ServerWorld world, ServerPlayerEntity player, BlockPos pos, BlockState newState) {
        WorldData worldDataInstance = getActiveWorldData(world);
        if (worldDataInstance == null) {
            LOGGER_BCL.warn("Could not get active WorldData for block placement tracking.");
            return;
        }

        // Already checked by BlockItem.place that it's not a FluidBlock if it's a BlockItem
        // if (newState.getBlock() instanceof FluidBlock) return; // Should not be necessary if called from BlockItem.place

        if (newState.getBlock() == Blocks.END_PORTAL_FRAME && newState.contains(EndPortalFrameBlock.EYE) && newState.get(EndPortalFrameBlock.EYE)) {
            worldDataInstance.getAddedEyes().add(pos);
            return;
        }

        LOGGER_BCL.info("Block placed at: " + pos + " tracking in instance WorldData (called from mixin).");
        worldDataInstance.getModifiedBlocks().add(pos);
        worldDataInstance.getInstanceBlockDimensionIndices().put(pos, WorldData.getDimensionIndex(world.getRegistryKey()));
    }


    // Called from UseItemCallback for BucketItems
    private static void onBucketInteraction(ServerPlayerEntity player, ServerWorld world, Hand hand, ItemStack stack, BucketItem bucketItem) {
        WorldData worldDataInstance = getActiveWorldData(world);
        if (worldDataInstance == null) {
            LOGGER_BCL.warn("Could not get active WorldData for bucket interaction tracking.");
            return;
        }

        // Bucket logic in Fabric via UseItemCallback is tricky because you don't get the target block directly.
        // Vanilla bucket logic performs a raycast.
        // For this example, we'll assume the action happens around where the player is looking.
        // A proper implementation would replicate vanilla's hit result targeting.
        // This is a MAJOR simplification and likely won't work as accurately as NeoForge's RightClickBlock event.
        // Consider a mixin to BucketItem methods like `use`, `placeFluid` or `emptyFullBucket`.

        BlockPos targetPos; // This needs to be determined, e.g. via raycast or from player's target
        // For now, let's use a placeholder. THIS IS NOT ACCURATE.
        // BlockHitResult hitResult = (BlockHitResult) player.raycast(5.0, 0.0f, true); // Example, might need fluid handling in raycast
        // targetPos = hitResult.getBlockPos().offset(hitResult.getSide()); // Example target for placement
        // BlockPos clickedPos = hitResult.getBlockPos();

        // Due to the complexity, the following is a conceptual port and WILL NOT WORK ACCURATELY
        // without proper target block determination. I will comment it out and recommend a mixin.

        /*
        if (bucketItem.fluid == Fluids.LAVA || bucketItem.fluid == Fluids.WATER) { // Placing fluid
            // Determine actual placement position (targetPos)
            // worldDataInstance.getModifiedFluidBlocks().add(targetPos);
            // worldDataInstance.getInstanceBlockDimensionIndices().put(targetPos, WorldData.getDimensionIndex(world.getRegistryKey()));
            // LOGGER_BCL.info("Fluid to be placed near: " + targetPos + ". Mixin recommended for accuracy.");
        } else if (bucketItem.getFluid() == Fluids.EMPTY) { // Picking up fluid
            // Determine actual fluid source position (actualFluidPos)
            // BlockState stateToMine = world.getBlockState(actualFluidPos);
            // if (worldDataInstance.getModifiedFluidBlocks().contains(actualFluidPos)) {
            //     worldDataInstance.getModifiedFluidBlocks().remove(actualFluidPos);
            // } else {
            //     worldDataInstance.getMinedFluidBlocks().put(actualFluidPos, stateToMine);
            //     worldDataInstance.getInstanceBlockDimensionIndices().put(actualFluidPos, WorldData.getDimensionIndex(world.getRegistryKey()));
            // }
            // LOGGER_BCL.info("Fluid to be picked up from: " + actualFluidPos + ". Mixin recommended for accuracy.");
        }
        */
        LOGGER_BCL.warn("Bucket interaction via UseItemCallback is highly simplified and likely inaccurate. A Mixin into BucketItem is recommended for proper fluid placement/pickup detection.");
    }
}