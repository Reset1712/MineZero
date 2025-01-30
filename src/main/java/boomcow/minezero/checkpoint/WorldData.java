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
    private static final Map<Integer, ResourceKey<Level>> dimensionMap = new HashMap<>();
    private static int nextDimensionIndex = 0; // Auto-incrementing index for dimensions

    public static final Set<BlockPos> modifiedBlocks = new HashSet<>();
    public static final Map<BlockPos, BlockState> minedBlocks = new HashMap<>();
    public static final Map<BlockPos, Integer> blockDimensionIndices = new HashMap<>();

    public Map<BlockPos, BlockState> getMinedBlocks() {
        return minedBlocks;
    }

    /**
     * Get or assign an index for a dimension.
     */
    public static int getDimensionIndex(ResourceKey<Level> dimension) {
        for (Map.Entry<Integer, ResourceKey<Level>> entry : dimensionMap.entrySet()) {
            if (entry.getValue().equals(dimension)) {
                return entry.getKey();
            }
        }
        int newIndex = nextDimensionIndex++;
        dimensionMap.put(newIndex, dimension);
        return newIndex;
    }

    public static ResourceKey<Level> getDimensionFromIndex(int index) {
        return dimensionMap.get(index);
    }
    public void saveBlockState(BlockPos pos, BlockState state, ResourceKey<Level> dimension) {
        blockPositions.add(pos);
        blockStates.add(state);
        blockDimensionIndices.put(pos, getDimensionIndex(dimension));
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
        minedBlocks.clear();
        modifiedBlocks.clear();
        blockStates.clear();
        blockPositions.clear();
        processedChunks.clear();
        blockEntityData.clear();
        blockDimensionIndices.clear();
    }

    public void saveAllLoadedChunks(ServerLevel level) {
        Logger logger = LogManager.getLogger();
        ServerChunkCache chunkCache = level.getChunkSource();
        int chunkSize = 16;
        int dimensionIndex = getDimensionIndex(level.dimension());

        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            BlockPos playerPos = player.blockPosition();
            int renderDistance = level.getServer().getPlayerList().getViewDistance();

            int playerChunkX = playerPos.getX() >> 4;
            int playerChunkZ = playerPos.getZ() >> 4;

            for (int dx = -renderDistance; dx <= renderDistance; dx++) {
                for (int dz = -renderDistance; dz <= renderDistance; dz++) {
                    int chunkX = playerChunkX + dx;
                    int chunkZ = playerChunkZ + dz;

                    LevelChunk chunk = chunkCache.getChunkNow(chunkX, chunkZ);
                    if (chunk != null) {
                        ChunkPos chunkPos = chunk.getPos();

                        if (processedChunks.contains(chunkPos)) continue;
                        processedChunks.add(chunkPos);

                        BlockPos minPos = chunk.getPos().getWorldPosition();
                        BlockPos maxPos = minPos.offset(chunkSize - 1, level.getMaxBuildHeight() - 1, chunkSize - 1);

                        for (int x = minPos.getX(); x <= maxPos.getX(); x++) {
                            for (int z = minPos.getZ(); z <= maxPos.getZ(); z++) {
                                for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++) {
                                    BlockPos pos = new BlockPos(x, y, z);
                                    BlockState state = chunk.getBlockState(pos);
                                    if (!state.isAir()) {
                                        saveBlockState(pos, state, level.dimension()); // Save with dimension
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