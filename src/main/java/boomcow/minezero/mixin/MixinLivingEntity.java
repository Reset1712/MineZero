package boomcow.minezero.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

    @Inject(method = "dropFromLootTable", at = @At("HEAD"))
    private void onDropFromLootTable(DamageSource damageSource, boolean hitByPlayer, CallbackInfo ci) {
        if (this.minezero$lootSeed != 0) {
            this.random.setSeed(this.minezero$lootSeed);
        }
    }
}