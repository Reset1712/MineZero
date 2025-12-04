package boomcow.minezero.checkpoint;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class WorldData {

    private static final Logger LOGGER_WD = LogManager.getLogger("MineZeroWorldData");

    // NBT Keys
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

    // Inner keys
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

    // Legacy Support List
    private List<IBlockState> blockStates_LEGACY = new ArrayList<>();

    private boolean isRaining;
    private boolean isThundering;
    private int rainTime;
    private int thunderTime;
    private int clearTime;
    private Map<BlockPos, NBTTagCompound> blockEntityData = new HashMap<>();
    private Set<ChunkPos> processedChunks = new HashSet<>();
    private Map<ChunkPos, List<SavedBlock>> savedBlocksByChunk = new HashMap<>();
    private long checkpointTick;
    private long dayTime;
    private long gameTime;
    private final List<LightningStrike> savedLightnings = new ArrayList<>();

    public final List<BlockPos> blockPositions = new ArrayList<>();
    public final Map<BlockPos, Integer> blockDimensionIndices = new HashMap<>();
    public final Set<BlockPos> modifiedBlocks = new HashSet<>();
    public final Map<BlockPos, IBlockState> minedBlocks = new HashMap<>();
    public final Set<BlockPos> addedEyes = new HashSet<>();
    public final Set<BlockPos> modifiedFluidBlocks = new HashSet<>();
    public final Map<BlockPos, IBlockState> minedFluidBlocks = new HashMap<>();
    public final Set<BlockPos> createdPortals = new HashSet<>();
    public final Map<BlockPos, IBlockState> destroyedPortals = new HashMap<>();
    public List<BlockPos> newFires = new ArrayList<>();

    // 1.12.2 uses Integers for dimensions, not ResourceKeys
    private static final Map<Integer, Integer> dimensionMap = new HashMap<>();
    private static int nextDimensionIndex = 0;

    public WorldData() {
        this.blockEntityData = new HashMap<>();
        this.dayTime = 0;
    }

    public List<BlockPos> getNewFires() {
        return newFires;
    }

    public void saveCheckpointTick(long checkpointTick) {
        this.checkpointTick = checkpointTick;
    }

    public long getCheckpointTick() {
        return this.checkpointTick;
    }

    /**
     * Returns an index for the given dimension ID, creating one if necessary.
     */
    public static int getDimensionIndex(int dimensionID) {
        for (Map.Entry<Integer, Integer> entry : dimensionMap.entrySet()) {
            if (entry.getValue().equals(dimensionID)) {
                return entry.getKey();
            }
        }
        int newIndex = nextDimensionIndex++;
        dimensionMap.put(newIndex, dimensionID);
        return newIndex;
    }

    public static int getDimensionFromIndex(int index) {
        return dimensionMap.getOrDefault(index, 0); // Default to 0 (Overworld) if not found
    }

    public void saveWeather(World world) {
        WorldInfo info = world.getWorldInfo();
        this.isRaining = info.isRaining();
        this.isThundering = info.isThundering();
        this.rainTime = info.getRainTime();
        this.thunderTime = info.getThunderTime();
        this.clearTime = info.getCleanWeatherTime();
    }


    /**
     * Saves a block state.
     * Note: dimension is now an int (ID).
     */
    public void saveBlockState(BlockPos pos, IBlockState state, int dimensionID) {
        ChunkPos chunkPos = new ChunkPos(pos);
        SavedBlock saved = new SavedBlock(pos, state, dimensionID);
        savedBlocksByChunk.computeIfAbsent(chunkPos, k -> new ArrayList<>()).add(saved);
        blockPositions.add(pos);
        blockStates_LEGACY.add(state);
        blockDimensionIndices.put(pos, getDimensionIndex(dimensionID));
    }

    public Map<ChunkPos, List<SavedBlock>> getSavedBlocksByChunk() {
        return this.savedBlocksByChunk;
    }

    public Map<BlockPos, NBTTagCompound> getBlockEntityData() {
        return this.blockEntityData;
    }

    public void saveBlockEntity(BlockPos pos, NBTTagCompound blockEntityNBT) {
        if (blockEntityNBT != null) {
            this.blockEntityData.put(pos, blockEntityNBT);
        }
    }

    public void saveDayTime(long dayTime) {
        this.dayTime = dayTime;
    }

    public long getDayTime() { return this.dayTime; }
    public long getGameTime() { return this.gameTime; }
    public void saveGameTime(long gameTime) { this.gameTime = gameTime; }

    public boolean isRaining() { return this.isRaining; }
    public boolean isThundering() { return this.isThundering; }
    public int getRainTime() { return this.rainTime; }
    public int getThunderTime() { return this.thunderTime; }
    public int getClearTime() { return this.clearTime; }

    public List<IBlockState> getBlockStates() { return this.blockStates_LEGACY; }
    public List<BlockPos> getBlockPositions() { return blockPositions; }

    public List<BlockPos> getInstanceBlockPositions() { return this.blockPositions; }
    public Map<BlockPos, Integer> getInstanceBlockDimensionIndices() { return this.blockDimensionIndices; }
    public Set<BlockPos> getModifiedBlocks() { return this.modifiedBlocks; }
    public Map<BlockPos, IBlockState> getMinedBlocks() { return this.minedBlocks; }
    public Set<BlockPos> getAddedEyes() { return this.addedEyes; }
    public Set<BlockPos> getModifiedFluidBlocks() { return this.modifiedFluidBlocks; }
    public Map<BlockPos, IBlockState> getMinedFluidBlocks() { return this.minedFluidBlocks; }
    public Set<BlockPos> getCreatedPortals() { return this.createdPortals; }
    public Map<BlockPos, IBlockState> getDestroyedPortals() { return this.destroyedPortals; }

    /**
     * Lightning Strike Class (Converted from Record/Inner Class)
     */
    public static class LightningStrike {
        public final BlockPos pos;
        public final long tickTime;

        public LightningStrike(BlockPos pos, long tickTime) {
            this.pos = pos;
            this.tickTime = tickTime;
        }

        public NBTTagCompound toNBT() {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setTag(KEY_POS, NBTUtil.createPosTag(pos));
            tag.setLong(KEY_TICK_TIME, tickTime);
            return tag;
        }

        public static LightningStrike fromNBT(NBTTagCompound tag) {
            BlockPos pos = NBTUtil.getPosFromTag(tag.getCompoundTag(KEY_POS));
            long tickTime = tag.getLong(KEY_TICK_TIME);
            return new LightningStrike(pos, tickTime);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LightningStrike that = (LightningStrike) o;
            return tickTime == that.tickTime && Objects.equals(pos, that.pos);
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
     * Saves all loaded chunks.
     * Updated for 1.12.2 Chunk Logic using ChunkProviderServer.
     */
    public void saveAllLoadedChunks(WorldServer world) {
        int dimensionID = world.provider.getDimension();
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        PlayerList playerList = server.getPlayerList();
        int renderDistance = playerList.getViewDistance();

        // access the server chunk provider directly
        net.minecraft.world.gen.ChunkProviderServer chunkProvider = world.getChunkProvider();

        // Iterate through players to find relevant chunks
        for (Object obj : playerList.getPlayers()) {
            if(!(obj instanceof net.minecraft.entity.player.EntityPlayerMP)) continue;
            net.minecraft.entity.player.EntityPlayerMP player = (net.minecraft.entity.player.EntityPlayerMP) obj;

            if (player.world.provider.getDimension() != dimensionID) continue;

            BlockPos playerPos = player.getPosition();
            int playerChunkX = playerPos.getX() >> 4;
            int playerChunkZ = playerPos.getZ() >> 4;

            for (int dx = -renderDistance; dx <= renderDistance; dx++) {
                for (int dz = -renderDistance; dz <= renderDistance; dz++) {
                    int chunkX = playerChunkX + dx;
                    int chunkZ = playerChunkZ + dz;

                    // In 1.12.2, getLoadedChunk returns null if the chunk isn't loaded.
                    // This is cleaner than checking chunkExists() and then getting it.
                    Chunk chunk = chunkProvider.getLoadedChunk(chunkX, chunkZ);

                    if (chunk == null) continue;

                    ChunkPos chunkPos = chunk.getPos();

                    if (!processedChunks.add(chunkPos)) continue;

                    for (Map.Entry<BlockPos, TileEntity> entry : chunk.getTileEntityMap().entrySet()) {
                        TileEntity blockEntity = entry.getValue();
                        if (blockEntity != null && !blockEntity.isInvalid()) {
                            BlockPos immutablePos = entry.getKey().toImmutable();
                            NBTTagCompound nbt = new NBTTagCompound();
                            blockEntity.writeToNBT(nbt);
                            saveBlockEntity(immutablePos, nbt);
                            blockDimensionIndices.put(immutablePos, getDimensionIndex(dimensionID));
                        }
                    }
                }
            }
        }
    }


    public NBTTagCompound saveToNBT(NBTTagCompound nbt) {
        nbt.setBoolean(KEY_WD_IS_RAINING, this.isRaining);
        nbt.setBoolean(KEY_WD_IS_THUNDERING, this.isThundering);
        nbt.setInteger(KEY_WD_RAIN_TIME, this.rainTime);
        nbt.setInteger(KEY_WD_THUNDER_TIME, this.thunderTime);
        nbt.setInteger(KEY_WD_CLEAR_TIME, this.clearTime);
        nbt.setLong(KEY_WD_CHECKPOINT_TICK, this.checkpointTick);
        nbt.setLong(KEY_WD_DAY_TIME, this.dayTime);
        nbt.setLong(KEY_WD_GAME_TIME, this.gameTime);

        NBTTagList beListTag = new NBTTagList();
        for (Map.Entry<BlockPos, NBTTagCompound> entry : this.blockEntityData.entrySet()) {
            NBTTagCompound beEntryTag = new NBTTagCompound();
            beEntryTag.setTag(KEY_MAP_KEY_POS, NBTUtil.createPosTag(entry.getKey()));
            beEntryTag.setTag(KEY_MAP_VALUE_NBT, entry.getValue());
            beListTag.appendTag(beEntryTag);
        }
        nbt.setTag(KEY_WD_BLOCK_ENTITY_DATA, beListTag);

        NBTTagList savedBlocksByChunkList = new NBTTagList();
        for (Map.Entry<ChunkPos, List<SavedBlock>> entry : this.savedBlocksByChunk.entrySet()) {
            NBTTagCompound chunkEntryTag = new NBTTagCompound();
            chunkEntryTag.setInteger(KEY_CHUNK_X, entry.getKey().x);
            chunkEntryTag.setInteger(KEY_CHUNK_Z, entry.getKey().z);
            NBTTagList blocksInChunkTag = new NBTTagList();
            for (SavedBlock savedBlock : entry.getValue()) {
                blocksInChunkTag.appendTag(savedBlock.toNBT());
            }
            chunkEntryTag.setTag(KEY_BLOCKS_IN_CHUNK, blocksInChunkTag);
            savedBlocksByChunkList.appendTag(chunkEntryTag);
        }
        nbt.setTag(KEY_WD_SAVED_BLOCKS_BY_CHUNK, savedBlocksByChunkList);

        NBTTagList lightningListTag = new NBTTagList();
        for (LightningStrike strike : this.savedLightnings) {
            lightningListTag.appendTag(strike.toNBT());
        }
        nbt.setTag(KEY_WD_SAVED_LIGHTNINGS, lightningListTag);

        NBTTagList bpListTag = new NBTTagList();
        for (BlockPos pos : this.blockPositions) {
            bpListTag.appendTag(NBTUtil.createPosTag(pos));
        }
        nbt.setTag(KEY_WD_BLOCK_POSITIONS, bpListTag);

        NBTTagList bdiListTag = new NBTTagList();
        for (Map.Entry<BlockPos, Integer> entry : this.blockDimensionIndices.entrySet()) {
            NBTTagCompound bdiEntryTag = new NBTTagCompound();
            bdiEntryTag.setTag(KEY_MAP_KEY_POS, NBTUtil.createPosTag(entry.getKey()));
            bdiEntryTag.setInteger(KEY_MAP_VALUE_INT, entry.getValue());
            bdiListTag.appendTag(bdiEntryTag);
        }
        nbt.setTag(KEY_WD_BLOCK_DIMENSION_INDICES, bdiListTag);

        NBTTagList mbListTag = new NBTTagList();
        for (BlockPos pos : this.modifiedBlocks) {
            mbListTag.appendTag(NBTUtil.createPosTag(pos));
        }
        nbt.setTag(KEY_WD_MODIFIED_BLOCKS, mbListTag);

        NBTTagList minedBListTag = new NBTTagList();
        for (Map.Entry<BlockPos, IBlockState> entry : this.minedBlocks.entrySet()) {
            NBTTagCompound minedBEntryTag = new NBTTagCompound();
            minedBEntryTag.setTag(KEY_MAP_KEY_POS, NBTUtil.createPosTag(entry.getKey()));
            NBTTagCompound stateTag = new NBTTagCompound();
            NBTUtil.writeBlockState(stateTag, entry.getValue());
            minedBEntryTag.setTag(KEY_MAP_VALUE_STATE, stateTag);
            minedBListTag.appendTag(minedBEntryTag);
        }
        nbt.setTag(KEY_WD_MINED_BLOCKS, minedBListTag);

        NBTTagList aeListTag = new NBTTagList();
        for (BlockPos pos : this.addedEyes) {
            aeListTag.appendTag(NBTUtil.createPosTag(pos));
        }
        nbt.setTag(KEY_WD_ADDED_EYES, aeListTag);

        NBTTagList mfbListTag = new NBTTagList();
        for (BlockPos pos : this.modifiedFluidBlocks) {
            mfbListTag.appendTag(NBTUtil.createPosTag(pos));
        }
        nbt.setTag(KEY_WD_MODIFIED_FLUID_BLOCKS, mfbListTag);

        NBTTagList minedFbListTag = new NBTTagList();
        for (Map.Entry<BlockPos, IBlockState> entry : this.minedFluidBlocks.entrySet()) {
            NBTTagCompound minedFbEntryTag = new NBTTagCompound();
            minedFbEntryTag.setTag(KEY_MAP_KEY_POS, NBTUtil.createPosTag(entry.getKey()));
            NBTTagCompound stateTag = new NBTTagCompound();
            NBTUtil.writeBlockState(stateTag, entry.getValue());
            minedFbEntryTag.setTag(KEY_MAP_VALUE_STATE, stateTag);
            minedFbListTag.appendTag(minedFbEntryTag);
        }
        nbt.setTag(KEY_WD_MINED_FLUID_BLOCKS, minedFbListTag);

        NBTTagList cpListTag = new NBTTagList();
        for (BlockPos pos : this.createdPortals) {
            cpListTag.appendTag(NBTUtil.createPosTag(pos));
        }
        nbt.setTag(KEY_WD_CREATED_PORTALS, cpListTag);

        NBTTagList dpListTag = new NBTTagList();
        for (Map.Entry<BlockPos, IBlockState> entry : this.destroyedPortals.entrySet()) {
            NBTTagCompound dpEntryTag = new NBTTagCompound();
            dpEntryTag.setTag(KEY_MAP_KEY_POS, NBTUtil.createPosTag(entry.getKey()));
            NBTTagCompound stateTag = new NBTTagCompound();
            NBTUtil.writeBlockState(stateTag, entry.getValue());
            dpEntryTag.setTag(KEY_MAP_VALUE_STATE, stateTag);
            dpListTag.appendTag(dpEntryTag);
        }
        nbt.setTag(KEY_WD_DESTROYED_PORTALS, dpListTag);

        NBTTagList nfListTag = new NBTTagList();
        for (BlockPos pos : this.newFires) {
            nfListTag.appendTag(NBTUtil.createPosTag(pos));
        }
        nbt.setTag(KEY_WD_NEW_FIRES, nfListTag);

        LOGGER_WD.debug("WorldData instance saved to NBT.");
        return nbt;
    }

    public void loadFromNBT(NBTTagCompound nbt) {
        this.isRaining = nbt.getBoolean(KEY_WD_IS_RAINING);
        this.isThundering = nbt.getBoolean(KEY_WD_IS_THUNDERING);
        this.rainTime = nbt.getInteger(KEY_WD_RAIN_TIME);
        this.thunderTime = nbt.getInteger(KEY_WD_THUNDER_TIME);
        this.clearTime = nbt.getInteger(KEY_WD_CLEAR_TIME);
        this.checkpointTick = nbt.getLong(KEY_WD_CHECKPOINT_TICK);
        this.dayTime = nbt.getLong(KEY_WD_DAY_TIME);
        this.gameTime = nbt.getLong(KEY_WD_GAME_TIME);

        this.blockEntityData.clear();
        if (nbt.hasKey(KEY_WD_BLOCK_ENTITY_DATA, 9)) { // 9 is List
            NBTTagList beListTag = nbt.getTagList(KEY_WD_BLOCK_ENTITY_DATA, 10); // 10 is Compound
            for (int i = 0; i < beListTag.tagCount(); i++) {
                NBTTagCompound beEntryTag = beListTag.getCompoundTagAt(i);
                BlockPos pos = NBTUtil.getPosFromTag(beEntryTag.getCompoundTag(KEY_MAP_KEY_POS));
                NBTTagCompound valueNbt = beEntryTag.getCompoundTag(KEY_MAP_VALUE_NBT);
                this.blockEntityData.put(pos, valueNbt);
            }
        }

        this.savedBlocksByChunk.clear();
        if (nbt.hasKey(KEY_WD_SAVED_BLOCKS_BY_CHUNK, 9)) {
            NBTTagList savedBlocksByChunkList = nbt.getTagList(KEY_WD_SAVED_BLOCKS_BY_CHUNK, 10);
            for (int i = 0; i < savedBlocksByChunkList.tagCount(); i++) {
                NBTTagCompound chunkEntryTag = savedBlocksByChunkList.getCompoundTagAt(i);
                ChunkPos chunkPos = new ChunkPos(chunkEntryTag.getInteger(KEY_CHUNK_X), chunkEntryTag.getInteger(KEY_CHUNK_Z));
                NBTTagList blocksInChunkTag = chunkEntryTag.getTagList(KEY_BLOCKS_IN_CHUNK, 10);
                List<SavedBlock> blocks = new ArrayList<>();
                for (int j = 0; j < blocksInChunkTag.tagCount(); j++) {
                    blocks.add(SavedBlock.fromNBT(blocksInChunkTag.getCompoundTagAt(j)));
                }
                this.savedBlocksByChunk.put(chunkPos, blocks);
            }
        }

        this.savedLightnings.clear();
        if (nbt.hasKey(KEY_WD_SAVED_LIGHTNINGS, 9)) {
            NBTTagList lightningListTag = nbt.getTagList(KEY_WD_SAVED_LIGHTNINGS, 10);
            for (int i = 0; i < lightningListTag.tagCount(); i++) {
                this.savedLightnings.add(LightningStrike.fromNBT(lightningListTag.getCompoundTagAt(i)));
            }
        }

        this.blockPositions.clear();
        if (nbt.hasKey(KEY_WD_BLOCK_POSITIONS, 9)) {
            NBTTagList listTag = nbt.getTagList(KEY_WD_BLOCK_POSITIONS, 10);
            for (int i = 0; i < listTag.tagCount(); i++) this.blockPositions.add(NBTUtil.getPosFromTag(listTag.getCompoundTagAt(i)));
        }

        this.blockDimensionIndices.clear();
        if (nbt.hasKey(KEY_WD_BLOCK_DIMENSION_INDICES, 9)) {
            NBTTagList listTag = nbt.getTagList(KEY_WD_BLOCK_DIMENSION_INDICES, 10);
            for (int i = 0; i < listTag.tagCount(); i++) {
                NBTTagCompound entryTag = listTag.getCompoundTagAt(i);
                this.blockDimensionIndices.put(NBTUtil.getPosFromTag(entryTag.getCompoundTag(KEY_MAP_KEY_POS)), entryTag.getInteger(KEY_MAP_VALUE_INT));
            }
        }

        this.modifiedBlocks.clear();
        if (nbt.hasKey(KEY_WD_MODIFIED_BLOCKS, 9)) {
            NBTTagList listTag = nbt.getTagList(KEY_WD_MODIFIED_BLOCKS, 10);
            for (int i = 0; i < listTag.tagCount(); i++) this.modifiedBlocks.add(NBTUtil.getPosFromTag(listTag.getCompoundTagAt(i)));
        }

        this.minedBlocks.clear();
        if (nbt.hasKey(KEY_WD_MINED_BLOCKS, 9)) {
            NBTTagList listTag = nbt.getTagList(KEY_WD_MINED_BLOCKS, 10);
            for (int i = 0; i < listTag.tagCount(); i++) {
                NBTTagCompound entryTag = listTag.getCompoundTagAt(i);
                this.minedBlocks.put(NBTUtil.getPosFromTag(entryTag.getCompoundTag(KEY_MAP_KEY_POS)), NBTUtil.readBlockState(entryTag.getCompoundTag(KEY_MAP_VALUE_STATE)));
            }
        }

        this.addedEyes.clear();
        if (nbt.hasKey(KEY_WD_ADDED_EYES, 9)) {
            NBTTagList listTag = nbt.getTagList(KEY_WD_ADDED_EYES, 10);
            for (int i = 0; i < listTag.tagCount(); i++) this.addedEyes.add(NBTUtil.getPosFromTag(listTag.getCompoundTagAt(i)));
        }

        this.modifiedFluidBlocks.clear();
        if (nbt.hasKey(KEY_WD_MODIFIED_FLUID_BLOCKS, 9)) {
            NBTTagList listTag = nbt.getTagList(KEY_WD_MODIFIED_FLUID_BLOCKS, 10);
            for (int i = 0; i < listTag.tagCount(); i++) this.modifiedFluidBlocks.add(NBTUtil.getPosFromTag(listTag.getCompoundTagAt(i)));
        }

        this.minedFluidBlocks.clear();
        if (nbt.hasKey(KEY_WD_MINED_FLUID_BLOCKS, 9)) {
            NBTTagList listTag = nbt.getTagList(KEY_WD_MINED_FLUID_BLOCKS, 10);
            for (int i = 0; i < listTag.tagCount(); i++) {
                NBTTagCompound entryTag = listTag.getCompoundTagAt(i);
                this.minedFluidBlocks.put(NBTUtil.getPosFromTag(entryTag.getCompoundTag(KEY_MAP_KEY_POS)), NBTUtil.readBlockState(entryTag.getCompoundTag(KEY_MAP_VALUE_STATE)));
            }
        }

        this.createdPortals.clear();
        if (nbt.hasKey(KEY_WD_CREATED_PORTALS, 9)) {
            NBTTagList listTag = nbt.getTagList(KEY_WD_CREATED_PORTALS, 10);
            for (int i = 0; i < listTag.tagCount(); i++) this.createdPortals.add(NBTUtil.getPosFromTag(listTag.getCompoundTagAt(i)));
        }

        this.destroyedPortals.clear();
        if (nbt.hasKey(KEY_WD_DESTROYED_PORTALS, 9)) {
            NBTTagList listTag = nbt.getTagList(KEY_WD_DESTROYED_PORTALS, 10);
            for (int i = 0; i < listTag.tagCount(); i++) {
                NBTTagCompound entryTag = listTag.getCompoundTagAt(i);
                this.destroyedPortals.put(NBTUtil.getPosFromTag(entryTag.getCompoundTag(KEY_MAP_KEY_POS)), NBTUtil.readBlockState(entryTag.getCompoundTag(KEY_MAP_VALUE_STATE)));
            }
        }

        this.newFires.clear();
        if (nbt.hasKey(KEY_WD_NEW_FIRES, 9)) {
            NBTTagList listTag = nbt.getTagList(KEY_WD_NEW_FIRES, 10);
            for (int i = 0; i < listTag.tagCount(); i++) this.newFires.add(NBTUtil.getPosFromTag(listTag.getCompoundTagAt(i)));
        }

        LOGGER_WD.debug("WorldData instance loaded from NBT.");
    }

    /**
     * SavedBlock Class (Converted from Java Record)
     */
    public static class SavedBlock {
        public final BlockPos pos;
        public final IBlockState state;
        public final int dimension; // In 1.12.2, dimensions are IDs (integers)

        public SavedBlock(BlockPos pos, IBlockState state, int dimension) {
            this.pos = pos;
            this.state = state;
            this.dimension = dimension;
        }

        public NBTTagCompound toNBT() {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setTag(KEY_POS, NBTUtil.createPosTag(pos));
            NBTTagCompound stateTag = new NBTTagCompound();
            NBTUtil.writeBlockState(stateTag, state);
            tag.setTag(KEY_STATE, stateTag);
            tag.setInteger(KEY_DIMENSION, dimension);
            return tag;
        }

        public static SavedBlock fromNBT(NBTTagCompound tag) {
            BlockPos pos = NBTUtil.getPosFromTag(tag.getCompoundTag(KEY_POS));
            IBlockState state = NBTUtil.readBlockState(tag.getCompoundTag(KEY_STATE));
            int dimension = tag.getInteger(KEY_DIMENSION);
            return new SavedBlock(pos, state, dimension);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SavedBlock that = (SavedBlock) o;
            return dimension == that.dimension &&
                    Objects.equals(pos, that.pos) &&
                    Objects.equals(state, that.state);
        }

        @Override
        public int hashCode() {
            return Objects.hash(pos, state, dimension);
        }
    }
}