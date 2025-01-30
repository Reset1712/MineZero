package boomcow.minezero.event;

import boomcow.minezero.checkpoint.WorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod.EventBusSubscriber
public class BlockChangeListener {

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BlockPos pos = event.getPos();
            ServerLevel level = (ServerLevel) player.level();
            int dimensionIndex = WorldData.getDimensionIndex(level.dimension());

            WorldData.modifiedBlocks.add(pos);
            WorldData.blockDimensionIndices.put(pos, dimensionIndex); // Store dimension index


        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            BlockPos pos = event.getPos();
            BlockState state = event.getState();
            ServerLevel level = (ServerLevel) player.level();
            int dimensionIndex = WorldData.getDimensionIndex(level.dimension());

            if (!WorldData.blockPositions.contains(pos)) {
                WorldData.minedBlocks.put(pos, state);
                WorldData.blockDimensionIndices.put(pos, dimensionIndex); // Store dimension index
            }
        }
    }
}