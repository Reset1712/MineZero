package boomcow.minezero.event;

import boomcow.minezero.checkpoint.CheckpointData;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber
public class LightningStrikeListener {

    @SubscribeEvent
    public static void onLightningStrike(EntityJoinWorldEvent event) {
        // Check if the entity joining is a Lightning Bolt
        if (!(event.getEntity() instanceof EntityLightningBolt)) return;

        World world = event.getWorld();

        // Ensure server-side only
        if (world.isRemote) return;

        CheckpointData data = CheckpointData.get(world);
        if (data != null && data.getWorldData() != null) {
            // In 1.12, getPosition() returns BlockPos
            BlockPos strikePos = event.getEntity().getPosition();

            // Use getTotalWorldTime() for game time
            long tickTime = world.getTotalWorldTime();

            data.getWorldData().addLightningStrike(strikePos, tickTime);
        }
    }
}