package boomcow.minezero.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class MixinDebugAI extends Entity {

    @Unique
    private static final Logger DEBUG_LOGGER = LogManager.getLogger("MineZero-Debug");

    public MixinDebugAI(net.minecraft.world.entity.EntityType<?> type, net.minecraft.world.level.Level level) {
        super(type, level);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if (this.level().isClientSide) return;

        if (this.hasCustomName() && "Debug".equals(this.getCustomName().getString())) {
            DEBUG_LOGGER.info("Tick: {} | UUID: {} | EntityID: {} | Pos: {}, {}, {} | HeadRot: {}", 
                this.level().getGameTime(),
                this.getUUID(),
                this.getId(),
                String.format("%.2f", this.getX()),
                String.format("%.2f", this.getY()),
                String.format("%.2f", this.getZ()),
                String.format("%.2f", this.getYHeadRot())
            );
        }
    }
}