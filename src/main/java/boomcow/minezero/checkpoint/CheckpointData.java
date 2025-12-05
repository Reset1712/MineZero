package boomcow.minezero.checkpoint;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.Constants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class CheckpointData extends WorldSavedData {
    public static final String DATA_NAME = "global_checkpoint";
    private static final Logger LOGGER = LogManager.getLogger();

    private static final String KEY_PLAYERS_DATA = "playersData";
    private static final String KEY_ANCHOR_PLAYER_UUID = "anchorPlayerUUID";
    private static final String KEY_ENTITY_DATA = "entityData";
    private static final String KEY_ENTITY_DIMENSIONS = "entityDimensions";
    private static final String KEY_ENTITY_AGGRO_TARGETS = "entityAggroTargets";
    private static final String KEY_GROUND_ITEMS = "groundItems";
    private static final String KEY_WORLD_DATA = "worldCheckpointData";

    private Map<UUID, NBTTagCompound> playersData = new HashMap<>();
    private UUID anchorPlayerUUID;

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

        if (nbt.hasKey(KEY_ANCHOR_PLAYER_UUID)) {
            try {
                this.anchorPlayerUUID = UUID.fromString(nbt.getString(KEY_ANCHOR_PLAYER_UUID));
            } catch (Exception e) {
                LOGGER.warn("Failed to parse Anchor Player UUID");
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

        NBTTagCompound playersNbt = new NBTTagCompound();
        for (Map.Entry<UUID, NBTTagCompound> entry : playersData.entrySet()) {
            playersNbt.setTag(entry.getKey().toString(), entry.getValue());
        }
        nbt.setTag(KEY_PLAYERS_DATA, playersNbt);

        if (anchorPlayerUUID != null) {
            nbt.setString(KEY_ANCHOR_PLAYER_UUID, anchorPlayerUUID.toString());
        }

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
        if (world.isRemote) {
            throw new RuntimeException("Attempted to access CheckpointData from client-side world.");
        }

        // ALWAYS use the Overworld (Dimension 0) MapStorage to ensure data is global across dimensions.
        // If we use 'world.getMapStorage()' on a Nether world, it saves to DIM-1/data, which is not loaded when in Overworld.
        WorldServer overworld = DimensionManager.getWorld(0);
        if (overworld == null) {
            // Fallback (should unlikely happen on a running server)
            LOGGER.warn("Overworld not found via DimensionManager! Using provided world storage.");
            overworld = (WorldServer) world;
        }

        MapStorage storage = overworld.getMapStorage();
        CheckpointData instance = (CheckpointData) storage.getOrLoadData(CheckpointData.class, DATA_NAME);

        if (instance != null) {
             LOGGER.info("CheckpointData instance: {}", instance);
        }

        if (instance == null) {
            instance = new CheckpointData(DATA_NAME);
            storage.setData(DATA_NAME, instance);
        }
        return instance;
    }

    public void setEntityAggroTargets(Map<UUID, UUID> aggroTargets) {
        this.entityAggroTargets = aggroTargets;
        this.markDirty();
    }

    public Map<UUID, UUID> getEntityAggroTargets() {
        return entityAggroTargets;
    }

    public void setEntityData(List<NBTTagCompound> entities) {
        this.entityData = entities;
        this.markDirty();
    }

    public void setGroundItems(List<NBTTagCompound> groundItems) {
        this.groundItems = groundItems;
        this.markDirty();
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
        sb.append("  EntityData Count: ").append(entityData.size()).append("\n");
        sb.append("  EntityDimensions Count: ").append(entityDimensions.size()).append("\n");
        sb.append("  GroundItems Count: ").append(groundItems.size()).append("\n");
        sb.append("  PlayersData Count: ").append(playersData.size()).append("\n");
        sb.append("  WorldData Present: ").append(worldData != null).append("\n");
        return sb.toString();
    }
}
