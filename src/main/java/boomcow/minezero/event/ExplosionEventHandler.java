package boomcow.minezero.event;

import boomcow.minezero.checkpoint.CheckpointData;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber
public class ExplosionEventHandler {

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        World world = event.getWorld();

        // Ensure we are on the server side
        if (world.isRemote || !(world instanceof WorldServer)) return;

        WorldServer level = (WorldServer) world;
        CheckpointData data = CheckpointData.get(level);

        if (data == null || data.getAnchorPlayerUUID() == null) return;

        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) return;

        EntityPlayerMP anchorPlayer = server.getPlayerList().getPlayerByUUID(data.getAnchorPlayerUUID());

        // In 1.12.2, use the 'isDead' field or check health <= 0
        if (anchorPlayer == null || anchorPlayer.isDead || anchorPlayer.getHealth() <= 0.0f) {
            event.getAffectedBlocks().clear();
        }
    }
}