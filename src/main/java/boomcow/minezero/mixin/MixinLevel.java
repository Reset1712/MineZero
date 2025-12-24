package boomcow.minezero.mixin;

import boomcow.minezero.util.ILevelRandomAccessor;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Level.class)
public abstract class MixinLevel implements ILevelRandomAccessor {

    @Shadow @Final @Mutable
    public RandomSource random;

    @Override
    public void minezero$setRandomSeed(long seed) {
        this.random.setSeed(seed);
    }
}