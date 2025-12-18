package boomcow.minezero.checkpoint;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.ServerLevelData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class WorldData {

    private static final String KEY_WD_IS_RAINING = "wd_isRaining";
    private static final String KEY_WD_IS_THUNDERING = "wd_isThundering";
    private static final String KEY_WD_RAIN_TIME = "wd_rainTime";
    private static final String KEY_WD_THUNDER_TIME = "wd_thunderTime";
    private static final String KEY_WD_CLEAR_TIME = "wd_clearTime";
    private static final String KEY_WD_BLOCK_ENTITY_DATA = "wd_blockEntityData";
    private static final String KEY_WD_CHECKPOINT_TICK = "wd_checkpointTick";
    private static final String KEY_WD_DAY_TIME = "wd_dayTime";
    private static final String KEY_WD_GAME_TIME = "wd_gameTime";
    private static final String KEY_WD_SAVED_LIGHTNINGS = "wd_savedLightnings";
    private static final String KEY_WD_BLOCK_DIMENSION_INDICES = "wd_blockDimensionIndices";
    private static final String KEY_WD_MODIFIED_BLOCKS = "wd_modifiedBlocks";
    private static final String KEY_WD_MINED_BLOCKS = "wd_minedBlocks";
    private static final String KEY_WD_ADDED_EYES = "wd_addedEyes";
    private static final String KEY_WD_MODIFIED_FLUID_BLOCKS = "wd_modifiedFluidBlocks";
    private static final String KEY_WD_MINED_FLUID_BLOCKS = "wd_minedFluidBlocks";
    private static final String KEY_WD_NEW_FIRES = "wd_newFires";

    private static final String KEY_POS = "pos";
    private static final String KEY_MAP_KEY_POS = "keyPos";
    private static final String KEY_MAP_VALUE_INT = "valueInt";
    private static final String KEY_MAP_VALUE_STATE = "valueState";
    private static final String KEY_MAP_VALUE_NBT = "valueNbt";

    private boolean isRaining;
    private boolean isThundering;
    private int rainTime;
    private int thunderTime;
    private int clearTime;
    private long checkpointTick;
    private long dayTime;
    private long gameTime;

    private Map<BlockPos, CompoundTag> blockEntityData = new HashMap<>();
    private Set<ChunkPos> processedChunks = new HashSet<>();
    private Map<ChunkPos, List<SavedBlock>> savedBlocksByChunk = new HashMap<>(); 
    private final List<LightningStrike> savedLightnings = new ArrayList<>();

    public final Map<BlockPos, Integer> blockDimensionIndices = new HashMap<>();
    public final Set<BlockPos> modifiedBlocks = new HashSet<>();
    public final Map<BlockPos, BlockState> minedBlocks = new HashMap<>();
    public final Set<BlockPos> addedEyes = new HashSet<>();
    public final Set<BlockPos> modifiedFluidBlocks = new HashSet<>();
    public final Map<BlockPos, BlockState> minedFluidBlocks = new HashMap<>();
    public final List<BlockPos> newFires = new ArrayList<>();

    private static final Map<Integer, ResourceKey<Level>> dimensionMap = new HashMap<>();
    private static int nextDimensionIndex = 0;

    public WorldData() {}

    public Set<BlockPos> getModifiedBlocks() { return modifiedBlocks; }
    public Map<BlockPos, BlockState> getMinedBlocks() { return minedBlocks; }
    public Map<BlockPos, Integer> getInstanceBlockDimensionIndices() { return blockDimensionIndices; }
    public long getCheckpointTick() { return checkpointTick; }
    public void saveCheckpointTick(long tick) { this.checkpointTick = tick; }

    public static int getDimensionIndex(ResourceKey<Level> dimension) {
        for (Map.Entry<Integer, ResourceKey<Level>> entry : dimensionMap.entrySet()) {
            if (entry.getValue().equals(dimension)) return entry.getKey();
        }
        int newIndex = nextDimensionIndex++;
        dimensionMap.put(newIndex, dimension);
        return newIndex;
    }

    public static ResourceKey<Level> getDimensionFromIndex(int index) {
        return dimensionMap.get(index);
    }

    public void saveWeather(ServerLevel level) {
        this.isRaining = level.isRaining();
        this.isThundering = level.isThundering();
        if (level.getLevelData() instanceof ServerLevelData serverData) {
            this.rainTime = serverData.getRainTime();
            this.thunderTime = serverData.getThunderTime();
            this.clearTime = serverData.getClearWeatherTime();
        }
    }

    public void saveDayTime(long dayTime) { this.dayTime = dayTime; }
    public long getDayTime() { return this.dayTime; }
    public void saveGameTime(long gameTime) { this.gameTime = gameTime; }
    public long getGameTime() { return this.gameTime; }
    public boolean isRaining() { return isRaining; }
    public boolean isThundering() { return isThundering; }
    public int getRainTime() { return rainTime; }
    public int getThunderTime() { return thunderTime; }
    public int getClearTime() { return clearTime; }

    public void saveBlockEntity(BlockPos pos, CompoundTag tag) {
        if (tag != null) blockEntityData.put(pos, tag);
    }
    public Map<BlockPos, CompoundTag> getBlockEntityData() { return blockEntityData; }

    public void saveAllLoadedChunks(ServerLevel level) {
        ServerChunkCache chunkCache = level.getChunkSource();
        ResourceKey<Level> currentDimension = level.dimension();
        int renderDistance = level.getServer().getPlayerList().getViewDistance();

        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            ChunkPos center = player.chunkPosition();
            for (int dx = -renderDistance; dx <= renderDistance; dx++) {
                for (int dz = -renderDistance; dz <= renderDistance; dz++) {
                    LevelChunk chunk = chunkCache.getChunkNow(center.x + dx, center.z + dz);
                    if (chunk != null && processedChunks.add(chunk.getPos())) {
                        for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
                            saveBlockEntity(entry.getKey().immutable(), entry.getValue().saveWithFullMetadata());
                            blockDimensionIndices.put(entry.getKey().immutable(), getDimensionIndex(currentDimension));
                        }
                    }
                }
            }
        }
    }

    public void addLightningStrike(BlockPos pos, long tickTime) {
        savedLightnings.add(new LightningStrike(pos, tickTime));
    }
    public List<LightningStrike> getSavedLightnings() { return savedLightnings; }
    public Set<BlockPos> getAddedEyes() { return addedEyes; }
    public Set<BlockPos> getModifiedFluidBlocks() { return modifiedFluidBlocks; }
    public Map<BlockPos, BlockState> getMinedFluidBlocks() { return minedFluidBlocks; }
    public List<BlockPos> getNewFires() { return newFires; }
    public Map<ChunkPos, List<SavedBlock>> getSavedBlocksByChunk() { return savedBlocksByChunk; }

    public void clearWorldData() {
        minedBlocks.clear();
        modifiedBlocks.clear();
        minedFluidBlocks.clear();
        modifiedFluidBlocks.clear();
        blockDimensionIndices.clear();
        blockEntityData.clear();
        processedChunks.clear();
        savedBlocksByChunk.clear();
        savedLightnings.clear();
        addedEyes.clear();
        newFires.clear();
        isRaining = false;
        isThundering = false;
        rainTime = 0;
        thunderTime = 0;
        clearTime = 0;
        dayTime = 0;
        gameTime = 0;
        checkpointTick = 0;
    }

    public CompoundTag saveToNBT(CompoundTag nbt) {
        nbt.putBoolean(KEY_WD_IS_RAINING, isRaining);
        nbt.putBoolean(KEY_WD_IS_THUNDERING, isThundering);
        nbt.putInt(KEY_WD_RAIN_TIME, rainTime);
        nbt.putInt(KEY_WD_THUNDER_TIME, thunderTime);
        nbt.putInt(KEY_WD_CLEAR_TIME, clearTime);
        nbt.putLong(KEY_WD_CHECKPOINT_TICK, checkpointTick);
        nbt.putLong(KEY_WD_DAY_TIME, dayTime);
        nbt.putLong(KEY_WD_GAME_TIME, gameTime);

        ListTag mbList = new ListTag();
        for (BlockPos p : modifiedBlocks) mbList.add(NbtUtils.writeBlockPos(p));
        nbt.put(KEY_WD_MODIFIED_BLOCKS, mbList);

        ListTag minedList = new ListTag();
        for (Map.Entry<BlockPos, BlockState> e : minedBlocks.entrySet()) {
            CompoundTag tag = new CompoundTag();
            tag.put(KEY_MAP_KEY_POS, NbtUtils.writeBlockPos(e.getKey()));
            tag.put(KEY_MAP_VALUE_STATE, NbtUtils.writeBlockState(e.getValue()));
            minedList.add(tag);
        }
        nbt.put(KEY_WD_MINED_BLOCKS, minedList);

        ListTag dimList = new ListTag();
        for (Map.Entry<BlockPos, Integer> e : blockDimensionIndices.entrySet()) {
            CompoundTag tag = new CompoundTag();
            tag.put(KEY_MAP_KEY_POS, NbtUtils.writeBlockPos(e.getKey()));
            tag.putInt(KEY_MAP_VALUE_INT, e.getValue());
            dimList.add(tag);
        }
        nbt.put(KEY_WD_BLOCK_DIMENSION_INDICES, dimList);

        ListTag beList = new ListTag();
        for (Map.Entry<BlockPos, CompoundTag> e : blockEntityData.entrySet()) {
            CompoundTag tag = new CompoundTag();
            tag.put(KEY_MAP_KEY_POS, NbtUtils.writeBlockPos(e.getKey()));
            tag.put(KEY_MAP_VALUE_NBT, e.getValue());
            beList.add(tag);
        }
        nbt.put(KEY_WD_BLOCK_ENTITY_DATA, beList);
        
        return nbt;
    }

    public void loadFromNBT(CompoundTag nbt) {
        clearWorldData();
        isRaining = nbt.getBoolean(KEY_WD_IS_RAINING);
        isThundering = nbt.getBoolean(KEY_WD_IS_THUNDERING);
        rainTime = nbt.getInt(KEY_WD_RAIN_TIME);
        thunderTime = nbt.getInt(KEY_WD_THUNDER_TIME);
        clearTime = nbt.getInt(KEY_WD_CLEAR_TIME);
        checkpointTick = nbt.getLong(KEY_WD_CHECKPOINT_TICK);
        dayTime = nbt.getLong(KEY_WD_DAY_TIME);
        gameTime = nbt.getLong(KEY_WD_GAME_TIME);

        if (nbt.contains(KEY_WD_MODIFIED_BLOCKS, Tag.TAG_LIST)) {
            ListTag list = nbt.getList(KEY_WD_MODIFIED_BLOCKS, Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) modifiedBlocks.add(NbtUtils.readBlockPos(list.getCompound(i)));
        }

        if (nbt.contains(KEY_WD_MINED_BLOCKS, Tag.TAG_LIST)) {
            ListTag list = nbt.getList(KEY_WD_MINED_BLOCKS, Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag tag = list.getCompound(i);
                minedBlocks.put(
                    NbtUtils.readBlockPos(tag.getCompound(KEY_MAP_KEY_POS)),
                    NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound(KEY_MAP_VALUE_STATE))
                );
            }
        }

        if (nbt.contains(KEY_WD_BLOCK_DIMENSION_INDICES, Tag.TAG_LIST)) {
            ListTag list = nbt.getList(KEY_WD_BLOCK_DIMENSION_INDICES, Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag tag = list.getCompound(i);
                blockDimensionIndices.put(NbtUtils.readBlockPos(tag.getCompound(KEY_MAP_KEY_POS)), tag.getInt(KEY_MAP_VALUE_INT));
            }
        }

        if (nbt.contains(KEY_WD_BLOCK_ENTITY_DATA, Tag.TAG_LIST)) {
            ListTag list = nbt.getList(KEY_WD_BLOCK_ENTITY_DATA, Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag tag = list.getCompound(i);
                blockEntityData.put(NbtUtils.readBlockPos(tag.getCompound(KEY_MAP_KEY_POS)), tag.getCompound(KEY_MAP_VALUE_NBT));
            }
        }
    }

    public static class LightningStrike {
        public final BlockPos pos;
        public final long tickTime;
        public LightningStrike(BlockPos pos, long tickTime) { this.pos = pos; this.tickTime = tickTime; }
        public CompoundTag toNBT() {
            CompoundTag tag = new CompoundTag();
            tag.put(KEY_POS, NbtUtils.writeBlockPos(pos));
            tag.putLong("tickTime", tickTime);
            return tag;
        }
        public static LightningStrike fromNBT(CompoundTag tag) {
            return new LightningStrike(NbtUtils.readBlockPos(tag.getCompound(KEY_POS)), tag.getLong("tickTime"));
        }
    }
    
    public static record SavedBlock(BlockPos pos, BlockState state, ResourceKey<Level> dimension) {}
}
