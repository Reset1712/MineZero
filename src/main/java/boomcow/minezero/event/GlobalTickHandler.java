package boomcow.minezero.event;

import boomcow.minezero.util.LightningScheduler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

@Mod.EventBusSubscriber
public class GlobalTickHandler {

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            // Get the server instance safely
            MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();

            // Ensure server is running
            if (server != null) {
                // In 1.12.2, 'server.worlds' is an array of all loaded WorldServer instances
                for (WorldServer level : server.worlds) {
                    LightningScheduler.tick(level);
                }
            }
        }
    }
}