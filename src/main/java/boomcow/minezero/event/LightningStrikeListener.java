package boomcow.minezero.event;

import boomcow.minezero.checkpoint.CheckpointData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LightningBolt;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

public class LightningStrikeListener {

    @SubscribeEvent
    public static void onLightningStrike(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof LightningBolt) || event.getLevel().isClientSide()) return;

        ServerLevel level = (ServerLevel) event.getLevel();
        CheckpointData data = CheckpointData.get(level);
        if (data != null && data.getWorldData() != null) {
            BlockPos strikePos = event.getEntity().blockPosition();
            long tickTime = level.getGameTime();
            data.getWorldData().addLightningStrike(strikePos, tickTime);
        }
    }
}

