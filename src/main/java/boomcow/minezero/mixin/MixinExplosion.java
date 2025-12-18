package boomcow.minezero.mixin;

import boomcow.minezero.checkpoint.CheckpointManager;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Explosion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Explosion.class)
public abstract class MixinExplosion {

    @Shadow @Final private ObjectArrayList<BlockPos> toBlow;

    @Inject(method = "finalizeExplosion", at = @At("HEAD"), cancellable = true)
    private void onFinalizeExplosion(boolean p_46077_, CallbackInfo ci) {
        if (CheckpointManager.wasRestoredThisTick) {
            this.toBlow.clear();
            ci.cancel();
        }
    }
}