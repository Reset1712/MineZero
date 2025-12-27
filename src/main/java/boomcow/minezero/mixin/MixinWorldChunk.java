package boomcow.minezero.mixin;

import boomcow.minezero.checkpoint.CheckpointData;
import boomcow.minezero.checkpoint.CheckpointManager;
import boomcow.minezero.checkpoint.WorldData;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldChunk.class)
public abstract class MixinWorldChunk {

    @Shadow public abstract World getWorld();

    @Shadow public abstract ChunkStatus getStatus();

    @Inject(method = "setBlockState", at = @At("HEAD"))
    private void onSetBlockState(BlockPos pos, BlockState newState, boolean moved, CallbackInfoReturnable<BlockState> cir) {
        World world = this.getWorld();

        if (world.isClient || !(world instanceof ServerWorld serverWorld)) {
            return;
        }

        ChunkStatus status = this.getStatus();
        if (status != null && !status.isAtLeast(ChunkStatus.FULL)) {
            return;
        }

        if (CheckpointManager.isRestoring) {
            return;
        }

        CheckpointData data = CheckpointData.get(serverWorld);
        WorldData worldData = data.getWorldData();

        if (worldData.getCheckpointTick() == 0) {
            return;
        }

        long now = serverWorld.getTime();

        if (now >= worldData.getCheckpointTick()) {
            BlockPos immutablePos = pos.toImmutable();
            BlockState oldState = world.getBlockState(immutablePos);

            if (!oldState.equals(newState)) {
                if (!worldData.getMinedBlocks().containsKey(immutablePos) && !worldData.getModifiedBlocks().contains(immutablePos)) {
                    worldData.getMinedBlocks().put(immutablePos, oldState);
                    worldData.getInstanceBlockDimensionIndices().put(immutablePos, WorldData.getDimensionIndex(serverWorld.getRegistryKey()));

                    if (!worldData.getBlockEntityData().containsKey(immutablePos)) {
                        BlockEntity be = world.getBlockEntity(immutablePos);
                        if (be != null) {
                            NbtCompound tag = be.createNbtWithId(world.getRegistryManager());
                            worldData.saveBlockEntity(immutablePos, tag);
                        }
                    }
                }

                boolean added = worldData.getModifiedBlocks().add(immutablePos);

                if (added) {
                    data.markDirty();
                }
            }
        }
    }
}
