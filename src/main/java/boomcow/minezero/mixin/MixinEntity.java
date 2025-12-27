package boomcow.minezero.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(Entity.class)
public abstract class MixinEntity {

    @Shadow @Final @Mutable
    protected Random random;

    @Shadow public abstract UUID getUuid();

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(EntityType<?> type, World world, CallbackInfo ci) {
        UUID uuid = this.getUuid();
        if (uuid != null) {
            long seed = uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits();
            this.random = Random.create(seed);
        }
    }

    @Inject(method = "readNbt", at = @At("TAIL"))
    private void onReadNbt(NbtCompound nbt, CallbackInfo ci) {
        UUID uuid = this.getUuid();
        if (uuid != null) {
            long seed = uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits();
            this.random = Random.create(seed);
        }
    }
}
