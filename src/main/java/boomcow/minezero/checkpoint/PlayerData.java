package boomcow.minezero.checkpoint;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.common.util.Constants;

import java.util.ArrayList;
import java.util.List;

public class PlayerData {
    public double posX;
    public double posY;
    public double posZ;
    public double motionX;
    public double motionY;
    public double motionZ;
    public float fallDistance;
    public float yaw;
    public float pitch;
    public float health;
    public int hunger;
    public int experienceLevel;
    public float experienceProgress;
    public int fireTicks;

    // 1.12.2 uses integer IDs for dimensions
    public int dimension;
    public List<ItemStack> inventory = new ArrayList<>();
    public String gameMode;
    public double spawnX;
    public double spawnY;
    public double spawnZ;
    public int spawnDimension;
    public boolean spawnForced;

    // MobEffectInstance -> PotionEffect
    public List<PotionEffect> potionEffects = new ArrayList<>();
    public NBTTagCompound advancements = new NBTTagCompound();

    public NBTTagCompound toNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setDouble("PosX", posX);
        tag.setDouble("PosY", posY);
        tag.setDouble("PosZ", posZ);
        tag.setDouble("MotionX", motionX);
        tag.setDouble("MotionY", motionY);
        tag.setDouble("MotionZ", motionZ);
        tag.setFloat("FallDistance", fallDistance);
        tag.setFloat("Yaw", yaw);
        tag.setFloat("Pitch", pitch);
        tag.setFloat("Health", health);
        tag.setInteger("Hunger", hunger);

        tag.setInteger("FireTicks", fireTicks);
        if (gameMode != null) {
            tag.setString("GameMode", gameMode);
        }
        tag.setInteger("ExperienceLevel", experienceLevel);
        tag.setFloat("ExperienceProgress", experienceProgress);

        tag.setDouble("SpawnX", spawnX);
        tag.setDouble("SpawnY", spawnY);
        tag.setDouble("SpawnZ", spawnZ);
        tag.setBoolean("SpawnForced", spawnForced);

        // Dimensions are integers in 1.12
        tag.setInteger("SpawnDimension", spawnDimension);
        tag.setInteger("Dimension", dimension);

        NBTTagList effectsTag = new NBTTagList();
        for (PotionEffect effect : potionEffects) {
            NBTTagCompound effectTag = new NBTTagCompound();
            effect.writeCustomPotionEffectToNBT(effectTag);
            effectsTag.appendTag(effectTag);
        }
        tag.setTag("PotionEffects", effectsTag);

        if (advancements != null) {
            tag.setTag("Advancements", advancements);
        }

        NBTTagCompound invTag = new NBTTagCompound();
        for (int i = 0; i < inventory.size(); i++) {
            NBTTagCompound stackTag = new NBTTagCompound();
            inventory.get(i).writeToNBT(stackTag);
            invTag.setTag("Slot" + i, stackTag);
        }
        tag.setTag("Inventory", invTag);

        return tag;
    }

    public static PlayerData fromNBT(NBTTagCompound tag) {
        PlayerData data = new PlayerData();
        data.posX = tag.getDouble("PosX");
        data.posY = tag.getDouble("PosY");
        data.posZ = tag.getDouble("PosZ");
        data.fallDistance = tag.getFloat("FallDistance");
        data.motionX = tag.getDouble("MotionX");
        data.motionY = tag.getDouble("MotionY");
        data.motionZ = tag.getDouble("MotionZ");
        data.yaw = tag.getFloat("Yaw");
        data.pitch = tag.getFloat("Pitch");
        data.health = tag.getFloat("Health");
        data.hunger = tag.getInteger("Hunger");
        data.fireTicks = tag.getInteger("FireTicks");

        data.spawnX = tag.getDouble("SpawnX");
        data.spawnY = tag.getDouble("SpawnY");
        data.spawnZ = tag.getDouble("SpawnZ");
        data.spawnForced = tag.getBoolean("SpawnForced");

        data.experienceLevel = tag.getInteger("ExperienceLevel");
        data.experienceProgress = tag.getFloat("ExperienceProgress");

        // Load Dimensions
        if (tag.hasKey("SpawnDimension")) {
            data.spawnDimension = tag.getInteger("SpawnDimension");
        }
        if (tag.hasKey("Dimension")) {
            data.dimension = tag.getInteger("Dimension");
        }

        data.potionEffects.clear();
        // 10 is the ID for CompoundTag
        NBTTagList effectsTag = tag.getTagList("PotionEffects", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < effectsTag.tagCount(); i++) {
            NBTTagCompound effectTag = effectsTag.getCompoundTagAt(i);
            PotionEffect effect = PotionEffect.readCustomPotionEffectFromNBT(effectTag);
            if (effect != null) {
                data.potionEffects.add(effect);
            }
        }

        if (tag.hasKey("Advancements")) {
            data.advancements = tag.getCompoundTag("Advancements");
        }

        if (tag.hasKey("GameMode")) {
            data.gameMode = tag.getString("GameMode");
        }

        NBTTagCompound invTag = tag.getCompoundTag("Inventory");
        data.inventory.clear();
        int i = 0;
        while (invTag.hasKey("Slot" + i)) {
            NBTTagCompound stackTag = invTag.getCompoundTag("Slot" + i);
            data.inventory.add(new ItemStack(stackTag));
            i++;
        }

        return data;
    }
}