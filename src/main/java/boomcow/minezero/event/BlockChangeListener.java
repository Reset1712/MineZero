package boomcow.minezero.event;

import boomcow.minezero.checkpoint.WorldData;
import net.minecraft.core.BlockPos;
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
        if (event.getEntity() instanceof ServerPlayer) {
            BlockPos pos = event.getPos();
            WorldData.modifiedBlocks.add(pos); // Track placed blocks
            Logger logger = LogManager.getLogger();
            logger.info("Block placed at " + pos);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer) {
            BlockPos pos = event.getPos();
            BlockState state = event.getState();

            // Only add to minedBlocks if it wasn't in the original saved positions
            if (!WorldData.blockPositions.contains(pos)) {
                WorldData.minedBlocks.put(pos, state); // Store block and original state
            }
        }
    }
}