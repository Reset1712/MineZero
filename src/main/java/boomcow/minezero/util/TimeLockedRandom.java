package boomcow.minezero.util;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;

public class TimeLockedRandom implements RandomSource {
    private final Entity entity;
    private final LegacyRandomSource delegate;
    private long lastTick = -1;

    public TimeLockedRandom(Entity entity) {
        this.entity = entity;
        this.delegate = new LegacyRandomSource(0);
    }

    private void updateSeed() {
        if (this.entity.level() != null) {
            long currentTick = this.entity.level().getGameTime();
            if (currentTick != this.lastTick) {
                long seed = (this.entity.getUUID().getMostSignificantBits() ^ this.entity.getUUID().getLeastSignificantBits()) ^ (currentTick * 6364136223846793005L);
                this.delegate.setSeed(seed);
                this.lastTick = currentTick;
            }
        }
    }

    @Override
    public RandomSource fork() {
        return this.delegate.fork();
    }

    @Override
    public PositionalRandomFactory forkPositional() {
        return this.delegate.forkPositional();
    }

    @Override
    public void setSeed(long seed) {
        this.delegate.setSeed(seed);
    }

    @Override
    public int nextInt() {
        updateSeed();
        return this.delegate.nextInt();
    }

    @Override
    public int nextInt(int bound) {
        updateSeed();
        return this.delegate.nextInt(bound);
    }

    @Override
    public long nextLong() {
        updateSeed();
        return this.delegate.nextLong();
    }

    @Override
    public boolean nextBoolean() {
        updateSeed();
        return this.delegate.nextBoolean();
    }

    @Override
    public float nextFloat() {
        updateSeed();
        return this.delegate.nextFloat();
    }

    @Override
    public double nextDouble() {
        updateSeed();
        return this.delegate.nextDouble();
    }

    @Override
    public double nextGaussian() {
        updateSeed();
        return this.delegate.nextGaussian();
    }

    @Override
    public void consumeCount(int count) {
        this.delegate.consumeCount(count);
    }
}