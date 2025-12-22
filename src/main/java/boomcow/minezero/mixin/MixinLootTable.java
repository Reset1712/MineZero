package boomcow.minezero.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.function.Consumer;

@Mixin(LootTable.class)
public abstract class MixinLootTable {

    @Shadow
    public abstract void getRandomItems(LootContext context, Consumer<ItemStack> stackConsumer);

    @Shadow @Final private LootContextParamSet paramSet;

    @Inject(method = "getRandomItems(Lnet/minecraft/world/level/storage/loot/LootParams;Ljava/util/function/Consumer;)V", at = @At("HEAD"), cancellable = true)
    private void onGetRandomItems(LootParams params, Consumer<ItemStack> consumer, CallbackInfo ci) {
        Entity entity = params.getOptionalParameter(LootContextParams.THIS_ENTITY);
        if (entity instanceof IMineZeroLootSeed lootEntity) {
            long seed = lootEntity.minezero$getLootSeed();
            if (seed != 0) {
                LootContext.Builder builder = new LootContext.Builder(params);
                builder.withOptionalRandomSeed(seed);
                LootContext context = builder.create(this.paramSet);
                this.getRandomItems(context, consumer);
                ci.cancel();
            }
        }
    }
}