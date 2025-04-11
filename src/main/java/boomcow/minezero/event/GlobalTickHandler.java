package boomcow.minezero.event;

import boomcow.minezero.util.LightningScheduler;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

@Mod.EventBusSubscriber
public class GlobalTickHandler {

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            for (ServerLevel level : ServerLifecycleHooks.getCurrentServer().getAllLevels()) {
                LightningScheduler.tick(level);
            }
        }
    }
}
