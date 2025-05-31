package boomcow.minezero.util; // Your package

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity; // Yarn mapping
import net.minecraft.server.world.ServerWorld; // Yarn mapping
import net.minecraft.util.math.BlockPos; // Yarn mapping
import net.minecraft.world.World; // For general world reference if needed, ServerWorld for specifics

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList; // Good for concurrent modification if ticking from multiple worlds

public class LightningScheduler {
    // Using CopyOnWriteArrayList can be safer if you might schedule from one thread
    // and tick/remove from another (though server ticks are usually single-threaded per world).
    // For simplicity with server tick, ArrayList is often fine if access is synchronized or managed.
    // If this tick method is only called for one specific ServerWorld at a time, ArrayList is fine.
    // If it could be called concurrently for different worlds and they share this list, consider thread safety.
    // For now, sticking to ArrayList as it's a direct port.
    private static final List<LightningTask> scheduledTasks = new ArrayList<>();
    // If you have multiple server worlds and this scheduler is global, you might need to make `scheduledTasks`
    // a Map<World, List<LightningTask>> or ensure tasks are only processed for the ticking world.
    // The current implementation iterates all tasks and checks world, which is fine.

    public static void schedule(ServerWorld world, BlockPos pos, long targetTickTime) {
        // It's good practice to add to a synchronized list or use a concurrent list if scheduling
        // can happen from multiple threads, though commands/events are usually on the server thread.
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
        long currentTick = world.getTime(); // Yarn mapping for getting current game time

        // Iterate carefully if modifying the list. Using an iterator is good.
        // If using CopyOnWriteArrayList, you can iterate directly without an explicit iterator.remove().
        synchronized (scheduledTasks) { // Synchronize access if modifying
            Iterator<LightningTask> iterator = scheduledTasks.iterator();
            while (iterator.hasNext()) {
                LightningTask task = iterator.next();
                // Only process tasks for the currently ticking world
                if (task.world == world && task.targetTickTime <= currentTick) {
                    LightningEntity lightningBolt = EntityType.LIGHTNING_BOLT.create(world); // Use EntityType.LIGHTNING_BOLT
                    if (lightningBolt != null) {
                        // Set position for LightningEntity. Vec3d.ofBottomCenter(pos) is often good.
                        lightningBolt.setPosition(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                        world.spawnEntity(lightningBolt); // Yarn mapping for adding entity
                    }
                    iterator.remove();
                }
            }
        }
    }

    // Inner class for the task
    private static class LightningTask { // Made private as it's an internal detail
        final ServerWorld world; // Mark as final if not reassigned
        final BlockPos pos;
        final long targetTickTime;

        public LightningTask(ServerWorld world, BlockPos pos, long targetTickTime) {
            this.world = world;
            this.pos = pos;
            this.targetTickTime = targetTickTime;
        }
    }
}