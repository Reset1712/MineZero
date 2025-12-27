package boomcow.minezero.checkpoint;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;
import net.minecraft.registry.RegistryKey;
import net.minecraft.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class CheckpointData extends PersistentState {
    public static final String DATA_NAME = "global_checkpoint";
    private static final Logger LOGGER = LoggerFactory.getLogger("MineZeroCheckpointData");

    private static final String KEY_CHECKPOINT_DIMENSION = "checkpointDimension";
    private static final String KEY_PLAYERS_DATA = "playersData";
    private static final String KEY_FIRE_TICKS = "fireTicks";
    private static final String KEY_ANCHOR_PLAYER_UUID = "anchorPlayerUUID";
    private static final String KEY_CHECKPOINT_POS_X = "checkpointPosX";
    private static final String KEY_CHECKPOINT_POS_Y = "checkpointPosY";
    private static final String KEY_CHECKPOINT_POS_Z = "checkpointPosZ";
    private static final String KEY_CHECKPOINT_HEALTH = "checkpointHealth";
    private static final String KEY_CHECKPOINT_HUNGER = "checkpointHunger";
    private static final String KEY_CHECKPOINT_XP = "checkpointXP";
    private static final String KEY_CHECKPOINT_DAY_TIME = "checkpointDayTime";
    private static final String KEY_ENTITY_DATA = "entityData";
    private static final String KEY_ENTITY_DIMENSIONS = "entityDimensions";
    private static final String KEY_ENTITY_AGGRO_TARGETS = "entityAggroTargets";
    private static final String KEY_GROUND_ITEMS = "groundItems";
    private static final String KEY_DYNAMIC_ENTITIES = "dynamicEntities";
    private static final String KEY_WORLD_DATA = "worldCheckpointData";

    private RegistryKey<World> checkpointDimension;
    private Map<UUID, NbtCompound> playersData = new HashMap<>();
    private int fireTicks;
    private UUID anchorPlayerUUID;
    private BlockPos checkpointPos;
    private List<ItemStack> checkpointInventory = new ArrayList<>();
    private float checkpointHealth;
    private int checkpointHunger;
    private int checkpointXP;
    private long checkpointDayTime;
    
    private List<NbtCompound> entityData = new ArrayList<>();
    private List<RegistryKey<World>> entityDimensions = new ArrayList<>();
    
    private Map<UUID, NbtCompound> dynamicEntityData = new HashMap<>();
    private Map<UUID, String> dynamicEntityDimensions = new HashMap<>();
    
    private Map<UUID, UUID> entityAggroTargets = new HashMap<>();
    private List<NbtCompound> groundItems = new ArrayList<>();
    private WorldData worldData = new WorldData();
    private final Set<UUID> savedEntityUUIDs = new HashSet<>();

    public CheckpointData() {}

    public void trackDynamicEntity(UUID uuid, NbtCompound nbt, RegistryKey<World> dimension) {
        if (!savedEntityUUIDs.contains(uuid)) {
            savedEntityUUIDs.add(uuid);
            dynamicEntityData.put(uuid, nbt);
            dynamicEntityDimensions.put(uuid, dimension.getValue().toString());
            markDirty();
        }
    }

    public Map<UUID, NbtCompound> getDynamicEntityData() {
        return dynamicEntityData;
    }

    public Map<UUID, String> getDynamicEntityDimensions() {
        return dynamicEntityDimensions;
    }

    public void clearAllEntityData() {
        this.entityData.clear();
        this.entityDimensions.clear();
        this.dynamicEntityData.clear();
        this.dynamicEntityDimensions.clear();
        this.savedEntityUUIDs.clear();
        this.markDirty();
    }

    public boolean isEntitySaved(UUID uuid) {
        return savedEntityUUIDs.contains(uuid);
    }

    public void setCheckpointDimension(RegistryKey<World> dimension) {
        this.checkpointDimension = dimension;
        this.markDirty();
    }

    public RegistryKey<World> getCheckpointDimension() {
        return checkpointDimension;
    }

    public void savePlayerData(UUID uuid, NbtCompound playerDataNBT) {
        this.playersData.put(uuid, playerDataNBT);
        markDirty();
    }

    public PlayerData getPlayerData(UUID uuid, RegistryWrapper.WrapperLookup lookupProvider) {
        NbtCompound nbt = playersData.get(uuid);
        if (nbt != null) {
            return PlayerData.fromNBT(nbt, lookupProvider);
        }
        return null;
    }

    public void setFireTicks(int fireTicks) {
        this.fireTicks = fireTicks;
        this.markDirty();
    }

    public int getFireTicks() {
        return fireTicks;
    }

    public void setAnchorPlayerUUID(UUID anchorPlayerUUID) {
        this.anchorPlayerUUID = anchorPlayerUUID;
        this.markDirty();
    }

    public UUID getAnchorPlayerUUID() {
        return anchorPlayerUUID;
    }

    public WorldData getWorldData() {
        return this.worldData;
    }

    public void saveWorldData(ServerWorld world) {
        this.worldData.clearWorldData();
        this.worldData.saveAllLoadedChunks(world);
        this.worldData.saveWeather(world);
        this.worldData.saveDayTime(world.getTimeOfDay());
        this.worldData.saveGameTime(world.getTime());
        this.worldData.saveCheckpointTick(world.getTime());
        this.markDirty();
    }

    public static CheckpointData load(NbtCompound nbt, RegistryWrapper.WrapperLookup lookupProvider) {
        CheckpointData data = new CheckpointData();

        if (nbt.contains(KEY_CHECKPOINT_DIMENSION, NbtElement.STRING_TYPE)) {
            try {
                data.checkpointDimension = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(nbt.getString(KEY_CHECKPOINT_DIMENSION)));
            } catch (Exception e) {
                LOGGER.warn("Failed to parse checkpointDimension Identifier: {}", nbt.getString(KEY_CHECKPOINT_DIMENSION), e);
            }
        }

        if (nbt.contains(KEY_PLAYERS_DATA, NbtElement.COMPOUND_TYPE)) {
            NbtCompound playersTag = nbt.getCompound(KEY_PLAYERS_DATA);
            for (String key : playersTag.getKeys()) {
                try {
                    UUID uuid = UUID.fromString(key);
                    data.playersData.put(uuid, playersTag.getCompound(key));
                } catch (IllegalArgumentException e) {
                    LOGGER.warn("Failed to parse UUID from key '{}' in PlayersData NBT", key, e);
                }
            }
        }

        data.fireTicks = nbt.getInt(KEY_FIRE_TICKS);

        if (nbt.containsUuid(KEY_ANCHOR_PLAYER_UUID)) {
            data.anchorPlayerUUID = nbt.getUuid(KEY_ANCHOR_PLAYER_UUID);
        }

        if (nbt.contains(KEY_CHECKPOINT_POS_X) && nbt.contains(KEY_CHECKPOINT_POS_Y) && nbt.contains(KEY_CHECKPOINT_POS_Z)) {
            data.checkpointPos = new BlockPos(nbt.getInt(KEY_CHECKPOINT_POS_X), nbt.getInt(KEY_CHECKPOINT_POS_Y), nbt.getInt(KEY_CHECKPOINT_POS_Z));
        }
        data.checkpointHealth = nbt.getFloat(KEY_CHECKPOINT_HEALTH);
        data.checkpointHunger = nbt.getInt(KEY_CHECKPOINT_HUNGER);
        data.checkpointXP = nbt.getInt(KEY_CHECKPOINT_XP);
        data.checkpointDayTime = nbt.getLong(KEY_CHECKPOINT_DAY_TIME);

        data.savedEntityUUIDs.clear();
        if (nbt.contains(KEY_ENTITY_DATA, NbtElement.LIST_TYPE)) {
            NbtList entityListTag = nbt.getList(KEY_ENTITY_DATA, NbtElement.COMPOUND_TYPE);
            data.entityData.clear();
            for (int i = 0; i < entityListTag.size(); i++) {
                NbtCompound tag = entityListTag.getCompound(i);
                data.entityData.add(tag);
                if (tag.containsUuid("UUID")) {
                    data.savedEntityUUIDs.add(tag.getUuid("UUID"));
                }
            }
        }

        if (nbt.contains(KEY_ENTITY_DIMENSIONS, NbtElement.LIST_TYPE)) {
            NbtList entityDimListTag = nbt.getList(KEY_ENTITY_DIMENSIONS, NbtElement.STRING_TYPE);
            data.entityDimensions.clear();
            for (int i = 0; i < entityDimListTag.size(); i++) {
                try {
                    data.entityDimensions.add(RegistryKey.of(RegistryKeys.WORLD, Identifier.of(entityDimListTag.getString(i))));
                } catch (Exception e) {
                    LOGGER.warn("Failed to load entity dimension from NBT: {}", entityDimListTag.getString(i), e);
                }
            }
        }

        if (nbt.contains(KEY_DYNAMIC_ENTITIES, NbtElement.LIST_TYPE)) {
            NbtList dynList = nbt.getList(KEY_DYNAMIC_ENTITIES, NbtElement.COMPOUND_TYPE);
            for(int i = 0; i < dynList.size(); i++) {
                NbtCompound entry = dynList.getCompound(i);
                UUID uuid = entry.getUuid("uuid");
                NbtCompound entityTag = entry.getCompound("tag");
                String dim = entry.getString("dim");
                
                data.dynamicEntityData.put(uuid, entityTag);
                data.dynamicEntityDimensions.put(uuid, dim);
                data.savedEntityUUIDs.add(uuid);
            }
        }

        if (nbt.contains(KEY_ENTITY_AGGRO_TARGETS, NbtElement.COMPOUND_TYPE)) {
            NbtCompound aggroTag = nbt.getCompound(KEY_ENTITY_AGGRO_TARGETS);
            data.entityAggroTargets.clear();
            for (String key : aggroTag.getKeys()) {
                try {
                    UUID entityUUID = UUID.fromString(key);
                    if (aggroTag.containsUuid(key)) {
                        UUID targetUUID = aggroTag.getUuid(key);
                        data.entityAggroTargets.put(entityUUID, targetUUID);
                    }
                } catch (IllegalArgumentException e) {
                    LOGGER.warn("Failed to parse UUID for aggro target key '{}' from NBT", key, e);
                }
            }
        }

        if (nbt.contains(KEY_GROUND_ITEMS, NbtElement.LIST_TYPE)) {
            NbtList groundItemsListTag = nbt.getList(KEY_GROUND_ITEMS, NbtElement.COMPOUND_TYPE);
            data.groundItems.clear();
            for (int i = 0; i < groundItemsListTag.size(); i++) {
                data.groundItems.add(groundItemsListTag.getCompound(i));
            }
        }
        if (nbt.contains(KEY_WORLD_DATA, NbtElement.COMPOUND_TYPE)) {
            data.worldData.loadFromNBT(nbt.getCompound(KEY_WORLD_DATA), lookupProvider);
        }
        return data;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup wrapperLookup) {
        if (checkpointDimension != null) {
            nbt.putString(KEY_CHECKPOINT_DIMENSION, checkpointDimension.getValue().toString());
        }

        NbtCompound playersNbt = new NbtCompound();
        for (Map.Entry<UUID, NbtCompound> entry : playersData.entrySet()) {
            playersNbt.put(entry.getKey().toString(), entry.getValue());
        }
        nbt.put(KEY_PLAYERS_DATA, playersNbt);

        nbt.putInt(KEY_FIRE_TICKS, fireTicks);

        if (anchorPlayerUUID != null) {
            nbt.putUuid(KEY_ANCHOR_PLAYER_UUID, anchorPlayerUUID);
        }

        if (checkpointPos != null) {
            nbt.putInt(KEY_CHECKPOINT_POS_X, checkpointPos.getX());
            nbt.putInt(KEY_CHECKPOINT_POS_Y, checkpointPos.getY());
            nbt.putInt(KEY_CHECKPOINT_POS_Z, checkpointPos.getZ());
        }
        nbt.putFloat(KEY_CHECKPOINT_HEALTH, checkpointHealth);
        nbt.putInt(KEY_CHECKPOINT_HUNGER, checkpointHunger);
        nbt.putInt(KEY_CHECKPOINT_XP, checkpointXP);
        nbt.putLong(KEY_CHECKPOINT_DAY_TIME, checkpointDayTime);

        NbtList entityListNbt = new NbtList();
        for (NbtCompound entityNBT : entityData) {
            entityListNbt.add(entityNBT);
        }
        nbt.put(KEY_ENTITY_DATA, entityListNbt);

        NbtList entityDimListNbt = new NbtList();
        for (RegistryKey<World> dimKey : entityDimensions) {
            if (dimKey != null) {
                entityDimListNbt.add(NbtString.of(dimKey.getValue().toString()));
            }
        }
        nbt.put(KEY_ENTITY_DIMENSIONS, entityDimListNbt);

        NbtList dynList = new NbtList();
        for (Map.Entry<UUID, NbtCompound> entry : dynamicEntityData.entrySet()) {
            NbtCompound wrapper = new NbtCompound();
            wrapper.putUuid("uuid", entry.getKey());
            wrapper.put("tag", entry.getValue());
            wrapper.putString("dim", dynamicEntityDimensions.get(entry.getKey()));
            dynList.add(wrapper);
        }
        nbt.put(KEY_DYNAMIC_ENTITIES, dynList);

        NbtCompound aggroTargetsNbt = new NbtCompound();
        for (Map.Entry<UUID, UUID> entry : entityAggroTargets.entrySet()) {
            aggroTargetsNbt.putUuid(entry.getKey().toString(), entry.getValue());
        }
        nbt.put(KEY_ENTITY_AGGRO_TARGETS, aggroTargetsNbt);

        NbtList groundItemsListNbt = new NbtList();
        for (NbtCompound itemNBT : groundItems) {
            groundItemsListNbt.add(itemNBT);
        }
        nbt.put(KEY_GROUND_ITEMS, groundItemsListNbt);
        
        NbtCompound worldDataNbt = new NbtCompound();
        if (this.worldData != null) {
            this.worldData.saveToNBT(worldDataNbt);
        }
        nbt.put(KEY_WORLD_DATA, worldDataNbt);
        return nbt;
    }

    public static CheckpointData get(ServerWorld world) {
        PersistentState.Type<CheckpointData> type = new PersistentState.Type<>(
                CheckpointData::new,
                CheckpointData::load,
                null
        );
        return world.getServer().getOverworld().getPersistentStateManager()
                .getOrCreate(type, CheckpointData.DATA_NAME);
    }

    public void setCheckpointPos(BlockPos pos) {
        this.checkpointPos = pos;
        this.markDirty();
    }

    public void setCheckpointHealth(float health) {
        this.checkpointHealth = health;
        this.markDirty();
    }

    public void setEntityAggroTargets(Map<UUID, UUID> aggroTargets) {
        this.entityAggroTargets = aggroTargets;
        this.markDirty();
    }

    public Map<UUID, UUID> getEntityAggroTargets() {
        return entityAggroTargets;
    }

    public void setCheckpointInventory(List<ItemStack> inv) {
        this.checkpointInventory = inv;
        this.markDirty();
    }

    public void setCheckpointHunger(int hunger) {
        this.checkpointHunger = hunger;
        this.markDirty();
    }

    public void setCheckpointXP(int xp) {
        this.checkpointXP = xp;
        this.markDirty();
    }

    public void setCheckpointDayTime(long dayTime) {
        this.checkpointDayTime = dayTime;
        this.markDirty();
    }

    public void setEntityData(List<NbtCompound> entities) {
        this.entityData = entities;
        this.markDirty();
    }

    public void setGroundItems(List<NbtCompound> groundItems) {
        this.groundItems = groundItems;
        this.markDirty();
    }

    public BlockPos getCheckpointPos() {
        return checkpointPos;
    }

    public float getCheckpointHealth() {
        return checkpointHealth;
    }

    public int getCheckpointHunger() {
        return checkpointHunger;
    }

    public int getCheckpointXP() {
        return checkpointXP;
    }

    public long getCheckpointDayTime() {
        return checkpointDayTime;
    }

    public List<ItemStack> getCheckpointInventory() {
        return checkpointInventory;
    }

    public List<NbtCompound> getEntityData() {
        return entityData;
    }
    public List<RegistryKey<World>> getEntityDimensions() {
        return entityDimensions;
    }
    public void setEntityDataWithDimensions(List<NbtCompound> entities, List<RegistryKey<World>> dimensions) {
        this.entityData = entities;
        this.entityDimensions = dimensions;
        this.savedEntityUUIDs.clear();
        for (NbtCompound tag : entities) {
            if (tag.containsUuid("UUID")) {
                this.savedEntityUUIDs.add(tag.getUuid("UUID"));
            }
        }
        this.markDirty();
    }

    public List<NbtCompound> getGroundItems() {
        return groundItems;
    }
}
