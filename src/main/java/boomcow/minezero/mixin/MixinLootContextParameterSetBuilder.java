package boomcow.minezero.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LootContextParameterSet.Builder.class)
public class MixinLootContextParameterSetBuilder {

    @Inject(method = "build", at = @At("HEAD"))
    private void onBuild(LootContextParameterSet.DynamicDrop dynamicDrop, CallbackInfoReturnable<LootContextParameterSet> cir) {
        LootContextParameterSet.Builder builder = (LootContextParameterSet.Builder) (Object) this;
        ServerWorld world = builder.getWorld();
        Entity entity = builder.getOptional(LootContextParameters.THIS_ENTITY);
        
        if (entity instanceof LivingEntity && !(entity instanceof PlayerEntity)) {
            if (world != null) {
                 // Logic injection here if needed
            }
        }
    }
}
