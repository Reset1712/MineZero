package boomcow.minezero.checkpoint;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
    public RegistryKey<World> dimension;
    public List<ItemStack> inventory = new ArrayList<>();
    public String gameMode;
    public double spawnX;
    public double spawnY;
    public double spawnZ;
    public RegistryKey<World> spawnDimension;
    public boolean spawnForced;

    public List<StatusEffectInstance> potionEffects = new ArrayList<>();
    public NbtCompound advancements = new NbtCompound();

    public NbtCompound toNBT(RegistryWrapper.WrapperLookup lookupProvider) {
        NbtCompound tag = new NbtCompound();
        tag.putDouble("PosX", posX);
        tag.putDouble("PosY", posY);
        tag.putDouble("PosZ", posZ);
        tag.putDouble("MotionX", motionX);
        tag.putDouble("MotionY", motionY);
        tag.putDouble("MotionZ", motionZ);
        tag.putFloat("FallDistance", fallDistance);
        tag.putFloat("Yaw", yaw);
        tag.putFloat("Pitch", pitch);
        tag.putFloat("Health", health);
        tag.putInt("Hunger", hunger);

        tag.putInt("FireTicks", fireTicks);
        tag.putString("GameMode", gameMode);
        tag.putInt("ExperienceLevel", experienceLevel);
        tag.putFloat("ExperienceProgress", experienceProgress);

        tag.putDouble("SpawnX", spawnX);
        tag.putDouble("SpawnY", spawnY);
        tag.putDouble("SpawnZ", spawnZ);
        tag.putBoolean("SpawnForced", spawnForced);

        if (spawnDimension != null) {
            tag.putString("SpawnDimension", spawnDimension.getValue().toString());
        }
        NbtList effectsTag = new NbtList();
        for (StatusEffectInstance effect : potionEffects) {
            NbtElement effectNbtTag = effect.writeNbt();
            effectsTag.add(effectNbtTag);
        }
        tag.put("PotionEffects", effectsTag);
        if (advancements != null) {
            tag.put("Advancements", advancements);
        }
        if (dimension != null) {
            tag.putString("Dimension", dimension.getValue().toString());
        }
        NbtCompound invSlotsTag = new NbtCompound();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack currentStack = inventory.get(i);
            if (currentStack != null && !currentStack.isEmpty()) {
                NbtElement stackNbt = currentStack.encode(lookupProvider);
                invSlotsTag.put("Slot" + i, stackNbt);
            }
        }
        if (!invSlotsTag.isEmpty()) {
            tag.put("Inventory", invSlotsTag);
        }

        return tag;
    }

    public static PlayerData fromNBT(NbtCompound tag, RegistryWrapper.WrapperLookup lookupProvider) {
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
        data.hunger = tag.getInt("Hunger");
        data.fireTicks = tag.getInt("FireTicks");

        data.spawnX = tag.getDouble("SpawnX");
        data.spawnY = tag.getDouble("SpawnY");
        data.spawnZ = tag.getDouble("SpawnZ");
        data.spawnForced = tag.getBoolean("SpawnForced");

        data.experienceLevel = tag.getInt("ExperienceLevel");
        data.experienceProgress = tag.getFloat("ExperienceProgress");

        if (tag.contains("SpawnDimension", NbtElement.STRING_TYPE)) {
            String spawnDimString = tag.getString("SpawnDimension");
            try {
                data.spawnDimension = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(spawnDimString));
            } catch (Exception e) {
                System.err.println("Failed to parse SpawnDimension Identifier string: " + spawnDimString + " - " + e.getMessage());
                data.spawnDimension = null;
            }
        }
        data.potionEffects.clear();
        NbtList effectsTag = tag.getList("PotionEffects", 10);
        for (int i = 0; i < effectsTag.size(); i++) {
            NbtCompound effectTag = effectsTag.getCompound(i);
            StatusEffectInstance effect = StatusEffectInstance.fromNbt(effectTag);
            if (effect != null) {
                data.potionEffects.add(effect);
            }
        }
        if (tag.contains("Advancements")) {
            data.advancements = tag.getCompound("Advancements");
        }
        if (tag.contains("Dimension", NbtElement.STRING_TYPE)) {
            String currentDimString = tag.getString("Dimension");
            try {
                data.dimension = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(currentDimString));
            } catch (Exception e) {
                System.err.println("Failed to parse player Dimension Identifier string: " + currentDimString + " - " + e.getMessage());
                data.dimension = null;
            }
        }

        if (tag.contains("GameMode")) {
            data.gameMode = tag.getString("GameMode");
        }

        data.inventory.clear();
        if (tag.contains("Inventory", NbtElement.COMPOUND_TYPE)) {
            NbtCompound invSlotsTag = tag.getCompound("Inventory");
            int i = 0;
            while (invSlotsTag.contains("Slot" + i, NbtElement.COMPOUND_TYPE) || invSlotsTag.contains("Slot" + i)) {
                 NbtElement stackNbt = invSlotsTag.get("Slot" + i);
                 Optional<ItemStack> parsedStackOptional = ItemStack.fromNbt(lookupProvider, stackNbt);

                if (parsedStackOptional.isPresent()) {
                    data.inventory.add(parsedStackOptional.get());
                } else {
                    data.inventory.add(ItemStack.EMPTY);
                    System.err.println("Failed to parse ItemStack from NBT for slot " + i);
                }
                i++;
            }
        }

        return data;
    }
}
