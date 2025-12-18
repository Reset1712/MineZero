package boomcow.minezero.mixin;

import boomcow.minezero.checkpoint.CheckpointData;
import boomcow.minezero.checkpoint.CheckpointManager;
import boomcow.minezero.checkpoint.WorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

@Mixin(LevelChunk.class)
public abstract class MixinLevelChunk {

    private static final Logger LOGGER = LogUtils.getLogger();

    @Shadow
    public abstract Level getLevel();

    @Shadow public abstract ChunkStatus getPersistedStatus();

    @Inject(method = "setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;", at = @At("HEAD"))
    private void onSetBlockState(BlockPos pos, BlockState newState, boolean isMoving, CallbackInfoReturnable<BlockState> cir) {
        Level level = this.getLevel();

        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        // --- NEW FIX: IGNORE WORLD GEN ---
        // If the chunk is not fully generated yet (e.g. creating new terrain), ignore these changes.
        // This prevents the "Void World" bug where the mod deletes newly generated chunks upon reset.
        ChunkStatus status = this.getPersistedStatus();
        if (status != null && !status.isOrAfter(ChunkStatus.FULL)) {
            return;
        }
        // ---------------------------------

        if (CheckpointManager.isRestoring) {
            return;
        }

        CheckpointData data = CheckpointData.get(serverLevel);
        WorldData worldData = data.getWorldData();
        
        if (worldData.getCheckpointTick() == 0) {
            return;
        }

        long now = serverLevel.getGameTime();

        if (now >= worldData.getCheckpointTick()) {
            BlockPos immutablePos = pos.immutable();
            BlockState oldState = level.getBlockState(immutablePos);

            if (!oldState.equals(newState)) {
                
                if (!worldData.getMinedBlocks().containsKey(immutablePos) && !worldData.getModifiedBlocks().contains(immutablePos)) {
                    worldData.getMinedBlocks().put(immutablePos, oldState);
                    worldData.getInstanceBlockDimensionIndices().put(immutablePos, WorldData.getDimensionIndex(serverLevel.dimension()));

                    if (!worldData.getBlockEntityData().containsKey(immutablePos)) {
                        BlockEntity be = level.getBlockEntity(immutablePos);
                        if (be != null) {
                            CompoundTag tag = be.saveWithFullMetadata(level.registryAccess());
                            worldData.saveBlockEntity(immutablePos, tag);
                        }
                    }
                }

                boolean added = worldData.getModifiedBlocks().add(immutablePos);
                
                if (added) {
                    data.setDirty();
                }
            }
        }
    }
}