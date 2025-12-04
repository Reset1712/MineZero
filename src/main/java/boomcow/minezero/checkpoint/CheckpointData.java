package boomcow.minezero.checkpoint;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.util.Constants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class CheckpointData extends WorldSavedData {
    public static final String DATA_NAME = "global_checkpoint";
    private static final Logger LOGGER = LogManager.getLogger();

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

    // In 1.12, dimensions are Integers. Default to 0 (Overworld)
    private int checkpointDimension = 0;

    private Map<UUID, NBTTagCompound> playersData = new HashMap<>();
    private int fireTicks;
    private UUID anchorPlayerUUID;
    private BlockPos checkpointPos;
    private List<ItemStack> checkpointInventory = new ArrayList<>();
    private float checkpointHealth;
    private int checkpointHunger;
    private int checkpointXP;
    private long checkpointDayTime;

    private List<NBTTagCompound> entityData = new ArrayList<>();
    private List<Integer> entityDimensions = new ArrayList<>();
    private Map<UUID, UUID> entityAggroTargets = new HashMap<>();
    private List<NBTTagCompound> groundItems = new ArrayList<>();

    private WorldData worldData = new WorldData();

    // Required Constructor for WorldSavedData
    public CheckpointData(String name) {
        super(name);
    }

    // Default constructor for creating new instances manually
    public CheckpointData() {
        super(DATA_NAME);
    }

    public void setCheckpointDimension(int dimension) {
        this.checkpointDimension = dimension;
        this.markDirty();
    }

    public int getCheckpointDimension() {
        return checkpointDimension;
    }

    public void savePlayerData(UUID uuid, PlayerData pdata) {
        playersData.put(uuid, pdata.toNBT());
        markDirty();
    }

    public PlayerData getPlayerData(UUID uuid) {
        if (playersData.containsKey(uuid)) {
            return PlayerData.fromNBT(playersData.get(uuid));
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

    public void saveWorldData(WorldServer level) {
        this.worldData.clearWorldData();
        this.worldData.saveAllLoadedChunks(level);
        this.worldData.saveWeather(level);
        this.worldData.saveDayTime(level.getWorldTime()); // getDayTime equivalent
        this.worldData.saveGameTime(level.getTotalWorldTime()); // getGameTime equivalent
        this.worldData.saveCheckpointTick(level.getTotalWorldTime());
        this.markDirty();
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        if (nbt.hasKey(KEY_CHECKPOINT_DIMENSION)) {
            // Read as integer for 1.12
            this.checkpointDimension = nbt.getInteger(KEY_CHECKPOINT_DIMENSION);
        }

        if (nbt.hasKey(KEY_PLAYERS_DATA, Constants.NBT.TAG_COMPOUND)) {
            NBTTagCompound playersTag = nbt.getCompoundTag(KEY_PLAYERS_DATA);
            for (String key : playersTag.getKeySet()) {
                try {
                    UUID uuid = UUID.fromString(key);
                    this.playersData.put(uuid, playersTag.getCompoundTag(key));
                } catch (IllegalArgumentException e) {
                    LOGGER.warn("Failed to parse UUID from key '{}' in PlayersData NBT", key, e);
                }
            }
        }

        this.fireTicks = nbt.getInteger(KEY_FIRE_TICKS);

        if (nbt.hasKey(KEY_ANCHOR_PLAYER_UUID)) {
            try {
                this.anchorPlayerUUID = UUID.fromString(nbt.getString(KEY_ANCHOR_PLAYER_UUID));
            } catch (Exception e) {
                LOGGER.warn("Failed to parse Anchor Player UUID");
            }
        }

        if (nbt.hasKey(KEY_CHECKPOINT_POS_X)) {
            this.checkpointPos = new BlockPos(
                    nbt.getInteger(KEY_CHECKPOINT_POS_X),
                    nbt.getInteger(KEY_CHECKPOINT_POS_Y),
                    nbt.getInteger(KEY_CHECKPOINT_POS_Z)
            );
        }

        this.checkpointHealth = nbt.getFloat(KEY_CHECKPOINT_HEALTH);
        this.checkpointHunger = nbt.getInteger(KEY_CHECKPOINT_HUNGER);
        this.checkpointXP = nbt.getInteger(KEY_CHECKPOINT_XP);
        this.checkpointDayTime = nbt.getLong(KEY_CHECKPOINT_DAY_TIME);

        if (nbt.hasKey(KEY_CHECKPOINT_INVENTORY, Constants.NBT.TAG_LIST)) {
            NBTTagList invListTag = nbt.getTagList(KEY_CHECKPOINT_INVENTORY, Constants.NBT.TAG_COMPOUND);
            this.checkpointInventory.clear();
            for (int i = 0; i < invListTag.tagCount(); i++) {
                this.checkpointInventory.add(new ItemStack(invListTag.getCompoundTagAt(i)));
            }
        }

        if (nbt.hasKey(KEY_ENTITY_DATA, Constants.NBT.TAG_LIST)) {
            NBTTagList entityListTag = nbt.getTagList(KEY_ENTITY_DATA, Constants.NBT.TAG_COMPOUND);
            this.entityData.clear();
            for (int i = 0; i < entityListTag.tagCount(); i++) {
                this.entityData.add(entityListTag.getCompoundTagAt(i));
            }
        }

        if (nbt.hasKey(KEY_ENTITY_DIMENSIONS, Constants.NBT.TAG_LIST)) {
            // Note: Storing dimensions as Int Array or List of Ints is efficient,
            // but to keep logic similar we read the list. NBTTagList of Ints (TAG_INT = 3)
            NBTTagList entityDimListTag = nbt.getTagList(KEY_ENTITY_DIMENSIONS, Constants.NBT.TAG_INT);
            this.entityDimensions.clear();
            for (int i = 0; i < entityDimListTag.tagCount(); i++) {
                this.entityDimensions.add(entityDimListTag.getIntAt(i));
            }
        }

        if (this.entityData.size() > 0 && this.entityDimensions.size() > 0 && this.entityData.size() != this.entityDimensions.size()) {
            LOGGER.warn("Mismatch in loaded entityData ({}) and entityDimensions ({}).", this.entityData.size(), this.entityDimensions.size());
        }

        if (nbt.hasKey(KEY_ENTITY_AGGRO_TARGETS, Constants.NBT.TAG_COMPOUND)) {
            NBTTagCompound aggroTag = nbt.getCompoundTag(KEY_ENTITY_AGGRO_TARGETS);
            this.entityAggroTargets.clear();
            for (String key : aggroTag.getKeySet()) {
                try {
                    UUID entityUUID = UUID.fromString(key);
                    // In 1.12, no getUUID, so we read String
                    String targetUuidStr = aggroTag.getString(key);
                    if (!targetUuidStr.isEmpty()) {
                        this.entityAggroTargets.put(entityUUID, UUID.fromString(targetUuidStr));
                    }
                } catch (IllegalArgumentException e) {
                    LOGGER.warn("Failed to parse UUID for aggro target key '{}' from NBT", key, e);
                }
            }
        }

        if (nbt.hasKey(KEY_GROUND_ITEMS, Constants.NBT.TAG_LIST)) {
            NBTTagList groundItemsListTag = nbt.getTagList(KEY_GROUND_ITEMS, Constants.NBT.TAG_COMPOUND);
            this.groundItems.clear();
            for (int i = 0; i < groundItemsListTag.tagCount(); i++) {
                this.groundItems.add(groundItemsListTag.getCompoundTagAt(i));
            }
        }

        if (nbt.hasKey(KEY_WORLD_DATA, Constants.NBT.TAG_COMPOUND)) {
            this.worldData.loadFromNBT(nbt.getCompoundTag(KEY_WORLD_DATA));
        } else {
            LOGGER.warn("Checkpoint NBT is missing WorldData. A new empty WorldData will be used.");
        }

        LOGGER.debug("CheckpointData loaded from NBT.");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        nbt.setInteger(KEY_CHECKPOINT_DIMENSION, checkpointDimension);

        NBTTagCompound playersNbt = new NBTTagCompound();
        for (Map.Entry<UUID, NBTTagCompound> entry : playersData.entrySet()) {
            playersNbt.setTag(entry.getKey().toString(), entry.getValue());
        }
        nbt.setTag(KEY_PLAYERS_DATA, playersNbt);

        nbt.setInteger(KEY_FIRE_TICKS, fireTicks);

        if (anchorPlayerUUID != null) {
            nbt.setString(KEY_ANCHOR_PLAYER_UUID, anchorPlayerUUID.toString());
        }

        if (checkpointPos != null) {
            nbt.setInteger(KEY_CHECKPOINT_POS_X, checkpointPos.getX());
            nbt.setInteger(KEY_CHECKPOINT_POS_Y, checkpointPos.getY());
            nbt.setInteger(KEY_CHECKPOINT_POS_Z, checkpointPos.getZ());
        }
        nbt.setFloat(KEY_CHECKPOINT_HEALTH, checkpointHealth);
        nbt.setInteger(KEY_CHECKPOINT_HUNGER, checkpointHunger);
        nbt.setInteger(KEY_CHECKPOINT_XP, checkpointXP);
        nbt.setLong(KEY_CHECKPOINT_DAY_TIME, checkpointDayTime);

        NBTTagList invListTag = new NBTTagList();
        for (ItemStack stack : checkpointInventory) {
            if (!stack.isEmpty()) {
                invListTag.appendTag(stack.writeToNBT(new NBTTagCompound()));
            }
        }
        nbt.setTag(KEY_CHECKPOINT_INVENTORY, invListTag);

        NBTTagList entityListNbt = new NBTTagList();
        for (NBTTagCompound entityNBT : entityData) {
            entityListNbt.appendTag(entityNBT);
        }
        nbt.setTag(KEY_ENTITY_DATA, entityListNbt);

        NBTTagList entityDimListNbt = new NBTTagList();
        for (Integer dimKey : entityDimensions) {
            // Using NBTTagInt (implied by just creating a list of ints? No, 1.12 is strict)
            // But NBTTagList only holds one type. We can use IntArray or List of Strings/Ints.
            // Let's use NBTTagList of Integers isn't directly supported via appendTag(int).
            // We must wrap in NBTTagInt.
            entityDimListNbt.appendTag(new net.minecraft.nbt.NBTTagInt(dimKey));
        }
        nbt.setTag(KEY_ENTITY_DIMENSIONS, entityDimListNbt);

        NBTTagCompound aggroTargetsNbt = new NBTTagCompound();
        for (Map.Entry<UUID, UUID> entry : entityAggroTargets.entrySet()) {
            aggroTargetsNbt.setString(entry.getKey().toString(), entry.getValue().toString());
        }
        nbt.setTag(KEY_ENTITY_AGGRO_TARGETS, aggroTargetsNbt);

        NBTTagList groundItemsListNbt = new NBTTagList();
        for (NBTTagCompound itemNBT : groundItems) {
            groundItemsListNbt.appendTag(itemNBT);
        }
        nbt.setTag(KEY_GROUND_ITEMS, groundItemsListNbt);

        NBTTagCompound worldDataNbt = new NBTTagCompound();
        if (this.worldData != null) {
            this.worldData.saveToNBT(worldDataNbt);
        }
        nbt.setTag(KEY_WORLD_DATA, worldDataNbt);

        LOGGER.debug("CheckpointData saved to NBT.");
        return nbt;
    }

    /**
     * Gets or creates the global checkpoint data.
     * In 1.12.2, we generally use the Overworld (Dim 0) storage for "Global" mod data.
     */
    public static CheckpointData get(World world) {
        // Ensure we are getting the MapStorage from the overworld (dimension 0) to keep it global
        MapStorage storage = world.getMapStorage();

        CheckpointData instance = (CheckpointData) storage.getOrLoadData(CheckpointData.class, DATA_NAME);

        if (instance == null) {
            instance = new CheckpointData(DATA_NAME);
            storage.setData(DATA_NAME, instance);
        }
        return instance;
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

    public void setEntityData(List<NBTTagCompound> entities) {
        this.entityData = entities;
        this.markDirty();
    }

    public void setGroundItems(List<NBTTagCompound> groundItems) {
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

    public List<NBTTagCompound> getEntityData() {
        return entityData;
    }

    public List<Integer> getEntityDimensions() {
        return entityDimensions;
    }

    public void setEntityDataWithDimensions(List<NBTTagCompound> entities, List<Integer> dimensions) {
        this.entityData = entities;
        this.entityDimensions = dimensions;
        this.markDirty();
    }

    public List<NBTTagCompound> getGroundItems() {
        return groundItems;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("CheckpointData Instance:\n");
        sb.append("  AnchorPlayerUUID: ").append(anchorPlayerUUID).append("\n");
        sb.append("  CheckpointPos: ").append(checkpointPos).append("\n");
        sb.append("  CheckpointDimension: ").append(checkpointDimension).append("\n");
        sb.append("  CheckpointHealth: ").append(checkpointHealth).append("\n");
        sb.append("  CheckpointHunger: ").append(checkpointHunger).append("\n");
        sb.append("  CheckpointXP: ").append(checkpointXP).append("\n");
        sb.append("  CheckpointDayTime: ").append(checkpointDayTime).append("\n");
        sb.append("  FireTicks: ").append(fireTicks).append("\n");
        sb.append("  EntityData Count: ").append(entityData.size()).append("\n");
        sb.append("  EntityDimensions Count: ").append(entityDimensions.size()).append("\n");
        sb.append("  GroundItems Count: ").append(groundItems.size()).append("\n");
        sb.append("  CheckpointInventory Count: ").append(checkpointInventory.size()).append("\n");
        sb.append("  PlayersData Count: ").append(playersData.size()).append("\n");
        sb.append("  WorldData Present: ").append(worldData != null).append("\n");
        return sb.toString();
    }
}