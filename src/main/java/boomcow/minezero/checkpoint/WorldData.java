package boomcow.minezero.checkpoint;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.ServerLevelData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class WorldData {








    private static final Logger LOGGER_WD = LogManager.getLogger("MineZeroWorldData"); // Separate logger

    // NBT Keys for WorldData
    private static final String KEY_WD_IS_RAINING = "wd_isRaining";
    private static final String KEY_WD_IS_THUNDERING = "wd_isThundering";
    private static final String KEY_WD_RAIN_TIME = "wd_rainTime";
    private static final String KEY_WD_THUNDER_TIME = "wd_thunderTime";
    private static final String KEY_WD_CLEAR_TIME = "wd_clearTime";
    private static final String KEY_WD_BLOCK_ENTITY_DATA = "wd_blockEntityData";
    private static final String KEY_WD_SAVED_BLOCKS_BY_CHUNK = "wd_savedBlocksByChunk";
    private static final String KEY_WD_CHECKPOINT_TICK = "wd_checkpointTick";
    private static final String KEY_WD_DAY_TIME = "wd_dayTime";
    private static final String KEY_WD_GAME_TIME = "wd_gameTime";
    private static final String KEY_WD_SAVED_LIGHTNINGS = "wd_savedLightnings";
    private static final String KEY_WD_BLOCK_POSITIONS = "wd_blockPositions";
    private static final String KEY_WD_BLOCK_DIMENSION_INDICES = "wd_blockDimensionIndices";
    private static final String KEY_WD_MODIFIED_BLOCKS = "wd_modifiedBlocks";
    private static final String KEY_WD_MINED_BLOCKS = "wd_minedBlocks";
    private static final String KEY_WD_ADDED_EYES = "wd_addedEyes";
    private static final String KEY_WD_MODIFIED_FLUID_BLOCKS = "wd_modifiedFluidBlocks";
    private static final String KEY_WD_MINED_FLUID_BLOCKS = "wd_minedFluidBlocks";
    private static final String KEY_WD_CREATED_PORTALS = "wd_createdPortals";
    private static final String KEY_WD_DESTROYED_PORTALS = "wd_destroyedPortals";
    private static final String KEY_WD_NEW_FIRES = "wd_newFires";
    private static final String KEY_WD_BLOCK_STATES_LEGACY = "wd_blockStatesLegacy"; // If you decide to keep and serialize it

    // Keys for inner structures within NBT
    private static final String KEY_POS = "pos";
    private static final String KEY_STATE = "state";
    private static final String KEY_DIMENSION = "dimension";
    private static final String KEY_CHUNK_X = "chunkX";
    private static final String KEY_CHUNK_Z = "chunkZ";
    private static final String KEY_BLOCKS_IN_CHUNK = "blocksInChunk";
    private static final String KEY_TICK_TIME = "tickTime";
    private static final String KEY_MAP_KEY_POS = "keyPos"; // For map serialization
    private static final String KEY_MAP_VALUE_INT = "valueInt"; // For Map<BlockPos, Integer>
    private static final String KEY_MAP_VALUE_STATE = "valueState"; // For Map<BlockPos, BlockState>
    private static final String KEY_MAP_VALUE_NBT = "valueNbt"; // For Map<BlockPos, CompoundTag>


    private List<BlockState> blockStates_LEGACY = new ArrayList<>(); // Consider if this is truly needed
    private boolean isRaining;
    private boolean isThundering;
    private int rainTime;
    private int thunderTime;
    private int clearTime;
    private Map<BlockPos, CompoundTag> blockEntityData = new HashMap<>();
    private Set<ChunkPos> processedChunks = new HashSet<>(); // Runtime helper, not typically serialized
    private Map<ChunkPos, List<SavedBlock>> savedBlocksByChunk = new HashMap<>();
    private long checkpointTick;
    private long dayTime;
    private long gameTime;
    private final List<LightningStrike> savedLightnings = new ArrayList<>();

    public final List<BlockPos> blockPositions = new ArrayList<>();
    public final Map<BlockPos, Integer> blockDimensionIndices = new HashMap<>();
    public final Set<BlockPos> modifiedBlocks = new HashSet<>();
    public final Map<BlockPos, BlockState> minedBlocks = new HashMap<>();
    public final Set<BlockPos> addedEyes = new HashSet<>();
    public final Set<BlockPos> modifiedFluidBlocks = new HashSet<>();
    public final Map<BlockPos, BlockState> minedFluidBlocks = new HashMap<>();
    public final Set<BlockPos> createdPortals = new HashSet<>();
    public final Map<BlockPos, BlockState> destroyedPortals = new HashMap<>();
    public List<BlockPos> newFires = new ArrayList<>();

    private static final Map<Integer, ResourceKey<Level>> dimensionMap = new HashMap<>();
    private static int nextDimensionIndex = 0; // Auto-incrementing index for dimensions
    public List<BlockPos> getNewFires() {
        return newFires;
    }

    public void saveCheckpointTick(long checkpointTick) {
        this.checkpointTick = checkpointTick;
    }

    public long getCheckpointTick() {
        return this.checkpointTick;
    }





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

    public void saveWeather(ServerLevel level) {
        Logger logger = LogManager.getLogger();
        this.isRaining = level.isRaining();
        this.isThundering = level.isThundering();


        if (level.getLevelData() instanceof ServerLevelData serverData) {
            this.rainTime = serverData.getRainTime();
            this.thunderTime = serverData.getThunderTime();
            this.clearTime = serverData.getClearWeatherTime(); // Optional, for completeness
        }
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
        blockStates_LEGACY.add(state);
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

    public long getGameTime() {
        return this.gameTime;
    }

    public void saveGameTime(long gameTime) {
        this.gameTime = gameTime;
    }

    public boolean isRaining() {
        return this.isRaining;
    }
    public boolean isThundering() {
        return this.isThundering;
    }
    public int getRainTime() {
        return this.rainTime;
    }
    public int getThunderTime() {
        return this.thunderTime;
    }
    public int getClearTime() {
        return this.clearTime;
    }

    public List<BlockState> getBlockStates() {
        return this.blockStates_LEGACY;
    }

    public List<BlockPos> getBlockPositions() {
        return blockPositions;
    }

    public static class LightningStrike {
        public final BlockPos pos;
        public final long tickTime;

        public LightningStrike(BlockPos pos, long tickTime) {
            this.pos = pos;
            this.tickTime = tickTime;
        }

        public CompoundTag toNBT() {
            CompoundTag tag = new CompoundTag();
            tag.put(KEY_POS, NbtUtils.writeBlockPos(pos));
            tag.putLong(KEY_TICK_TIME, tickTime);
            return tag;
        }

        public static LightningStrike fromNBT(CompoundTag tag) {
            // Corrected way to parse BlockPos using CODEC:
            BlockPos pos = BlockPos.CODEC.parse(NbtOps.INSTANCE, tag.getCompound(KEY_POS))
                    .resultOrPartial(LOGGER_WD::error) // Log any parsing errors
                    .orElse(BlockPos.ZERO); // Provide a default in case of failure

            long tickTime = tag.getLong(KEY_TICK_TIME);
            return new LightningStrike(pos, tickTime);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LightningStrike that = (LightningStrike) o;
            return tickTime == that.tickTime && pos.equals(that.pos);
        }

        @Override
        public int hashCode() {
            return Objects.hash(pos, tickTime);
        }
    }



    public void addLightningStrike(BlockPos pos, long tickTime) {
        Logger logger = LogManager.getLogger();
        LightningStrike newStrike = new LightningStrike(pos, tickTime);

        if (!savedLightnings.contains(newStrike)) {
            savedLightnings.add(newStrike);
        }
    }


    public List<LightningStrike> getSavedLightnings() {
        return savedLightnings;
    }


    public List<BlockPos> getInstanceBlockPositions() { return this.blockPositions; }
    public Map<BlockPos, Integer> getInstanceBlockDimensionIndices() { return this.blockDimensionIndices; }
    public Set<BlockPos> getModifiedBlocks() { return this.modifiedBlocks; } // Getter for the instance field
    public Map<BlockPos, BlockState> getMinedBlocks() { return this.minedBlocks; }
    public Set<BlockPos> getAddedEyes() { return this.addedEyes; }
    public Set<BlockPos> getModifiedFluidBlocks() { return this.modifiedFluidBlocks; }
    public Map<BlockPos, BlockState> getMinedFluidBlocks() { return this.minedFluidBlocks; }
    public Set<BlockPos> getCreatedPortals() { return this.createdPortals; }
    public Map<BlockPos, BlockState> getDestroyedPortals() { return this.destroyedPortals; }








    /**
     * Clears all saved world data.
     */
    public void clearWorldData() {
        minedBlocks.clear();
        modifiedBlocks.clear();
        minedFluidBlocks.clear();
        modifiedFluidBlocks.clear();
        blockStates_LEGACY.clear();
        blockPositions.clear();
        processedChunks.clear();
        blockEntityData.clear();
        blockDimensionIndices.clear();
        savedBlocksByChunk.clear();
        savedLightnings.clear();
        rainTime = 0;
        thunderTime = 0;
        clearTime = 0;
        isRaining = false;
        isThundering = false;
        dayTime = 0;
        gameTime = 0;
        checkpointTick = 0;
        newFires.clear();
        createdPortals.clear();
        destroyedPortals.clear();
        addedEyes.clear();
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


                    // Save block entities.
                    for (BlockPos entityPos : chunk.getBlockEntities().keySet()) {
                        BlockEntity blockEntity = chunk.getBlockEntity(entityPos);
                        if (blockEntity != null && !blockEntity.isRemoved()) {
                            BlockPos immutablePos = entityPos.immutable();
                            saveBlockEntity(immutablePos, blockEntity.saveWithFullMetadata(level.registryAccess()));
                            blockDimensionIndices.put(immutablePos, getDimensionIndex(currentDimension));
                        }
                    }
                }
            }
        }
    }


    // --- SERIALIZATION (NBT) ---
    public CompoundTag saveToNBT(CompoundTag nbt) {
        nbt.putBoolean(KEY_WD_IS_RAINING, this.isRaining);
        nbt.putBoolean(KEY_WD_IS_THUNDERING, this.isThundering);
        nbt.putInt(KEY_WD_RAIN_TIME, this.rainTime);
        nbt.putInt(KEY_WD_THUNDER_TIME, this.thunderTime);
        nbt.putInt(KEY_WD_CLEAR_TIME, this.clearTime);
        nbt.putLong(KEY_WD_CHECKPOINT_TICK, this.checkpointTick);
        nbt.putLong(KEY_WD_DAY_TIME, this.dayTime);
        nbt.putLong(KEY_WD_GAME_TIME, this.gameTime);

        // BlockEntityData (improved serialization)
        ListTag beListTag = new ListTag();
        for (Map.Entry<BlockPos, CompoundTag> entry : this.blockEntityData.entrySet()) {
            CompoundTag beEntryTag = new CompoundTag();
            beEntryTag.put(KEY_MAP_KEY_POS, NbtUtils.writeBlockPos(entry.getKey()));
            beEntryTag.put(KEY_MAP_VALUE_NBT, entry.getValue());
            beListTag.add(beEntryTag);
        }
        nbt.put(KEY_WD_BLOCK_ENTITY_DATA, beListTag);

        // SavedBlocksByChunk
        ListTag savedBlocksByChunkList = new ListTag();
        for (Map.Entry<ChunkPos, List<SavedBlock>> entry : this.savedBlocksByChunk.entrySet()) {
            CompoundTag chunkEntryTag = new CompoundTag();
            chunkEntryTag.putInt(KEY_CHUNK_X, entry.getKey().x);
            chunkEntryTag.putInt(KEY_CHUNK_Z, entry.getKey().z);
            ListTag blocksInChunkTag = new ListTag();
            for (SavedBlock savedBlock : entry.getValue()) {
                blocksInChunkTag.add(savedBlock.toNBT());
            }
            chunkEntryTag.put(KEY_BLOCKS_IN_CHUNK, blocksInChunkTag);
            savedBlocksByChunkList.add(chunkEntryTag);
        }
        nbt.put(KEY_WD_SAVED_BLOCKS_BY_CHUNK, savedBlocksByChunkList);

        // SavedLightnings
        ListTag lightningListTag = new ListTag();
        for (LightningStrike strike : this.savedLightnings) {
            lightningListTag.add(strike.toNBT());
        }
        nbt.put(KEY_WD_SAVED_LIGHTNINGS, lightningListTag);

        // --- Serialize Instance Block Change Tracking Fields ---
        // blockPositions (List<BlockPos>)
        ListTag bpListTag = new ListTag();
        for (BlockPos pos : this.blockPositions) {
            bpListTag.add(NbtUtils.writeBlockPos(pos));
        }
        nbt.put(KEY_WD_BLOCK_POSITIONS, bpListTag);

        // blockDimensionIndices (Map<BlockPos, Integer>)
        ListTag bdiListTag = new ListTag();
        for (Map.Entry<BlockPos, Integer> entry : this.blockDimensionIndices.entrySet()) {
            CompoundTag bdiEntryTag = new CompoundTag();
            bdiEntryTag.put(KEY_MAP_KEY_POS, NbtUtils.writeBlockPos(entry.getKey()));
            bdiEntryTag.putInt(KEY_MAP_VALUE_INT, entry.getValue());
            bdiListTag.add(bdiEntryTag);
        }
        nbt.put(KEY_WD_BLOCK_DIMENSION_INDICES, bdiListTag);

        // modifiedBlocks (Set<BlockPos>)
        ListTag mbListTag = new ListTag();
        for (BlockPos pos : this.modifiedBlocks) {
            mbListTag.add(NbtUtils.writeBlockPos(pos));
        }
        nbt.put(KEY_WD_MODIFIED_BLOCKS, mbListTag);

        // minedBlocks (Map<BlockPos, BlockState>)
        ListTag minedBListTag = new ListTag();
        for (Map.Entry<BlockPos, BlockState> entry : this.minedBlocks.entrySet()) {
            CompoundTag minedBEntryTag = new CompoundTag();
            minedBEntryTag.put(KEY_MAP_KEY_POS, NbtUtils.writeBlockPos(entry.getKey()));
            minedBEntryTag.put(KEY_MAP_VALUE_STATE, NbtUtils.writeBlockState(entry.getValue()));
            minedBListTag.add(minedBEntryTag);
        }
        nbt.put(KEY_WD_MINED_BLOCKS, minedBListTag);

        // addedEyes (Set<BlockPos>)
        ListTag aeListTag = new ListTag();
        for (BlockPos pos : this.addedEyes) {
            aeListTag.add(NbtUtils.writeBlockPos(pos));
        }
        nbt.put(KEY_WD_ADDED_EYES, aeListTag);

        // modifiedFluidBlocks (Set<BlockPos>)
        ListTag mfbListTag = new ListTag();
        for (BlockPos pos : this.modifiedFluidBlocks) {
            mfbListTag.add(NbtUtils.writeBlockPos(pos));
        }
        nbt.put(KEY_WD_MODIFIED_FLUID_BLOCKS, mfbListTag);

        // minedFluidBlocks (Map<BlockPos, BlockState>)
        ListTag minedFbListTag = new ListTag();
        for (Map.Entry<BlockPos, BlockState> entry : this.minedFluidBlocks.entrySet()) {
            CompoundTag minedFbEntryTag = new CompoundTag();
            minedFbEntryTag.put(KEY_MAP_KEY_POS, NbtUtils.writeBlockPos(entry.getKey()));
            minedFbEntryTag.put(KEY_MAP_VALUE_STATE, NbtUtils.writeBlockState(entry.getValue()));
            minedFbListTag.add(minedFbEntryTag);
        }
        nbt.put(KEY_WD_MINED_FLUID_BLOCKS, minedFbListTag);

        // createdPortals (Set<BlockPos>)
        ListTag cpListTag = new ListTag();
        for (BlockPos pos : this.createdPortals) {
            cpListTag.add(NbtUtils.writeBlockPos(pos));
        }
        nbt.put(KEY_WD_CREATED_PORTALS, cpListTag);

        // destroyedPortals (Map<BlockPos, BlockState>)
        ListTag dpListTag = new ListTag();
        for (Map.Entry<BlockPos, BlockState> entry : this.destroyedPortals.entrySet()) {
            CompoundTag dpEntryTag = new CompoundTag();
            dpEntryTag.put(KEY_MAP_KEY_POS, NbtUtils.writeBlockPos(entry.getKey()));
            dpEntryTag.put(KEY_MAP_VALUE_STATE, NbtUtils.writeBlockState(entry.getValue()));
            dpListTag.add(dpEntryTag);
        }
        nbt.put(KEY_WD_DESTROYED_PORTALS, dpListTag);

        // newFires (List<BlockPos>)
        ListTag nfListTag = new ListTag();
        for (BlockPos pos : this.newFires) {
            nfListTag.add(NbtUtils.writeBlockPos(pos));
        }
        nbt.put(KEY_WD_NEW_FIRES, nfListTag);

        // blockStates_LEGACY (List<BlockState>) - If you keep it
        // ListTag bslListTag = new ListTag();
        // for (BlockState bs : this.blockStates_LEGACY) {
        //     bslListTag.add(NbtUtils.writeBlockState(bs));
        // }
        // nbt.put(KEY_WD_BLOCK_STATES_LEGACY, bslListTag);

        LOGGER_WD.debug("WorldData instance saved to NBT.");
        return nbt;
    }

    public void loadFromNBT(CompoundTag nbt, HolderLookup.Provider lookupProvider) {
        this.isRaining = nbt.getBoolean(KEY_WD_IS_RAINING);
        this.isThundering = nbt.getBoolean(KEY_WD_IS_THUNDERING);
        this.rainTime = nbt.getInt(KEY_WD_RAIN_TIME);
        this.thunderTime = nbt.getInt(KEY_WD_THUNDER_TIME);
        this.clearTime = nbt.getInt(KEY_WD_CLEAR_TIME);
        this.checkpointTick = nbt.getLong(KEY_WD_CHECKPOINT_TICK);
        this.dayTime = nbt.getLong(KEY_WD_DAY_TIME);
        this.gameTime = nbt.getLong(KEY_WD_GAME_TIME);

        // IMPORTANT: Create NbtOps with the current lookupProvider for robust codec parsing.
        // This is crucial for anything that relies on registry lookups (like BlockStates, Items, etc.).


        // BlockEntityData
        this.blockEntityData.clear();
        if (nbt.contains(KEY_WD_BLOCK_ENTITY_DATA, Tag.TAG_LIST)) {
            ListTag beListTag = nbt.getList(KEY_WD_BLOCK_ENTITY_DATA, Tag.TAG_COMPOUND);
            for (int i = 0; i < beListTag.size(); i++) {
                CompoundTag beEntryTag = beListTag.getCompound(i);
                // Use NbtOps.INSTANCE for BlockPos.CODEC.parse (it does not require registry context)
                BlockPos pos = BlockPos.CODEC.parse(NbtOps.INSTANCE, beEntryTag.getCompound(KEY_MAP_KEY_POS))
                        .resultOrPartial(LOGGER_WD::error)
                        .orElse(BlockPos.ZERO);
                CompoundTag valueNbt = beEntryTag.getCompound(KEY_MAP_VALUE_NBT);
                this.blockEntityData.put(pos, valueNbt);
            }
        }

        // SavedBlocksByChunk
        this.savedBlocksByChunk.clear();
        if (nbt.contains(KEY_WD_SAVED_BLOCKS_BY_CHUNK, Tag.TAG_LIST)) {
            ListTag savedBlocksByChunkList = nbt.getList(KEY_WD_SAVED_BLOCKS_BY_CHUNK, Tag.TAG_COMPOUND);
            for (int i = 0; i < savedBlocksByChunkList.size(); i++) {
                CompoundTag chunkEntryTag = savedBlocksByChunkList.getCompound(i);
                ChunkPos chunkPos = new ChunkPos(chunkEntryTag.getInt(KEY_CHUNK_X), chunkEntryTag.getInt(KEY_CHUNK_Z));
                ListTag blocksInChunkTag = chunkEntryTag.getList(KEY_BLOCKS_IN_CHUNK, Tag.TAG_COMPOUND);
                List<SavedBlock> blocks = new ArrayList<>();
                for (int j = 0; j < blocksInChunkTag.size(); j++) {
                    // SavedBlock.fromNBT should handle its own BlockPos and BlockState reading correctly.
                    // It should also ideally receive the lookupProvider or use NbtOps.create(provider).
                    blocks.add(SavedBlock.fromNBT(blocksInChunkTag.getCompound(j)));
                }
                this.savedBlocksByChunk.put(chunkPos, blocks);
            }
        }

        // SavedLightnings
        this.savedLightnings.clear();
        if (nbt.contains(KEY_WD_SAVED_LIGHTNINGS, Tag.TAG_LIST)) {
            ListTag lightningListTag = nbt.getList(KEY_WD_SAVED_LIGHTNINGS, Tag.TAG_COMPOUND);
            for (int i = 0; i < lightningListTag.size(); i++) {
                // LightningStrike.fromNBT should ideally also use the lookupProvider if it deserializes entities etc.
                this.savedLightnings.add(LightningStrike.fromNBT(lightningListTag.getCompound(i)));
            }
        }

        // --- Load Instance Block Change Tracking Fields ---

        // Prepare HolderGetter<Block> once for all BlockState reading in this method
        // Use the stored lookupProvider
        HolderGetter<Block> blockHolderGetter = lookupProvider.lookupOrThrow(Registries.BLOCK);

        this.blockPositions.clear();
        if (nbt.contains(KEY_WD_BLOCK_POSITIONS, Tag.TAG_LIST)) {
            ListTag listTag = nbt.getList(KEY_WD_BLOCK_POSITIONS, Tag.TAG_COMPOUND);
            for (int i = 0; i < listTag.size(); i++) {
                this.blockPositions.add(
                        BlockPos.CODEC.parse(NbtOps.INSTANCE, listTag.getCompound(i)) // Use NbtOps.INSTANCE
                                .resultOrPartial(LOGGER_WD::error)
                                .orElse(BlockPos.ZERO)
                );
            }
        }

        this.blockDimensionIndices.clear();
        if (nbt.contains(KEY_WD_BLOCK_DIMENSION_INDICES, Tag.TAG_LIST)) {
            ListTag listTag = nbt.getList(KEY_WD_BLOCK_DIMENSION_INDICES, Tag.TAG_COMPOUND);
            for (int i = 0; i < listTag.size(); i++) {
                CompoundTag entryTag = listTag.getCompound(i);
                this.blockDimensionIndices.put(
                        BlockPos.CODEC.parse(NbtOps.INSTANCE, entryTag.getCompound(KEY_MAP_KEY_POS)) // Use NbtOps.INSTANCE
                                .resultOrPartial(LOGGER_WD::error)
                                .orElse(BlockPos.ZERO),
                        entryTag.getInt(KEY_MAP_VALUE_INT)
                );
            }
        }

        this.modifiedBlocks.clear();
        if (nbt.contains(KEY_WD_MODIFIED_BLOCKS, Tag.TAG_LIST)) {
            ListTag listTag = nbt.getList(KEY_WD_MODIFIED_BLOCKS, Tag.TAG_COMPOUND);
            for (int i = 0; i < listTag.size(); i++) {
                this.modifiedBlocks.add(
                        BlockPos.CODEC.parse(NbtOps.INSTANCE, listTag.getCompound(i)) // Use NbtOps.INSTANCE
                                .resultOrPartial(LOGGER_WD::error)
                                .orElse(BlockPos.ZERO)
                );
            }
        }

        this.minedBlocks.clear();
        if (nbt.contains(KEY_WD_MINED_BLOCKS, Tag.TAG_LIST)) {
            ListTag listTag = nbt.getList(KEY_WD_MINED_BLOCKS, Tag.TAG_COMPOUND);
            for (int i = 0; i < listTag.size(); i++) {
                CompoundTag entryTag = listTag.getCompound(i);
                this.minedBlocks.put(
                        BlockPos.CODEC.parse(NbtOps.INSTANCE, entryTag.getCompound(KEY_MAP_KEY_POS)) // Use NbtOps.INSTANCE
                                .resultOrPartial(LOGGER_WD::error)
                                .orElse(BlockPos.ZERO),
                        NbtUtils.readBlockState(blockHolderGetter, entryTag.getCompound(KEY_MAP_VALUE_STATE))
                );
            }
        }

        this.addedEyes.clear();
        if (nbt.contains(KEY_WD_ADDED_EYES, Tag.TAG_LIST)) {
            ListTag listTag = nbt.getList(KEY_WD_ADDED_EYES, Tag.TAG_COMPOUND);
            for (int i = 0; i < listTag.size(); i++) {
                this.addedEyes.add(
                        BlockPos.CODEC.parse(NbtOps.INSTANCE, listTag.getCompound(i)) // Use NbtOps.INSTANCE
                                .resultOrPartial(LOGGER_WD::error)
                                .orElse(BlockPos.ZERO)
                );
            }
        }

        this.modifiedFluidBlocks.clear();
        if (nbt.contains(KEY_WD_MODIFIED_FLUID_BLOCKS, Tag.TAG_LIST)) {
            ListTag listTag = nbt.getList(KEY_WD_MODIFIED_FLUID_BLOCKS, Tag.TAG_COMPOUND);
            for (int i = 0; i < listTag.size(); i++) {
                this.modifiedFluidBlocks.add(
                        BlockPos.CODEC.parse(NbtOps.INSTANCE, listTag.getCompound(i)) // Use NbtOps.INSTANCE
                                .resultOrPartial(LOGGER_WD::error)
                                .orElse(BlockPos.ZERO)
                );
            }
        }

        this.minedFluidBlocks.clear();
        if (nbt.contains(KEY_WD_MINED_FLUID_BLOCKS, Tag.TAG_LIST)) {
            ListTag listTag = nbt.getList(KEY_WD_MINED_FLUID_BLOCKS, Tag.TAG_COMPOUND);
            for (int i = 0; i < listTag.size(); i++) {
                CompoundTag entryTag = listTag.getCompound(i);
                this.minedFluidBlocks.put(
                        BlockPos.CODEC.parse(NbtOps.INSTANCE, entryTag.getCompound(KEY_MAP_KEY_POS)) // Use NbtOps.INSTANCE
                                .resultOrPartial(LOGGER_WD::error)
                                .orElse(BlockPos.ZERO),
                        NbtUtils.readBlockState(blockHolderGetter, entryTag.getCompound(KEY_MAP_VALUE_STATE))
                );
            }
        }

        this.createdPortals.clear();
        if (nbt.contains(KEY_WD_CREATED_PORTALS, Tag.TAG_LIST)) {
            ListTag listTag = nbt.getList(KEY_WD_CREATED_PORTALS, Tag.TAG_COMPOUND);
            for (int i = 0; i < listTag.size(); i++) {
                this.createdPortals.add(
                        BlockPos.CODEC.parse(NbtOps.INSTANCE, listTag.getCompound(i)) // Use NbtOps.INSTANCE
                                .resultOrPartial(LOGGER_WD::error)
                                .orElse(BlockPos.ZERO)
                );
            }
        }

        this.destroyedPortals.clear();
        if (nbt.contains(KEY_WD_DESTROYED_PORTALS, Tag.TAG_LIST)) {
            ListTag listTag = nbt.getList(KEY_WD_DESTROYED_PORTALS, Tag.TAG_COMPOUND);
            for (int i = 0; i < listTag.size(); i++) {
                CompoundTag entryTag = listTag.getCompound(i);
                this.destroyedPortals.put(
                        BlockPos.CODEC.parse(NbtOps.INSTANCE, entryTag.getCompound(KEY_MAP_KEY_POS)) // Use NbtOps.INSTANCE
                                .resultOrPartial(LOGGER_WD::error)
                                .orElse(BlockPos.ZERO),
                        NbtUtils.readBlockState(blockHolderGetter, entryTag.getCompound(KEY_MAP_VALUE_STATE))
                );
            }
        }

        this.newFires.clear();
        if (nbt.contains(KEY_WD_NEW_FIRES, Tag.TAG_LIST)) {
            ListTag listTag = nbt.getList(KEY_WD_NEW_FIRES, Tag.TAG_COMPOUND);
            for (int i = 0; i < listTag.size(); i++) {
                this.newFires.add(
                        BlockPos.CODEC.parse(NbtOps.INSTANCE, listTag.getCompound(i)) // Use NbtOps.INSTANCE
                                .resultOrPartial(LOGGER_WD::error)
                                .orElse(BlockPos.ZERO)
                );
            }
        }

        // Load blockStates_LEGACY (if you are keeping it)
        this.blockStates_LEGACY.clear();
        if (nbt.contains(KEY_WD_BLOCK_STATES_LEGACY, Tag.TAG_LIST)) {
            ListTag listTag = nbt.getList(KEY_WD_BLOCK_STATES_LEGACY, Tag.TAG_COMPOUND); // Assuming these are BlockState NBTs directly
            for (int i = 0; i < listTag.size(); i++) {
                this.blockStates_LEGACY.add(NbtUtils.readBlockState(blockHolderGetter, listTag.getCompound(i)));
            }
        }

        LOGGER_WD.debug("WorldData instance loaded from NBT.");
    }

    /**
     * Record to store saved block data.
     */
    public static record SavedBlock(BlockPos pos, BlockState state, ResourceKey<Level> dimension) {
        // NBT Keys are static and defined in the outer WorldData class

        public CompoundTag toNBT() {
            CompoundTag tag = new CompoundTag();
            // KEY_POS is static in WorldData, so WorldData.KEY_POS or just KEY_POS if no ambiguity
            tag.put(WorldData.KEY_POS, NbtUtils.writeBlockPos(pos));
            tag.put(WorldData.KEY_STATE, NbtUtils.writeBlockState(state));
            tag.putString(WorldData.KEY_DIMENSION, dimension.location().toString());
            return tag;
        }

        public static SavedBlock fromNBT(CompoundTag tag) {
            BlockPos pos = BlockPos.CODEC.parse(NbtOps.INSTANCE, tag.getCompound(WorldData.KEY_POS))
                    .resultOrPartial(WorldData.LOGGER_WD::error)
                    .orElse(BlockPos.ZERO);

            // Explicitly cast BuiltInRegistries.BLOCK to HolderGetter<Block>
            HolderGetter<Block> blockHolderGetter = (HolderGetter<Block>) BuiltInRegistries.BLOCK;
            BlockState state = NbtUtils.readBlockState(blockHolderGetter, tag.getCompound(WorldData.KEY_STATE));

            ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(tag.getString(WorldData.KEY_DIMENSION)));
            return new SavedBlock(pos, state, dimension);
        }
    }
}
