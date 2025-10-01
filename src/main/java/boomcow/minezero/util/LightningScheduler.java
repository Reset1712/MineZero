package boomcow.minezero.util;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class LightningScheduler {
    private static final List<LightningTask> scheduledTasks = new ArrayList<>();

    public static void schedule(ServerWorld world, BlockPos pos, long targetTickTime) {
        synchronized (scheduledTasks) {
            scheduledTasks.add(new LightningTask(world, pos, targetTickTime));
        }
    }

    /**
     * This method should be called regularly, e.g., every server tick for each ServerWorld
     * where lightning strikes might be scheduled.
     * @param world The ServerWorld that is currently ticking.
     */
    public static void tick(ServerWorld world) {
        long currentTick = world.getTime();
        synchronized (scheduledTasks) {
            Iterator<LightningTask> iterator = scheduledTasks.iterator();
            while (iterator.hasNext()) {
                LightningTask task = iterator.next();
                if (task.world == world && task.targetTickTime <= currentTick) {
                    LightningEntity lightningBolt = EntityType.LIGHTNING_BOLT.create(world);
                    if (lightningBolt != null) {
                        lightningBolt.setPosition(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                        world.spawnEntity(lightningBolt);
                    }
                    iterator.remove();
                }
            }
        }
    }
    private static class LightningTask {
        final ServerWorld world;
        final BlockPos pos;
        final long targetTickTime;

        public LightningTask(ServerWorld world, BlockPos pos, long targetTickTime) {
            this.world = world;
            this.pos = pos;
            this.targetTickTime = targetTickTime;
        }
    }
}