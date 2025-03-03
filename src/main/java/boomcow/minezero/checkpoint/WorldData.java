package boomcow.minezero.checkpoint;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class WorldData {
    // Legacy collections for compatibility.
    public static final List<BlockPos> blockPositions = new ArrayList<>();
    private List<BlockState> blockStates = new ArrayList<>();
    public static final Map<BlockPos, Integer> blockDimensionIndices = new HashMap<>();

    // Data for block entities.
    private Map<BlockPos, CompoundTag> blockEntityData;

    // Set of chunks that have already been processed during saving.
    private Set<ChunkPos> processedChunks = new HashSet<>();

    // New: Saved block states grouped by chunk position.
    private Map<ChunkPos, List<SavedBlock>> savedBlocksByChunk = new HashMap<>();

    // Dimension mapping for saving dimension information.
    private static final Map<Integer, ResourceKey<Level>> dimensionMap = new HashMap<>();
    private static int nextDimensionIndex = 0; // Auto-incrementing index for dimensions

    public static final Set<BlockPos> modifiedBlocks = new HashSet<>();
    public static final Map<BlockPos, BlockState> minedBlocks = new HashMap<>();

    public static final Set<BlockPos> modifiedFluidBlocks = new HashSet<>();
    public static final Map<BlockPos, BlockState> minedFluidBlocks = new HashMap<>();


    private long dayTime;

    public WorldData() {
        this.blockEntityData = new HashMap<>();
        this.dayTime = 0;
    }

    /**
     * Returns an index for the given dimension, creating one if necessary.
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

    /**
     * Saves a block state:
     *  - Adds the saved block to a chunk-based map for optimized restoration.
     *  - Updates legacy global lists/maps (blockPositions, blockStates, blockDimensionIndices) for compatibility.
     */
    public void saveBlockState(BlockPos pos, BlockState state, ResourceKey<Level> dimension) {
        // Save in the chunk-based structure.
        ChunkPos chunkPos = new ChunkPos(pos);
        SavedBlock saved = new SavedBlock(pos, state, dimension);
        savedBlocksByChunk.computeIfAbsent(chunkPos, k -> new ArrayList<>()).add(saved);

        // Update legacy collections.
        blockPositions.add(pos);
        blockStates.add(state);
        blockDimensionIndices.put(pos, getDimensionIndex(dimension));
    }

    /**
     * Returns the saved blocks grouped by chunk.
     */
    public Map<ChunkPos, List<SavedBlock>> getSavedBlocksByChunk() {
        return this.savedBlocksByChunk;
    }

    public Map<BlockPos, CompoundTag> getBlockEntityData() {
        return this.blockEntityData;
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

    public List<BlockPos> getBlockPositions() {
        return blockPositions;
    }

    /**
     * Clears all saved world data.
     */
    public void clearWorldData() {
        minedBlocks.clear();
        modifiedBlocks.clear();
        blockStates.clear();
        blockPositions.clear();
        processedChunks.clear();
        blockEntityData.clear();
        blockDimensionIndices.clear();
        savedBlocksByChunk.clear();
    }

    /**
     * Saves all loaded chunks within players' render distance.
     * Blocks that are air are skipped.
     */
    public void saveAllLoadedChunks(ServerLevel level) {
        Logger logger = LogManager.getLogger();
        ServerChunkCache chunkCache = level.getChunkSource();
        final int chunkSize = 16;
        ResourceKey<Level> currentDimension = level.dimension();
        int renderDistance = level.getServer().getPlayerList().getViewDistance();

        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            BlockPos playerPos = player.blockPosition();
            int playerChunkX = playerPos.getX() >> 4;
            int playerChunkZ = playerPos.getZ() >> 4;

            for (int dx = -renderDistance; dx <= renderDistance; dx++) {
                for (int dz = -renderDistance; dz <= renderDistance; dz++) {
                    int chunkX = playerChunkX + dx;
                    int chunkZ = playerChunkZ + dz;

                    LevelChunk chunk = chunkCache.getChunkNow(chunkX, chunkZ);
                    if (chunk == null) continue;

                    ChunkPos chunkPos = chunk.getPos();
                    if (!processedChunks.add(chunkPos)) continue;

                    // Get all sections from the chunk.
                    LevelChunkSection[] sections = chunk.getSections();
                    for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
                        LevelChunkSection section = sections[sectionIndex];
                        if (section == null) continue;
                        // Calculate the section's base Y coordinate.
                        int sectionBaseY = level.getMinBuildHeight() + (sectionIndex * 16);

                        // Optionally, if you have access to the section's palette, you could check if the section is empty.
                        // For example:
                        // if (section.getPalette().size() == 1 && section.getPalette().get(0).isAir()) continue;

                        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
                        for (int localX = 0; localX < 16; localX++) {
                            for (int localY = 0; localY < 16; localY++) {
                                for (int localZ = 0; localZ < 16; localZ++) {
                                    BlockState state = section.getBlockState(localX, localY, localZ);
                                    if (!state.isAir()) {
                                        // Convert local section coordinates to world coordinates.
                                        int worldX = chunkPos.getMinBlockX() + localX;
                                        int worldY = sectionBaseY + localY;
                                        int worldZ = chunkPos.getMinBlockZ() + localZ;
                                        mutablePos.set(worldX, worldY, worldZ);
                                        saveBlockState(mutablePos.immutable(), state, currentDimension);
                                    }
                                }
                            }
                        }
                    }

                    // Save block entities.
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



    /**
     * Record to store saved block data.
     */
    public static record SavedBlock(BlockPos pos, BlockState state, ResourceKey<Level> dimension) {}
}
