package boomcow.minezero.checkpoint;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class WorldData {

    private static final Logger LOGGER_WD = LoggerFactory.getLogger("MineZeroWorldData");
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
    private static final String KEY_WD_BLOCK_STATES_LEGACY = "wd_blockStatesLegacy";
    private static final String KEY_POS = "pos";
    private static final String KEY_STATE = "state";
    private static final String KEY_DIMENSION = "dimension";
    private static final String KEY_CHUNK_X = "chunkX";
    private static final String KEY_CHUNK_Z = "chunkZ";
    private static final String KEY_BLOCKS_IN_CHUNK = "blocksInChunk";
    private static final String KEY_TICK_TIME = "tickTime";
    private static final String KEY_MAP_KEY_POS = "keyPos";
    private static final String KEY_MAP_VALUE_INT = "valueInt";
    private static final String KEY_MAP_VALUE_STATE = "valueState";
    private static final String KEY_MAP_VALUE_NBT = "valueNbt";


    private List<BlockState> blockStates_LEGACY = new ArrayList<>();
    private boolean isRaining;
    private boolean isThundering;
    private int rainTime;
    private int thunderTime;
    private int clearTime;
    private Map<BlockPos, NbtCompound> blockEntityData = new HashMap<>();
    private Set<ChunkPos> processedChunks = new HashSet<>();
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

    private static final Map<Integer, RegistryKey<World>> dimensionMap = new HashMap<>();
    private static int nextDimensionIndex = 0;
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

    public static int getDimensionIndex(RegistryKey<World> dimension) {
        for (Map.Entry<Integer, RegistryKey<World>> entry : dimensionMap.entrySet()) {
            if (entry.getValue().equals(dimension)) {
                return entry.getKey();
            }
        }
        int newIndex = nextDimensionIndex++;
        dimensionMap.put(newIndex, dimension);
        return newIndex;
    }

    public void saveWeather(ServerWorld world) {
        this.isRaining = world.isRaining();
        this.isThundering = world.isThundering();
        this.rainTime = world.getLevelProperties().getRainTime();
        this.thunderTime = world.getLevelProperties().getThunderTime();
        this.clearTime = world.getLevelProperties().getClearWeatherTime();
    }

    public static RegistryKey<World> getDimensionFromIndex(int index) {
        return dimensionMap.get(index);
    }

    public void saveBlockState(BlockPos pos, BlockState state, RegistryKey<World> dimension) {
        ChunkPos chunkPos = new ChunkPos(pos);
        SavedBlock saved = new SavedBlock(pos, state, dimension);
        savedBlocksByChunk.computeIfAbsent(chunkPos, k -> new ArrayList<>()).add(saved);
        blockPositions.add(pos);
        blockStates_LEGACY.add(state);
        blockDimensionIndices.put(pos, getDimensionIndex(dimension));
    }

    public Map<ChunkPos, List<SavedBlock>> getSavedBlocksByChunk() {
        return this.savedBlocksByChunk;
    }

    public Map<BlockPos, NbtCompound> getBlockEntityData() {
        return this.blockEntityData;
    }

    public void saveBlockEntity(BlockPos pos, NbtCompound blockEntityNBT) {
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

        public NbtCompound toNBT() {
            NbtCompound tag = new NbtCompound();
            tag.put(KEY_POS, NbtHelper.fromBlockPos(pos));
            tag.putLong(KEY_TICK_TIME, tickTime);
            return tag;
        }

        public static LightningStrike fromNBT(NbtCompound tag) {
            BlockPos pos = BlockPos.fromLong(tag.getLong(KEY_POS)); // NbtHelper logic
            // Actually NbtHelper.toBlockPos(tag.getCompound(KEY_POS)) if stored as compound
            if (tag.contains(KEY_POS, NbtElement.COMPOUND_TYPE)) {
                pos = NbtHelper.toBlockPos(tag.getCompound(KEY_POS));
            }

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
    public Set<BlockPos> getModifiedBlocks() { return this.modifiedBlocks; }
    public Map<BlockPos, BlockState> getMinedBlocks() { return this.minedBlocks; }
    public Set<BlockPos> getAddedEyes() { return this.addedEyes; }
    public Set<BlockPos> getModifiedFluidBlocks() { return this.modifiedFluidBlocks; }
    public Map<BlockPos, BlockState> getMinedFluidBlocks() { return this.minedFluidBlocks; }
    public Set<BlockPos> getCreatedPortals() { return this.createdPortals; }
    public Map<BlockPos, BlockState> getDestroyedPortals() { return this.destroyedPortals; }

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

    public void saveAllLoadedChunks(ServerWorld world) {
        ServerChunkManager chunkCache = world.getChunkManager();
        RegistryKey<World> currentDimension = world.getRegistryKey();
        int renderDistance = world.getServer().getPlayerManager().getViewDistance();

        for (ServerPlayerEntity player : world.getPlayers()) {
            BlockPos playerPos = player.getBlockPos();
            int playerChunkX = playerPos.getX() >> 4;
            int playerChunkZ = playerPos.getZ() >> 4;

            for (int dx = -renderDistance; dx <= renderDistance; dx++) {
                for (int dz = -renderDistance; dz <= renderDistance; dz++) {
                    int chunkX = playerChunkX + dx;
                    int chunkZ = playerChunkZ + dz;

                    WorldChunk chunk = chunkCache.getWorldChunk(chunkX, chunkZ);
                    if (chunk == null) continue;

                    ChunkPos chunkPos = chunk.getPos();
                    if (!processedChunks.add(chunkPos)) continue;
                    for (BlockPos entityPos : chunk.getBlockEntities().keySet()) {
                        BlockEntity blockEntity = chunk.getBlockEntity(entityPos);
                        if (blockEntity != null && !blockEntity.isRemoved()) {
                            BlockPos immutablePos = entityPos.toImmutable();
                            saveBlockEntity(immutablePos, blockEntity.createNbtWithId(world.getRegistryManager()));
                            blockDimensionIndices.put(immutablePos, getDimensionIndex(currentDimension));
                        }
                    }
                }
            }
        }
    }

    public NbtCompound saveToNBT(NbtCompound nbt) {
        nbt.putBoolean(KEY_WD_IS_RAINING, this.isRaining);
        nbt.putBoolean(KEY_WD_IS_THUNDERING, this.isThundering);
        nbt.putInt(KEY_WD_RAIN_TIME, this.rainTime);
        nbt.putInt(KEY_WD_THUNDER_TIME, this.thunderTime);
        nbt.putInt(KEY_WD_CLEAR_TIME, this.clearTime);
        nbt.putLong(KEY_WD_CHECKPOINT_TICK, this.checkpointTick);
        nbt.putLong(KEY_WD_DAY_TIME, this.dayTime);
        nbt.putLong(KEY_WD_GAME_TIME, this.gameTime);
        NbtList beListTag = new NbtList();
        for (Map.Entry<BlockPos, NbtCompound> entry : this.blockEntityData.entrySet()) {
            NbtCompound beEntryTag = new NbtCompound();
            beEntryTag.put(KEY_MAP_KEY_POS, NbtHelper.fromBlockPos(entry.getKey()));
            beEntryTag.put(KEY_MAP_VALUE_NBT, entry.getValue());
            beListTag.add(beEntryTag);
        }
        nbt.put(KEY_WD_BLOCK_ENTITY_DATA, beListTag);
        NbtList savedBlocksByChunkList = new NbtList();
        for (Map.Entry<ChunkPos, List<SavedBlock>> entry : this.savedBlocksByChunk.entrySet()) {
            NbtCompound chunkEntryTag = new NbtCompound();
            chunkEntryTag.putInt(KEY_CHUNK_X, entry.getKey().x);
            chunkEntryTag.putInt(KEY_CHUNK_Z, entry.getKey().z);
            NbtList blocksInChunkTag = new NbtList();
            for (SavedBlock savedBlock : entry.getValue()) {
                blocksInChunkTag.add(savedBlock.toNBT());
            }
            chunkEntryTag.put(KEY_BLOCKS_IN_CHUNK, blocksInChunkTag);
            savedBlocksByChunkList.add(chunkEntryTag);
        }
        nbt.put(KEY_WD_SAVED_BLOCKS_BY_CHUNK, savedBlocksByChunkList);
        NbtList lightningListTag = new NbtList();
        for (LightningStrike strike : this.savedLightnings) {
            lightningListTag.add(strike.toNBT());
        }
        nbt.put(KEY_WD_SAVED_LIGHTNINGS, lightningListTag);
        NbtList bpListTag = new NbtList();
        for (BlockPos pos : this.blockPositions) {
            bpListTag.add(NbtHelper.fromBlockPos(pos));
        }
        nbt.put(KEY_WD_BLOCK_POSITIONS, bpListTag);
        NbtList bdiListTag = new NbtList();
        for (Map.Entry<BlockPos, Integer> entry : this.blockDimensionIndices.entrySet()) {
            NbtCompound bdiEntryTag = new NbtCompound();
            bdiEntryTag.put(KEY_MAP_KEY_POS, NbtHelper.fromBlockPos(entry.getKey()));
            bdiEntryTag.putInt(KEY_MAP_VALUE_INT, entry.getValue());
            bdiListTag.add(bdiEntryTag);
        }
        nbt.put(KEY_WD_BLOCK_DIMENSION_INDICES, bdiListTag);
        NbtList mbListTag = new NbtList();
        for (BlockPos pos : this.modifiedBlocks) {
            mbListTag.add(NbtHelper.fromBlockPos(pos));
        }
        nbt.put(KEY_WD_MODIFIED_BLOCKS, mbListTag);
        NbtList minedBListTag = new NbtList();
        for (Map.Entry<BlockPos, BlockState> entry : this.minedBlocks.entrySet()) {
            NbtCompound minedBEntryTag = new NbtCompound();
            minedBEntryTag.put(KEY_MAP_KEY_POS, NbtHelper.fromBlockPos(entry.getKey()));
            minedBEntryTag.put(KEY_MAP_VALUE_STATE, NbtHelper.fromBlockState(entry.getValue()));
            minedBListTag.add(minedBEntryTag);
        }
        nbt.put(KEY_WD_MINED_BLOCKS, minedBListTag);
        NbtList aeListTag = new NbtList();
        for (BlockPos pos : this.addedEyes) {
            aeListTag.add(NbtHelper.fromBlockPos(pos));
        }
        nbt.put(KEY_WD_ADDED_EYES, aeListTag);
        NbtList mfbListTag = new NbtList();
        for (BlockPos pos : this.modifiedFluidBlocks) {
            mfbListTag.add(NbtHelper.fromBlockPos(pos));
        }
        nbt.put(KEY_WD_MODIFIED_FLUID_BLOCKS, mfbListTag);
        NbtList minedFbListTag = new NbtList();
        for (Map.Entry<BlockPos, BlockState> entry : this.minedFluidBlocks.entrySet()) {
            NbtCompound minedFbEntryTag = new NbtCompound();
            minedFbEntryTag.put(KEY_MAP_KEY_POS, NbtHelper.fromBlockPos(entry.getKey()));
            minedFbEntryTag.put(KEY_MAP_VALUE_STATE, NbtHelper.fromBlockState(entry.getValue()));
            minedFbListTag.add(minedFbEntryTag);
        }
        nbt.put(KEY_WD_MINED_FLUID_BLOCKS, minedFbListTag);
        NbtList cpListTag = new NbtList();
        for (BlockPos pos : this.createdPortals) {
            cpListTag.add(NbtHelper.fromBlockPos(pos));
        }
        nbt.put(KEY_WD_CREATED_PORTALS, cpListTag);
        NbtList dpListTag = new NbtList();
        for (Map.Entry<BlockPos, BlockState> entry : this.destroyedPortals.entrySet()) {
            NbtCompound dpEntryTag = new NbtCompound();
            dpEntryTag.put(KEY_MAP_KEY_POS, NbtHelper.fromBlockPos(entry.getKey()));
            dpEntryTag.put(KEY_MAP_VALUE_STATE, NbtHelper.fromBlockState(entry.getValue()));
            dpListTag.add(dpEntryTag);
        }
        nbt.put(KEY_WD_DESTROYED_PORTALS, dpListTag);
        NbtList nfListTag = new NbtList();
        for (BlockPos pos : this.newFires) {
            nfListTag.add(NbtHelper.fromBlockPos(pos));
        }
        nbt.put(KEY_WD_NEW_FIRES, nfListTag);

        return nbt;
    }

    public void loadFromNBT(NbtCompound nbt, RegistryWrapper.WrapperLookup lookupProvider) {
        this.isRaining = nbt.getBoolean(KEY_WD_IS_RAINING);
        this.isThundering = nbt.getBoolean(KEY_WD_IS_THUNDERING);
        this.rainTime = nbt.getInt(KEY_WD_RAIN_TIME);
        this.thunderTime = nbt.getInt(KEY_WD_THUNDER_TIME);
        this.clearTime = nbt.getInt(KEY_WD_CLEAR_TIME);
        this.checkpointTick = nbt.getLong(KEY_WD_CHECKPOINT_TICK);
        this.dayTime = nbt.getLong(KEY_WD_DAY_TIME);
        this.gameTime = nbt.getLong(KEY_WD_GAME_TIME);
        this.blockEntityData.clear();
        if (nbt.contains(KEY_WD_BLOCK_ENTITY_DATA, NbtElement.LIST_TYPE)) {
            NbtList beListTag = nbt.getList(KEY_WD_BLOCK_ENTITY_DATA, NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < beListTag.size(); i++) {
                NbtCompound beEntryTag = beListTag.getCompound(i);
                BlockPos pos = NbtHelper.toBlockPos(beEntryTag.getCompound(KEY_MAP_KEY_POS));
                NbtCompound valueNbt = beEntryTag.getCompound(KEY_MAP_VALUE_NBT);
                this.blockEntityData.put(pos, valueNbt);
            }
        }
        this.savedBlocksByChunk.clear();
        if (nbt.contains(KEY_WD_SAVED_BLOCKS_BY_CHUNK, NbtElement.LIST_TYPE)) {
            NbtList savedBlocksByChunkList = nbt.getList(KEY_WD_SAVED_BLOCKS_BY_CHUNK, NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < savedBlocksByChunkList.size(); i++) {
                NbtCompound chunkEntryTag = savedBlocksByChunkList.getCompound(i);
                ChunkPos chunkPos = new ChunkPos(chunkEntryTag.getInt(KEY_CHUNK_X), chunkEntryTag.getInt(KEY_CHUNK_Z));
                NbtList blocksInChunkTag = chunkEntryTag.getList(KEY_BLOCKS_IN_CHUNK, NbtElement.COMPOUND_TYPE);
                List<SavedBlock> blocks = new ArrayList<>();
                for (int j = 0; j < blocksInChunkTag.size(); j++) {
                    blocks.add(SavedBlock.fromNBT(blocksInChunkTag.getCompound(j), lookupProvider));
                }
                this.savedBlocksByChunk.put(chunkPos, blocks);
            }
        }
        this.savedLightnings.clear();
        if (nbt.contains(KEY_WD_SAVED_LIGHTNINGS, NbtElement.LIST_TYPE)) {
            NbtList lightningListTag = nbt.getList(KEY_WD_SAVED_LIGHTNINGS, NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < lightningListTag.size(); i++) {
                this.savedLightnings.add(LightningStrike.fromNBT(lightningListTag.getCompound(i)));
            }
        }
        
        HolderGetter<Block> blockHolderGetter = lookupProvider.getWrapperOrThrow(RegistryKeys.BLOCK);

        this.blockPositions.clear();
        if (nbt.contains(KEY_WD_BLOCK_POSITIONS, NbtElement.LIST_TYPE)) {
            NbtList listTag = nbt.getList(KEY_WD_BLOCK_POSITIONS, NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < listTag.size(); i++) {
                this.blockPositions.add(NbtHelper.toBlockPos(listTag.getCompound(i)));
            }
        }

        this.blockDimensionIndices.clear();
        if (nbt.contains(KEY_WD_BLOCK_DIMENSION_INDICES, NbtElement.LIST_TYPE)) {
            NbtList listTag = nbt.getList(KEY_WD_BLOCK_DIMENSION_INDICES, NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < listTag.size(); i++) {
                NbtCompound entryTag = listTag.getCompound(i);
                this.blockDimensionIndices.put(
                        NbtHelper.toBlockPos(entryTag.getCompound(KEY_MAP_KEY_POS)),
                        entryTag.getInt(KEY_MAP_VALUE_INT)
                );
            }
        }

        this.modifiedBlocks.clear();
        if (nbt.contains(KEY_WD_MODIFIED_BLOCKS, NbtElement.LIST_TYPE)) {
            NbtList listTag = nbt.getList(KEY_WD_MODIFIED_BLOCKS, NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < listTag.size(); i++) {
                this.modifiedBlocks.add(NbtHelper.toBlockPos(listTag.getCompound(i)));
            }
        }

        this.minedBlocks.clear();
        if (nbt.contains(KEY_WD_MINED_BLOCKS, NbtElement.LIST_TYPE)) {
            NbtList listTag = nbt.getList(KEY_WD_MINED_BLOCKS, NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < listTag.size(); i++) {
                NbtCompound entryTag = listTag.getCompound(i);
                this.minedBlocks.put(
                        NbtHelper.toBlockPos(entryTag.getCompound(KEY_MAP_KEY_POS)),
                        NbtHelper.toBlockState(blockHolderGetter, entryTag.getCompound(KEY_MAP_VALUE_STATE))
                );
            }
        }

        this.addedEyes.clear();
        if (nbt.contains(KEY_WD_ADDED_EYES, NbtElement.LIST_TYPE)) {
            NbtList listTag = nbt.getList(KEY_WD_ADDED_EYES, NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < listTag.size(); i++) {
                this.addedEyes.add(NbtHelper.toBlockPos(listTag.getCompound(i)));
            }
        }

        this.modifiedFluidBlocks.clear();
        if (nbt.contains(KEY_WD_MODIFIED_FLUID_BLOCKS, NbtElement.LIST_TYPE)) {
            NbtList listTag = nbt.getList(KEY_WD_MODIFIED_FLUID_BLOCKS, NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < listTag.size(); i++) {
                this.modifiedFluidBlocks.add(NbtHelper.toBlockPos(listTag.getCompound(i)));
            }
        }

        this.minedFluidBlocks.clear();
        if (nbt.contains(KEY_WD_MINED_FLUID_BLOCKS, NbtElement.LIST_TYPE)) {
            NbtList listTag = nbt.getList(KEY_WD_MINED_FLUID_BLOCKS, NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < listTag.size(); i++) {
                NbtCompound entryTag = listTag.getCompound(i);
                this.minedFluidBlocks.put(
                        NbtHelper.toBlockPos(entryTag.getCompound(KEY_MAP_KEY_POS)),
                        NbtHelper.toBlockState(blockHolderGetter, entryTag.getCompound(KEY_MAP_VALUE_STATE))
                );
            }
        }

        this.createdPortals.clear();
        if (nbt.contains(KEY_WD_CREATED_PORTALS, NbtElement.LIST_TYPE)) {
            NbtList listTag = nbt.getList(KEY_WD_CREATED_PORTALS, NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < listTag.size(); i++) {
                this.createdPortals.add(NbtHelper.toBlockPos(listTag.getCompound(i)));
            }
        }

        this.destroyedPortals.clear();
        if (nbt.contains(KEY_WD_DESTROYED_PORTALS, NbtElement.LIST_TYPE)) {
            NbtList listTag = nbt.getList(KEY_WD_DESTROYED_PORTALS, NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < listTag.size(); i++) {
                NbtCompound entryTag = listTag.getCompound(i);
                this.destroyedPortals.put(
                        NbtHelper.toBlockPos(entryTag.getCompound(KEY_MAP_KEY_POS)),
                        NbtHelper.toBlockState(blockHolderGetter, entryTag.getCompound(KEY_MAP_VALUE_STATE))
                );
            }
        }

        this.newFires.clear();
        if (nbt.contains(KEY_WD_NEW_FIRES, NbtElement.LIST_TYPE)) {
            NbtList listTag = nbt.getList(KEY_WD_NEW_FIRES, NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < listTag.size(); i++) {
                this.newFires.add(NbtHelper.toBlockPos(listTag.getCompound(i)));
            }
        }
        this.blockStates_LEGACY.clear();
        if (nbt.contains(KEY_WD_BLOCK_STATES_LEGACY, NbtElement.LIST_TYPE)) {
            NbtList listTag = nbt.getList(KEY_WD_BLOCK_STATES_LEGACY, NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < listTag.size(); i++) {
                this.blockStates_LEGACY.add(NbtHelper.toBlockState(blockHolderGetter, listTag.getCompound(i)));
            }
        }
    }

    public static record SavedBlock(BlockPos pos, BlockState state, RegistryKey<World> dimension) {

        public NbtCompound toNBT() {
            NbtCompound tag = new NbtCompound();
            tag.put(WorldData.KEY_POS, NbtHelper.fromBlockPos(pos));
            tag.put(WorldData.KEY_STATE, NbtHelper.fromBlockState(state));
            tag.putString(WorldData.KEY_DIMENSION, dimension.getValue().toString());
            return tag;
        }

        public static SavedBlock fromNBT(NbtCompound tag, RegistryWrapper.WrapperLookup lookupProvider) {
            BlockPos pos = NbtHelper.toBlockPos(tag.getCompound(WorldData.KEY_POS));
            HolderGetter<Block> blockHolderGetter = lookupProvider.getWrapperOrThrow(RegistryKeys.BLOCK);
            BlockState state = NbtHelper.toBlockState(blockHolderGetter, tag.getCompound(WorldData.KEY_STATE));

            RegistryKey<World> dimension = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(tag.getString(WorldData.KEY_DIMENSION)));
            return new SavedBlock(pos, state, dimension);
        }
    }
}
