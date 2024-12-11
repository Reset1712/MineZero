package boomcow.minezero.checkpoint;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CheckpointData extends SavedData {
    public static final String DATA_NAME = "minezero_checkpoint";

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
    private List<CompoundTag> groundItems = new ArrayList<>();

    public CheckpointData() {}

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

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        if (checkpointPos != null) {
            nbt.putInt("PosX", checkpointPos.getX());
            nbt.putInt("PosY", checkpointPos.getY());
            nbt.putInt("PosZ", checkpointPos.getZ());
        }
        nbt.putFloat("Health", checkpointHealth);
        nbt.putInt("Hunger", checkpointHunger);
        nbt.putInt("XP", checkpointXP);

        nbt.putLong("DayTime", checkpointDayTime);

        // Save inventory
        ListTag invList = new ListTag();
        for (ItemStack stack : checkpointInventory) {
            CompoundTag stackTag = new CompoundTag();
            stack.save(stackTag);
            invList.add(stackTag);
        }
        nbt.put("Inventory", invList);

        // Save entities
        ListTag entityList = new ListTag();
        for (CompoundTag eNBT : entityData) {
            entityList.add(eNBT);
        }
        nbt.put("Entities", entityList);

        // Save ground items
        ListTag groundItemsList = new ListTag();
        for (CompoundTag itemNBT : groundItems) {
            groundItemsList.add(itemNBT);
        }
        nbt.put("GroundItems", groundItemsList);
        nbt.putInt("FireTicks", fireTicks);

        if (anchorPlayerUUID != null) {
            nbt.putUUID("AnchorPlayerUUID", anchorPlayerUUID);
        }

        return nbt;
    }

    public static CheckpointData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(CheckpointData::load, CheckpointData::new, DATA_NAME);
    }

    public void setCheckpointPos(BlockPos pos) {
        this.checkpointPos = pos;
        this.setDirty();
    }

    public void setCheckpointHealth(float health) {
        this.checkpointHealth = health;
        this.setDirty();
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

    public List<CompoundTag> getGroundItems() {
        return groundItems;
    }
}
