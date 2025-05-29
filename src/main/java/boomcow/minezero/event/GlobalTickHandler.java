package boomcow.minezero.event;

import boomcow.minezero.util.LightningScheduler;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;


public class GlobalTickHandler {

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {

        for (ServerLevel level : ServerLifecycleHooks.getCurrentServer().getAllLevels()) {
            LightningScheduler.tick(level);
        }

    }
}
