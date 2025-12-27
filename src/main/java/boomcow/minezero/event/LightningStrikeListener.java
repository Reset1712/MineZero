package boomcow.minezero.event;

import boomcow.minezero.checkpoint.CheckpointData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.entity.LightningEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class LightningStrikeListener {

    public static void register() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (world.isClient) return;
            
            if (entity instanceof LightningEntity) {
                ServerWorld serverWorld = (ServerWorld) world;
                CheckpointData data = CheckpointData.get(serverWorld);
                if (data != null) {
                    BlockPos strikePos = entity.getBlockPos();
                    long tickTime = world.getTime();
                    data.getWorldData().addLightningStrike(strikePos, tickTime);
                }
            }
        });
    }
}
