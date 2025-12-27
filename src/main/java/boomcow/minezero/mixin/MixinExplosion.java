package boomcow.minezero.mixin;

import boomcow.minezero.checkpoint.CheckpointManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(Explosion.class)
public abstract class MixinExplosion {

    @Shadow @Final private List<BlockPos> affectedBlocks;

    @Inject(method = "affectWorld", at = @At("HEAD"), cancellable = true)
    private void onAffectWorld(boolean particles, CallbackInfo ci) {
        if (CheckpointManager.wasRestoredThisTick) {
            this.affectedBlocks.clear();
            ci.cancel();
        }
    }
}
