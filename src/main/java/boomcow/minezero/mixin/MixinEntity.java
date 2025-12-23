package boomcow.minezero.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(Entity.class)
public abstract class MixinEntity {

    @Shadow
    public RandomSource random;

    @Shadow
    public abstract UUID getUUID();

    @Inject(method = "load", at = @At("TAIL"))
    private void onLoad(CompoundTag tag, CallbackInfo ci) {
        UUID uuid = this.getUUID();
        if (uuid != null) {
            long seed = uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits();
            this.random = RandomSource.create(seed);
        }
    }
}