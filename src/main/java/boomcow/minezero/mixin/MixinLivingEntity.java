package boomcow.minezero.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.function.Consumer;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity extends Entity {

    public MixinLivingEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Unique
    private long minezero$lootSeed = 0;

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void onAddAdditionalSaveData(CompoundTag compound, CallbackInfo ci) {
        if (this.minezero$lootSeed == 0) {
            this.minezero$lootSeed = this.random.nextLong();
        }
        compound.putLong("MineZeroLootSeed", this.minezero$lootSeed);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void onReadAdditionalSaveData(CompoundTag compound, CallbackInfo ci) {
        if (compound.contains("MineZeroLootSeed")) {
            this.minezero$lootSeed = compound.getLong("MineZeroLootSeed");
        }
    }

    @Redirect(method = "dropFromLootTable", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/storage/loot/LootTable;getRandomItems(Lnet/minecraft/world/level/storage/loot/LootParams;Ljava/util/function/Consumer;)V"))
    private void redirectLootGeneration(LootTable table, LootParams params, Consumer<ItemStack> consumer) {
        if (this.minezero$lootSeed != 0) {
            LootContext.Builder builder = new LootContext.Builder(params);
            builder.withOptionalRandomSeed(this.minezero$lootSeed);
            LootContext context = builder.create(Optional.empty());
            table.getRandomItems(context, consumer);
        } else {
            table.getRandomItems(params, consumer);
        }
    }
}