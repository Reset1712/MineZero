package boomcow.minezero.checkpoint;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.Level;

import java.util.*;

public class CheckpointData extends SavedData {
    public static final String DATA_NAME = "global_checkpoint";
    private ResourceKey<Level> checkpointDimension; // Track the dimension
    public void setCheckpointDimension(ResourceKey<Level> dimension) {
        this.checkpointDimension = dimension;
        this.setDirty();
    }

    public ResourceKey<Level> getCheckpointDimension() {
        return checkpointDimension;
    }
    private Map<UUID, CompoundTag> playersData = new HashMap<>();

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


    private int fireTicks;

    public void setFireTicks(int fireTicks) {
        this.fireTicks = fireTicks;
        this.setDirty();
    }

    public int getFireTicks() {
        return fireTicks;
    }

    private UUID anchorPlayerUUID;

    public void setAnchorPlayerUUID(UUID anchorPlayerUUID) {
        this.anchorPlayerUUID = anchorPlayerUUID;
        this.setDirty();
    }

    public UUID getAnchorPlayerUUID() {
        return anchorPlayerUUID;
    }


    private BlockPos checkpointPos;
    private List<ItemStack> checkpointInventory = new ArrayList<>();
    private float checkpointHealth;
    private int checkpointHunger;
    private int checkpointXP;

    private long checkpointDayTime;
    private List<CompoundTag> entityData = new ArrayList<>();
    private List<ResourceKey<Level>> entityDimensions = new ArrayList<>();
    private Map<UUID, UUID> entityAggroTargets = new HashMap<>();



    private List<CompoundTag> groundItems = new ArrayList<>();

    private WorldData worldData = new WorldData();

    public CheckpointData() {
    }

    public WorldData getWorldData() {
        return this.worldData;
    }

    public void saveWorldData(ServerLevel level) {
        this.worldData.clearWorldData();
        this.worldData.saveAllLoadedChunks(level);
        this.worldData.saveWeather(level);
        this.worldData.saveDayTime(level.getDayTime());
        this.setDirty();
    }

    public static CheckpointData load(CompoundTag nbt) {
        CheckpointData data = new CheckpointData();
        if (nbt.contains("PosX") && nbt.contains("PosY") && nbt.contains("PosZ")) {
            data.checkpointPos = new BlockPos(nbt.getInt("PosX"), nbt.getInt("PosY"), nbt.getInt("PosZ"));
        }
        data.checkpointHealth = nbt.getFloat("Health");
        data.checkpointHunger = nbt.getInt("Hunger");
        data.checkpointXP = nbt.getInt("XP");

        if (nbt.contains("DayTime", Tag.TAG_LONG)) {
            data.checkpointDayTime = nbt.getLong("DayTime");
        }

        // Load inventory
        ListTag invList = nbt.getList("Inventory", Tag.TAG_COMPOUND);
        for (int i = 0; i < invList.size(); i++) {
            CompoundTag stackTag = invList.getCompound(i);
            ItemStack stack = ItemStack.of(stackTag);
            data.checkpointInventory.add(stack);
        }

        // Load entities
        if (nbt.contains("Entities", Tag.TAG_LIST)) {
            ListTag entityList = nbt.getList("Entities", Tag.TAG_COMPOUND);
            for (int i = 0; i < entityList.size(); i++) {
                data.entityData.add(entityList.getCompound(i));
            }
        }

        if (nbt.contains("EntityAggroTargets")) {
            CompoundTag aggroTag = nbt.getCompound("EntityAggroTargets");
            for (String key : aggroTag.getAllKeys()) {
                UUID entityUUID = UUID.fromString(key);
                UUID targetUUID = aggroTag.getUUID(key);
                data.entityAggroTargets.put(entityUUID, targetUUID);
            }
        }


        // Load ground items
        if (nbt.contains("GroundItems", Tag.TAG_LIST)) {
            ListTag groundItemsList = nbt.getList("GroundItems", Tag.TAG_COMPOUND);
            for (int i = 0; i < groundItemsList.size(); i++) {
                data.groundItems.add(groundItemsList.getCompound(i));
            }
        }
        data.fireTicks = nbt.getInt("FireTicks");

        if (nbt.hasUUID("AnchorPlayerUUID")) {
            data.setAnchorPlayerUUID(nbt.getUUID("AnchorPlayerUUID"));
        }

        if (nbt.contains("PlayersData")) {
            CompoundTag playersTag = nbt.getCompound("PlayersData");
            for (String key : playersTag.getAllKeys()) {
                UUID uuid = UUID.fromString(key);
                data.playersData.put(uuid, playersTag.getCompound(key));
            }
        }


        return data;
    }

    // Saves nbt data to a compound tag
    @Override
    public CompoundTag save(CompoundTag nbt) {
        return nbt;
    }

    public static CheckpointData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage()
                .computeIfAbsent(CheckpointData::load, CheckpointData::new, CheckpointData.DATA_NAME);
    }

    public String toString(ServerLevel level) {
        CheckpointData data = get(level); // Call the get method to retrieve the data
        StringBuilder sb = new StringBuilder();
        sb.append("CheckpointData:\n");
        sb.append("AnchorPlayerUUID: ").append(data.getAnchorPlayerUUID()).append("\n");
        sb.append("CheckpointPos: ").append(data.getCheckpointPos()).append("\n");
        sb.append("CheckpointHealth: ").append(data.getCheckpointHealth()).append("\n");
        sb.append("CheckpointHunger: ").append(data.getCheckpointHunger()).append("\n");
        sb.append("CheckpointXP: ").append(data.getCheckpointXP()).append("\n");
        sb.append("CheckpointDayTime: ").append(data.getCheckpointDayTime()).append("\n");
        sb.append("FireTicks: ").append(data.getFireTicks()).append("\n");
        sb.append("EntityData: ").append(data.getEntityData()).append("\n");
        sb.append("GroundItems: ").append(data.getGroundItems()).append("\n");
        sb.append("CheckpointInventory: ").append(data.getCheckpointInventory()).append("\n");
        sb.append("PlayersData: ").append(data.playersData).append("\n");
        return sb.toString();
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
        this.setDirty();
    }

    public List<CompoundTag> getGroundItems() {
        return groundItems;
    }
}
