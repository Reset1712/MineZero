package boomcow.minezero.util;

import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class LightningScheduler {
    private static final List<LightningTask> scheduled = new ArrayList<>();

    // Changing ServerLevel to World allows WorldServer to be passed in easily
    public static void schedule(World level, BlockPos pos, long tickTime) {
        scheduled.add(new LightningTask(level, pos, tickTime));
    }

    public static void tick(World level) {
        long currentTick = level.getTotalWorldTime(); // getGameTime equivalent
        Iterator<LightningTask> iterator = scheduled.iterator();

        while (iterator.hasNext()) {
            LightningTask task = iterator.next();

            // In 1.12, strict object equality (==) for Worlds is generally fine
            // provided the scheduler isn't holding onto old world instances after restarts.
            if (task.level == level && task.tickTime <= currentTick) {

                // 1.12.2: Instantiate directly. boolean is 'effectOnly' (false = does damage)
                EntityLightningBolt bolt = new EntityLightningBolt(level, task.pos.getX(), task.pos.getY(), task.pos.getZ(), false);

                // Specific method for lightning/weather
                level.addWeatherEffect(bolt);

                iterator.remove();
            }
        }
    }

    public static class LightningTask {
        World level;
        BlockPos pos;
        long tickTime;

        public LightningTask(World level, BlockPos pos, long tickTime) {
            this.level = level;
            this.pos = pos;
            this.tickTime = tickTime;
        }
    }
}