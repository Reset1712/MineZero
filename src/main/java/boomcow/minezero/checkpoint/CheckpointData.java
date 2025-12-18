package boomcow.minezero.checkpoint;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.slf4j.Logger;

import java.util.*;

public class CheckpointData extends SavedData {
    public static final String DATA_NAME = "global_checkpoint";
    private static final Logger LOGGER = LogUtils.getLogger();

    // NBT Keys
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
    private static final String KEY_CHECKPOINT_INVENTORY = "checkpointInventory";
    private static final String KEY_WORLD_DATA = "worldCheckpointData";
    
    // NEW KEY
    private static final String KEY_DYNAMIC_ENTITIES = "dynamicEntities";

    private ResourceKey<Level> checkpointDimension;
    private Map<UUID, CompoundTag> playersData = new HashMap<>();
    private int fireTicks;
    private UUID anchorPlayerUUID;
    private BlockPos checkpointPos;
    private List<ItemStack> checkpointInventory = new ArrayList<>();
    private float checkpointHealth;
    private int checkpointHunger;
    private int checkpointXP;
    private long checkpointDayTime;
    
    // Initial entities (around the player when checkpoint was set)
    private List<CompoundTag> entityData = new ArrayList<>();
    private List<ResourceKey<Level>> entityDimensions = new ArrayList<>();
    
    // Dynamic entities (encountered during gameplay)
    // Map<UUID, Pair<CompoundTag, DimensionString>>
    private Map<UUID, CompoundTag> dynamicEntityData = new HashMap<>();
    private Map<UUID, String> dynamicEntityDimensions = new HashMap<>();

    private Map<UUID, UUID> entityAggroTargets = new HashMap<>();
    private List<CompoundTag> groundItems = new ArrayList<>();
    private WorldData worldData = new WorldData();

    private final Set<UUID> savedEntityUUIDs = new HashSet<>();

    public CheckpointData() {
    }

    // --- Dynamic Entity Management ---
    public void trackDynamicEntity(UUID uuid, CompoundTag nbt, ResourceKey<Level> dimension) {
        if (!savedEntityUUIDs.contains(uuid)) {
            savedEntityUUIDs.add(uuid);
            dynamicEntityData.put(uuid, nbt);
            dynamicEntityDimensions.put(uuid, dimension.location().toString());
            setDirty();
        }
    }

    public Map<UUID, CompoundTag> getDynamicEntityData() {
        return dynamicEntityData;
    }

    public Map<UUID, String> getDynamicEntityDimensions() {
        return dynamicEntityDimensions;
    }
    // ---------------------------------

    public void setCheckpointDimension(ResourceKey<Level> dimension) {
        this.checkpointDimension = dimension;
        this.setDirty();
    }

    public ResourceKey<Level> getCheckpointDimension() {
        return checkpointDimension;
    }

    public void savePlayerData(UUID uuid, PlayerData pdata) {
        playersData.put(uuid, pdata.toNBT());
        setDirty();
    }

    public PlayerData getPlayerData(UUID uuid) {
        if (playersData.containsKey(uuid)) {
            return PlayerData.fromNBT(playersData.get(uuid));
        }
        return null;
    }

    public void setFireTicks(int fireTicks) {
        this.fireTicks = fireTicks;
        this.setDirty();
    }

    public int getFireTicks() {
        return fireTicks;
    }

    public void setAnchorPlayerUUID(UUID anchorPlayerUUID) {
        this.anchorPlayerUUID = anchorPlayerUUID;
        this.setDirty();
    }

    public UUID getAnchorPlayerUUID() {
        return anchorPlayerUUID;
    }

    public WorldData getWorldData() {
        return this.worldData;
    }

    public void saveWorldData(ServerLevel level) {
        this.worldData.clearWorldData();
        this.worldData.saveAllLoadedChunks(level);
        this.worldData.saveWeather(level);
        this.worldData.saveDayTime(level.getDayTime());
        this.worldData.saveGameTime(level.getGameTime());
        this.worldData.saveCheckpointTick(level.getGameTime());
        this.setDirty();
    }

    // --- Clears data when a NEW checkpoint is set ---
    public void clearAllEntityData() {
        this.entityData.clear();
        this.entityDimensions.clear();
        this.dynamicEntityData.clear();
        this.dynamicEntityDimensions.clear();
        this.savedEntityUUIDs.clear();
        this.setDirty();
    }

    public static CheckpointData load(CompoundTag nbt) {
        CheckpointData data = new CheckpointData();

        if (nbt.contains(KEY_CHECKPOINT_DIMENSION, Tag.TAG_STRING)) {
            data.checkpointDimension = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(nbt.getString(KEY_CHECKPOINT_DIMENSION)));
        }

        if (nbt.contains(KEY_PLAYERS_DATA, Tag.TAG_COMPOUND)) {
            CompoundTag playersTag = nbt.getCompound(KEY_PLAYERS_DATA);
            for (String key : playersTag.getAllKeys()) {
                try {
                    UUID uuid = UUID.fromString(key);
                    data.playersData.put(uuid, playersTag.getCompound(key));
                } catch (IllegalArgumentException e) {
                    LOGGER.warn("Failed to parse UUID from key '{}' in PlayersData NBT", key, e);
                }
            }
        }

        data.fireTicks = nbt.getInt(KEY_FIRE_TICKS);

        if (nbt.hasUUID(KEY_ANCHOR_PLAYER_UUID)) {
            data.anchorPlayerUUID = nbt.getUUID(KEY_ANCHOR_PLAYER_UUID);
        }

        if (nbt.contains(KEY_CHECKPOINT_POS_X)) {
            data.checkpointPos = new BlockPos(nbt.getInt(KEY_CHECKPOINT_POS_X), nbt.getInt(KEY_CHECKPOINT_POS_Y), nbt.getInt(KEY_CHECKPOINT_POS_Z));
        }
        data.checkpointHealth = nbt.getFloat(KEY_CHECKPOINT_HEALTH);
        data.checkpointHunger = nbt.getInt(KEY_CHECKPOINT_HUNGER);
        data.checkpointXP = nbt.getInt(KEY_CHECKPOINT_XP);
        data.checkpointDayTime = nbt.getLong(KEY_CHECKPOINT_DAY_TIME);

        if (nbt.contains(KEY_CHECKPOINT_INVENTORY, Tag.TAG_LIST)) {
            ListTag invListTag = nbt.getList(KEY_CHECKPOINT_INVENTORY, Tag.TAG_COMPOUND);
            data.checkpointInventory.clear();
            for (int i = 0; i < invListTag.size(); i++) {
                data.checkpointInventory.add(ItemStack.of(invListTag.getCompound(i)));
            }
        }

        data.savedEntityUUIDs.clear();
        
        // Load initial entities
        if (nbt.contains(KEY_ENTITY_DATA, Tag.TAG_LIST)) {
            ListTag entityListTag = nbt.getList(KEY_ENTITY_DATA, Tag.TAG_COMPOUND);
            data.entityData.clear();
            for (int i = 0; i < entityListTag.size(); i++) {
                CompoundTag tag = entityListTag.getCompound(i);
                data.entityData.add(tag);
                if (tag.hasUUID("UUID")) {
                    data.savedEntityUUIDs.add(tag.getUUID("UUID"));
                }
            }
        }

        if (nbt.contains(KEY_ENTITY_DIMENSIONS, Tag.TAG_LIST)) {
            ListTag entityDimListTag = nbt.getList(KEY_ENTITY_DIMENSIONS, Tag.TAG_STRING);
            data.entityDimensions.clear();
            for (int i = 0; i < entityDimListTag.size(); i++) {
                try {
                    data.entityDimensions.add(ResourceKey.create(Registries.DIMENSION, new ResourceLocation(entityDimListTag.getString(i))));
                } catch (Exception e) {
                    LOGGER.warn("Failed to load entity dimension", e);
                }
            }
        }

        // Load dynamic entities
        if (nbt.contains(KEY_DYNAMIC_ENTITIES, Tag.TAG_LIST)) {
            ListTag dynList = nbt.getList(KEY_DYNAMIC_ENTITIES, Tag.TAG_COMPOUND);
            for(int i = 0; i < dynList.size(); i++) {
                CompoundTag entry = dynList.getCompound(i);
                UUID uuid = entry.getUUID("uuid");
                CompoundTag entityTag = entry.getCompound("tag");
                String dim = entry.getString("dim");
                
                data.dynamicEntityData.put(uuid, entityTag);
                data.dynamicEntityDimensions.put(uuid, dim);
                data.savedEntityUUIDs.add(uuid);
            }
        }

        if (nbt.contains(KEY_ENTITY_AGGRO_TARGETS, Tag.TAG_COMPOUND)) {
            CompoundTag aggroTag = nbt.getCompound(KEY_ENTITY_AGGRO_TARGETS);
            data.entityAggroTargets.clear();
            for (String key : aggroTag.getAllKeys()) {
                try {
                    UUID entityUUID = UUID.fromString(key);
                    if (aggroTag.hasUUID(key)) {
                        UUID targetUUID = aggroTag.getUUID(key);
                        data.entityAggroTargets.put(entityUUID, targetUUID);
                    }
                } catch (IllegalArgumentException e) {
                    LOGGER.warn("Failed to parse UUID for aggro target", e);
                }
            }
        }

        if (nbt.contains(KEY_GROUND_ITEMS, Tag.TAG_LIST)) {
            ListTag groundItemsListTag = nbt.getList(KEY_GROUND_ITEMS, Tag.TAG_COMPOUND);
            data.groundItems.clear();
            for (int i = 0; i < groundItemsListTag.size(); i++) {
                data.groundItems.add(groundItemsListTag.getCompound(i));
            }
        }

        if (nbt.contains(KEY_WORLD_DATA, Tag.TAG_COMPOUND)) {
            data.worldData.loadFromNBT(nbt.getCompound(KEY_WORLD_DATA));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        if (checkpointDimension != null) {
            nbt.putString(KEY_CHECKPOINT_DIMENSION, checkpointDimension.location().toString());
        }

        CompoundTag playersNbt = new CompoundTag();
        for (Map.Entry<UUID, CompoundTag> entry : playersData.entrySet()) {
            playersNbt.put(entry.getKey().toString(), entry.getValue());
        }
        nbt.put(KEY_PLAYERS_DATA, playersNbt);

        nbt.putInt(KEY_FIRE_TICKS, fireTicks);

        if (anchorPlayerUUID != null) {
            nbt.putUUID(KEY_ANCHOR_PLAYER_UUID, anchorPlayerUUID);
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

        ListTag invListTag = new ListTag();
        for (ItemStack stack : checkpointInventory) {
            if (!stack.isEmpty()) {
                invListTag.add(stack.save(new CompoundTag()));
            }
        }
        nbt.put(KEY_CHECKPOINT_INVENTORY, invListTag);

        ListTag entityListNbt = new ListTag();
        for (CompoundTag entityNBT : entityData) {
            entityListNbt.add(entityNBT);
        }
        nbt.put(KEY_ENTITY_DATA, entityListNbt);

        ListTag entityDimListNbt = new ListTag();
        for (ResourceKey<Level> dimKey : entityDimensions) {
            if (dimKey != null) {
                entityDimListNbt.add(StringTag.valueOf(dimKey.location().toString()));
            }
        }
        nbt.put(KEY_ENTITY_DIMENSIONS, entityDimListNbt);

        // Save Dynamic Entities
        ListTag dynList = new ListTag();
        for (Map.Entry<UUID, CompoundTag> entry : dynamicEntityData.entrySet()) {
            CompoundTag wrapper = new CompoundTag();
            wrapper.putUUID("uuid", entry.getKey());
            wrapper.put("tag", entry.getValue());
            wrapper.putString("dim", dynamicEntityDimensions.get(entry.getKey()));
            dynList.add(wrapper);
        }
        nbt.put(KEY_DYNAMIC_ENTITIES, dynList);

        CompoundTag aggroTargetsNbt = new CompoundTag();
        for (Map.Entry<UUID, UUID> entry : entityAggroTargets.entrySet()) {
            aggroTargetsNbt.putUUID(entry.getKey().toString(), entry.getValue());
        }
        nbt.put(KEY_ENTITY_AGGRO_TARGETS, aggroTargetsNbt);

        ListTag groundItemsListNbt = new ListTag();
        for (CompoundTag itemNBT : groundItems) {
            groundItemsListNbt.add(itemNBT);
        }
        nbt.put(KEY_GROUND_ITEMS, groundItemsListNbt);

        CompoundTag worldDataNbt = new CompoundTag();
        if (this.worldData != null) {
            this.worldData.saveToNBT(worldDataNbt);
        }
        nbt.put(KEY_WORLD_DATA, worldDataNbt);
        return nbt;
    }

    public static CheckpointData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage()
                .computeIfAbsent(CheckpointData::load, CheckpointData::new, CheckpointData.DATA_NAME);
    }

    public void setCheckpointPos(BlockPos pos) {
        this.checkpointPos = pos;
        this.setDirty();
    }

    public void setCheckpointHealth(float health) {
        this.checkpointHealth = health;
        this.setDirty();
    }

    public void setEntityAggroTargets(Map<UUID, UUID> aggroTargets) {
        this.entityAggroTargets = aggroTargets;
        this.setDirty();
    }

    public Map<UUID, UUID> getEntityAggroTargets() {
        return entityAggroTargets;
    }

    public void setCheckpointInventory(List<ItemStack> inv) {
        this.checkpointInventory = inv;
        this.setDirty();
    }

    public void setCheckpointHunger(int hunger) {
        this.checkpointHunger = hunger;
        this.setDirty();
    }

    public void setCheckpointXP(int xp) {
        this.checkpointXP = xp;
        this.setDirty();
    }

    public void setCheckpointDayTime(long dayTime) {
        this.checkpointDayTime = dayTime;
        this.setDirty();
    }

    public void setEntityData(List<CompoundTag> entities) {
        this.entityData = entities;
        this.setDirty();
    }

    public void setGroundItems(List<CompoundTag> groundItems) {
        this.groundItems = groundItems;
        this.setDirty();
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

    public List<CompoundTag> getEntityData() {
        return entityData;
    }

    public List<ResourceKey<Level>> getEntityDimensions() {
        return entityDimensions;
    }

    public void setEntityDataWithDimensions(List<CompoundTag> entities, List<ResourceKey<Level>> dimensions) {
        this.entityData = entities;
        this.entityDimensions = dimensions;
        this.savedEntityUUIDs.clear();
        for (CompoundTag tag : entities) {
            if (tag.hasUUID("UUID")) {
                this.savedEntityUUIDs.add(tag.getUUID("UUID"));
            }
        }
        this.setDirty();
    }

    public boolean isEntitySaved(UUID uuid) {
        return savedEntityUUIDs.contains(uuid);
    }

    public List<CompoundTag> getGroundItems() {
        return groundItems;
    }
}