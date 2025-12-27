package boomcow.minezero.event;

import boomcow.minezero.util.LightningScheduler;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.world.ServerWorld;

public class GlobalTickHandler {

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerWorld world : server.getWorlds()) {
                LightningScheduler.tick(world);
            }
        });
    }
}
