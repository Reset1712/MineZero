package boomcow.minezero.event;

import boomcow.minezero.checkpoint.CheckpointData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;


public class ExplosionEventHandler {

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        ServerLevel level = (ServerLevel) event.getLevel();
        CheckpointData data = CheckpointData.get(level);
        if (data == null || data.getAnchorPlayerUUID() == null) return;
        ServerPlayer anchorPlayer = level.getServer().getPlayerList().getPlayer(data.getAnchorPlayerUUID());
        if (anchorPlayer == null || anchorPlayer.isDeadOrDying()) {
            event.getAffectedBlocks().clear();
        }
    }
}
