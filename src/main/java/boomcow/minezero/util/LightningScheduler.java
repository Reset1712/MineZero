package boomcow.minezero.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class LightningScheduler {
    private static final List<LightningTask> scheduled = new ArrayList<>();

    public static void schedule(ServerLevel level, BlockPos pos, long tickTime) {
        scheduled.add(new LightningTask(level, pos, tickTime));
    }

    public static void tick(ServerLevel level) {
        long currentTick = level.getGameTime();
        Iterator<LightningTask> iterator = scheduled.iterator();
        while (iterator.hasNext()) {
            LightningTask task = iterator.next();
            if (task.level == level && task.tickTime <= currentTick) {
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
                if (bolt != null) {
                    bolt.moveTo(task.pos.getX(), task.pos.getY(), task.pos.getZ());
                    level.addFreshEntity(bolt);
                }
                iterator.remove();
            }
        }
    }

    public static class LightningTask {
        ServerLevel level;
        BlockPos pos;
        long tickTime;

        public LightningTask(ServerLevel level, BlockPos pos, long tickTime) {
            this.level = level;
            this.pos = pos;
            this.tickTime = tickTime;
        }
    }
}
