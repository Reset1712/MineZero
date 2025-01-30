package boomcow.minezero.checkpoint;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.*;

public class WorldData {
//    private Map<BlockPos, BlockState> blockStates;
    private Map<BlockPos, CompoundTag> blockEntityData;
    private Set<ChunkPos> processedChunks = new HashSet<>();
    private List<BlockState> blockStates = new ArrayList<>();
    public static final List<BlockPos> blockPositions = new ArrayList<>();

    public static final Set<BlockPos> modifiedBlocks = new HashSet<>();
    public static final Map<BlockPos, BlockState> minedBlocks = new HashMap<>();

    public Map<BlockPos, BlockState> getMinedBlocks() {
        return minedBlocks;
    }


    public void saveBlockState(BlockPos pos, BlockState state) {
        blockPositions.add(pos);
        blockStates.add(state);
    }

    public List<BlockPos> getBlockPositions() {
        return this.blockPositions;
    }


    private long dayTime;

    public WorldData() {
        this.blockEntityData = new HashMap<>();
        this.dayTime = 0;
    }


    public void saveBlockEntity(BlockPos pos, CompoundTag blockEntityNBT) {
        if (blockEntityNBT != null) {
            this.blockEntityData.put(pos, blockEntityNBT);
        }
    }

    public void saveDayTime(long dayTime) {
        this.dayTime = dayTime;
    }

    public long getDayTime() {
        return this.dayTime;
    }

    public List<BlockState> getBlockStates() {
        return this.blockStates;
    }

    public Map<BlockPos, CompoundTag> getBlockEntityData() {
        return this.blockEntityData;
    }

    public void clearWorldData() {
        this.minedBlocks.clear();
        this.modifiedBlocks.clear();
        this.blockStates.clear();
        this.blockPositions.clear();
        this.processedChunks.clear();
        this.blockEntityData.clear();
    }

    public void saveAllLoadedChunks(ServerLevel level) {
        Logger logger = LogManager.getLogger();
        ServerChunkCache chunkCache = level.getChunkSource();
        int chunkSize = 16; // Chunks are 16x16 blocks

        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            BlockPos playerPos = player.blockPosition();
            int renderDistance = level.getServer().getPlayerList().getViewDistance(); // ✅ Correct method

            int playerChunkX = playerPos.getX() >> 4;
            int playerChunkZ = playerPos.getZ() >> 4;

            // Calculate total chunks expected
            int totalChunks = (2 * renderDistance + 1) * (2 * renderDistance + 1);
            int processedChunkCount = 0; // Track number of processed chunks

            for (int dx = -renderDistance; dx <= renderDistance; dx++) {
                for (int dz = -renderDistance; dz <= renderDistance; dz++) {
                    int chunkX = playerChunkX + dx;
                    int chunkZ = playerChunkZ + dz;

                    // Get the chunk if it's loaded
                    LevelChunk chunk = chunkCache.getChunkNow(chunkX, chunkZ);
                    if (chunk != null) {
                        ChunkPos chunkPos = chunk.getPos();

                        // ✅ Check if we already processed this chunk
                        if (processedChunks.contains(chunkPos)) continue;
                        processedChunks.add(chunkPos); // ✅ Mark as processed

                        // Update progress
//                        processedChunkCount++;
//                        double progress = (double) processedChunkCount / totalChunks * 100;
//                        logger.info(String.format("Saving chunks... Progress: %.2f%% (%d/%d)", progress, processedChunkCount, totalChunks));

                        BlockPos minPos = chunk.getPos().getWorldPosition();
                        BlockPos maxPos = minPos.offset(chunkSize - 1, level.getMaxBuildHeight() - 1, chunkSize - 1);

                        for (int x = minPos.getX(); x <= maxPos.getX(); x++) {
                            for (int z = minPos.getZ(); z <= maxPos.getZ(); z++) {
                                for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++) {
                                    BlockPos pos = new BlockPos(x, y, z);
                                    BlockState state = chunk.getBlockState(pos);
                                    if (!state.isAir()) {
                                        saveBlockState(pos, state);
                                    }
                                }
                            }
                        }

                        for (BlockPos entityPos : chunk.getBlockEntities().keySet()) {
                            BlockEntity blockEntity = chunk.getBlockEntity(entityPos);
                            if (blockEntity != null && !blockEntity.isRemoved()) {
                                saveBlockEntity(entityPos, blockEntity.saveWithFullMetadata());
                            }
                        }
                    }
                }
            }
        }
    }

}