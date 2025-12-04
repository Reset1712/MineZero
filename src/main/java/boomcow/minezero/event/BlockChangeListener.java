package boomcow.minezero.event;

import boomcow.minezero.checkpoint.CheckpointData;
import boomcow.minezero.checkpoint.WorldData;
import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemBucket;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber
public class BlockChangeListener {
    private static final Logger LOGGER_BCL = LogManager.getLogger("MineZeroBCL");

    private static WorldData getActiveWorldData(World level) {
        if (level == null || level.getMinecraftServer() == null) return null;
        // CheckpointData.get handles the World/MapStorage logic
        CheckpointData checkpointData = CheckpointData.get(level);
        return checkpointData.getWorldData();
    }

    @SubscribeEvent
    public static void onSolidBlockPlace(BlockEvent.PlaceEvent event) {
        if (event.getPlayer() instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) event.getPlayer();
            WorldServer level = (WorldServer) player.world;
            WorldData worldDataInstance = getActiveWorldData(level);

            if (worldDataInstance == null) {
                LOGGER_BCL.warn("Could not get active WorldData for block placement tracking.");
                return;
            }

            IBlockState newState = event.getPlacedBlock();
            BlockPos pos = event.getPos().toImmutable();

            // In 1.12, liquids are blocks.
            if (newState.getBlock() instanceof BlockLiquid) return;

            // End Portal Frame Eye check
            if (newState.getBlock() == Blocks.END_PORTAL_FRAME && newState.getValue(BlockEndPortalFrame.EYE)) {
                worldDataInstance.getAddedEyes().add(pos);
                return;
            }

            LOGGER_BCL.info("Block placed at: " + pos + " tracking in instance WorldData.");
            worldDataInstance.getModifiedBlocks().add(pos);
            worldDataInstance.getInstanceBlockDimensionIndices().put(pos, WorldData.getDimensionIndex(level.provider.getDimension()));
        }
    }

    @SubscribeEvent
    public static void onSolidBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) event.getPlayer();
            WorldServer level = (WorldServer) player.world;
            WorldData worldDataInstance = getActiveWorldData(level);

            if (worldDataInstance == null) {
                LOGGER_BCL.warn("Could not get active WorldData for block break tracking.");
                return;
            }

            IBlockState state = event.getState();
            if (!(state.getBlock() instanceof BlockLiquid)) {

                List<BlockPos> brokenBlockPositions = new ArrayList<>();
                BlockPos pos = event.getPos().toImmutable();
                brokenBlockPositions.add(pos);
                Block block = state.getBlock();

                // Multi-block logic (Doors/Beds)
                if (block instanceof BlockDoor) {
                    if (state.getValue(BlockDoor.HALF) == BlockDoor.EnumDoorHalf.LOWER) {
                        brokenBlockPositions.add(pos.up());
                    } else {
                        brokenBlockPositions.add(pos.down());
                    }
                } else if (block instanceof BlockBed) {
                    EnumFacing facing = state.getValue(BlockBed.FACING);
                    if (state.getValue(BlockBed.PART) == BlockBed.EnumPartType.FOOT) {
                        brokenBlockPositions.add(pos.offset(facing));
                    } else {
                        brokenBlockPositions.add(pos.offset(facing.getOpposite()));
                    }
                }

                for (BlockPos currentPos : brokenBlockPositions) {
                    if (worldDataInstance.getModifiedBlocks().contains(currentPos)) {
                        worldDataInstance.getModifiedBlocks().remove(currentPos);
                    } else {
                        IBlockState currentState = level.getBlockState(currentPos);
                        worldDataInstance.getMinedBlocks().put(currentPos, currentState);
                        worldDataInstance.getInstanceBlockDimensionIndices().put(currentPos, WorldData.getDimensionIndex(level.provider.getDimension()));
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBucketRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntityPlayer() instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) event.getEntityPlayer();

        // Ensure we are on server side
        if (event.getWorld().isRemote) return;
        WorldServer level = (WorldServer) event.getWorld();

        WorldData worldDataInstance = getActiveWorldData(level);

        if (worldDataInstance == null) {
            LOGGER_BCL.warn("Could not get active WorldData for bucket interaction tracking.");
            return;
        }

        ItemStack stack = event.getItemStack();
        if (stack.getItem() instanceof ItemBucket) {
            ItemBucket bucketItem = (ItemBucket) stack.getItem();
            BlockPos clickedPos = event.getPos().toImmutable();
            BlockPos targetPos = clickedPos.offset(event.getFace()).toImmutable();

            // In 1.12, ItemBucket uses 'isFull' Block field to determine content.
            // Blocks.AIR means empty bucket.
            Block containedBlock = getContainedBlock(bucketItem);
            boolean isFull = containedBlock != Blocks.AIR;

            if (isFull) {
                // Placing fluid
                worldDataInstance.getModifiedFluidBlocks().add(targetPos);
                worldDataInstance.getInstanceBlockDimensionIndices().put(targetPos, WorldData.getDimensionIndex(level.provider.getDimension()));
                LOGGER_BCL.info("Fluid placed at: " + targetPos + " tracking in instance WorldData.");
            } else {
                // Picking up fluid (Empty Bucket)
                IBlockState fluidStateSource = level.getBlockState(clickedPos);
                BlockPos actualFluidPos = null;

                // Check if the clicked block is a source block
                if (isSourceBlock(fluidStateSource)) {
                    actualFluidPos = clickedPos;
                } else {
                    // Check if the target block is a source block (e.g. clicking side of block against water)
                    IBlockState adjacentState = level.getBlockState(targetPos);
                    if (isSourceBlock(adjacentState)) {
                        actualFluidPos = targetPos;
                    }
                }

                // Fallback check
                if(actualFluidPos == null && isAnyLiquid(level.getBlockState(event.getPos()))){
                    actualFluidPos = event.getPos();
                }

                if (actualFluidPos != null) {
                    IBlockState stateToMine = level.getBlockState(actualFluidPos);
                    if (worldDataInstance.getModifiedFluidBlocks().contains(actualFluidPos)) {
                        worldDataInstance.getModifiedFluidBlocks().remove(actualFluidPos);
                    } else {
                        worldDataInstance.getMinedFluidBlocks().put(actualFluidPos, stateToMine);
                        worldDataInstance.getInstanceBlockDimensionIndices().put(actualFluidPos, WorldData.getDimensionIndex(level.provider.getDimension()));
                    }
                    LOGGER_BCL.info("Fluid picked up from: " + actualFluidPos + " tracking in instance WorldData.");
                }
            }
        }
    }

    // Helper to get contained block from private field in 1.12 or via check
    private static Block getContainedBlock(ItemBucket bucket) {
        if (bucket == Items.WATER_BUCKET) return Blocks.FLOWING_WATER;
        if (bucket == Items.LAVA_BUCKET) return Blocks.FLOWING_LAVA;
        return Blocks.AIR; // Items.BUCKET
    }

    // Helper to check if a state is a liquid source block
    private static boolean isSourceBlock(IBlockState state) {
        Block block = state.getBlock();
        // In 1.12, level 0 is source. Works for vanilla liquids.
        return (block == Blocks.WATER || block == Blocks.LAVA || block == Blocks.FLOWING_WATER || block == Blocks.FLOWING_LAVA)
                && state.getValue(BlockLiquid.LEVEL) == 0;
    }

    private static boolean isAnyLiquid(IBlockState state) {
        return state.getBlock() instanceof BlockLiquid;
    }
}