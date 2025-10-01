package boomcow.minezero.event;

import boomcow.minezero.checkpoint.CheckpointData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class ExplosionEventHandler {

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        ServerLevel level = (ServerLevel) event.getLevel();

        CheckpointData data = CheckpointData.get(level);
        if (data == null || data.getAnchorPlayerUUID() == null)
            return;

        ServerPlayer anchorPlayer = level.getServer().getPlayerList().getPlayer(data.getAnchorPlayerUUID());
        if (anchorPlayer == null || anchorPlayer.isDeadOrDying()) {

            event.getAffectedBlocks().clear();
        }
    }
}
