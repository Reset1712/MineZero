package boomcow.minezero.checkpoint;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class PlayerData {
    public double posX;
    public double posY;
    public double posZ;
    public float yaw;
    public float pitch;
    public float health;
    public int hunger;
    public int xp;
    public int fireTicks;
    public List<ItemStack> inventory = new ArrayList<>();

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("PosX", posX);
        tag.putDouble("PosY", posY);
        tag.putDouble("PosZ", posZ);
        tag.putFloat("Yaw", yaw);
        tag.putFloat("Pitch", pitch);
        tag.putFloat("Health", health);
        tag.putInt("Hunger", hunger);
        tag.putInt("XP", xp);
        tag.putInt("FireTicks", fireTicks);

        CompoundTag invTag = new CompoundTag();
        for (int i = 0; i < inventory.size(); i++) {
            CompoundTag stackTag = new CompoundTag();
            inventory.get(i).save(stackTag);
            invTag.put("Slot" + i, stackTag);
        }
        tag.put("Inventory", invTag);

        return tag;
    }

    public static PlayerData fromNBT(CompoundTag tag) {
        PlayerData data = new PlayerData();
        data.posX = tag.getDouble("PosX");
        data.posY = tag.getDouble("PosY");
        data.posZ = tag.getDouble("PosZ");
        data.yaw = tag.getFloat("Yaw");
        data.pitch = tag.getFloat("Pitch");
        data.health = tag.getFloat("Health");
        data.hunger = tag.getInt("Hunger");
        data.xp = tag.getInt("XP");
        data.fireTicks = tag.getInt("FireTicks");

        CompoundTag invTag = tag.getCompound("Inventory");
        data.inventory.clear();
        int i = 0;
        while (invTag.contains("Slot" + i)) {
            CompoundTag stackTag = invTag.getCompound("Slot" + i);
            data.inventory.add(ItemStack.of(stackTag));
            i++;
        }

        return data;
    }
}
