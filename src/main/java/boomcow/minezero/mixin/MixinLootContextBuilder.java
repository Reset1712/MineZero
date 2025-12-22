package boomcow.minezero.mixin;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(LootContext.Builder.class)
public class MixinLootContextBuilder {

    @Shadow
    private LootParams params;

    @Shadow
    private RandomSource random;

    @Inject(method = "create", at = @At("HEAD"))
    private void onCreate(Optional<ResourceLocation> sequence, CallbackInfoReturnable<LootContext> cir) {
        Entity entity = this.params.getOptionalParameter(LootContextParams.THIS_ENTITY);
        if (entity instanceof LivingEntity && !(entity instanceof Player)) {
            long seed = entity.getUUID().getMostSignificantBits() ^ entity.getUUID().getLeastSignificantBits();
            this.random = RandomSource.create(seed);
        }
    }
}