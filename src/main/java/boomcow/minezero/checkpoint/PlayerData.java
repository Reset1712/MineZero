package boomcow.minezero.checkpoint;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

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
    public ResourceKey<Level> dimension;
    public List<ItemStack> inventory = new ArrayList<>();
    public String gameMode;
    public double spawnX;
    public double spawnY;
    public double spawnZ;
    public ResourceKey<Level> spawnDimension;
    public boolean spawnForced;

    public List<MobEffectInstance> potionEffects = new ArrayList<>();
    // New field for advancements.
    public CompoundTag advancements = new CompoundTag();

    public CompoundTag toNBT(HolderLookup.Provider lookupProvider) {
        CompoundTag tag = new CompoundTag();
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
            tag.putString("SpawnDimension", spawnDimension.location().toString());
        }

        // Save potion effects
        ListTag effectsTag = new ListTag();
        for (MobEffectInstance effect : potionEffects) {
            // 1. Get the Tag from save()
            Tag effectNbtTag = effect.save(); // save() returns Tag

            // 2. ListTag.add(Tag) accepts any Tag, so this is fine.
            //    No cast to CompoundTag is strictly necessary for the .add() operation itself.
            effectsTag.add(effectNbtTag);

            // If you needed to operate on it as a CompoundTag for some other reason (not needed here):
            // if (effectNbtTag instanceof CompoundTag) {
            //     CompoundTag specificEffectCompoundTag = (CompoundTag) effectNbtTag;
            //     // Now you can use specificEffectCompoundTag with methods requiring CompoundTag
            // } else if (effectNbtTag != null) {
            //     // This would be unexpected if MobEffectInstance always saves as CompoundTag
            //     PD_LOGGER.warn("MobEffectInstance saved as a Tag type other than CompoundTag: {}", effectNbtTag.getType().getName());
            // }
        }
        tag.put("PotionEffects", effectsTag);

        // Save advancements
        if (advancements != null) {
            tag.put("Advancements", advancements);
        }

        // Save dimension
        if (dimension != null) {
            tag.putString("Dimension", dimension.location().toString());
        }

        // --- CORRECTED Inventory Saving ---
        CompoundTag invSlotsTag = new CompoundTag(); // Your original structure for inventory
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack currentStack = inventory.get(i);
            if (currentStack != null && !currentStack.isEmpty()) {
                // ItemStack.save(HolderLookup.Provider) returns CompoundTag
                Tag stackNbt = currentStack.save(lookupProvider);
                invSlotsTag.put("Slot" + i, stackNbt);
            }
        }
        if (!invSlotsTag.isEmpty()) {
            tag.put("Inventory", invSlotsTag);
        }



        return tag;
    }

    public static PlayerData fromNBT(CompoundTag tag, HolderLookup.Provider lookupProvider) {
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


        if (tag.contains("SpawnDimension", Tag.TAG_STRING)) { // Good practice to check type with Tag.TAG_STRING (which is 8)
            String spawnDimString = tag.getString("SpawnDimension");
            try {
                // --- CORRECTED ResourceLocation creation ---
                data.spawnDimension = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(spawnDimString));
                // --- END CORRECTION ---
            } catch (Exception e) { // Catch potential errors from ResourceLocation.parse if string is invalid
                // PD_LOGGER.error("Failed to parse SpawnDimension ResourceLocation string: {}", spawnDimString, e);
                System.err.println("Failed to parse SpawnDimension ResourceLocation string: " + spawnDimString + " - " + e.getMessage());
                data.spawnDimension = null; // Or set to a default like Level.OVERWORLD if appropriate
            }
        }

        // Load potion effects
        data.potionEffects.clear();
        ListTag effectsTag = tag.getList("PotionEffects", 10);
        for (int i = 0; i < effectsTag.size(); i++) {
            CompoundTag effectTag = effectsTag.getCompound(i);
            MobEffectInstance effect = MobEffectInstance.load(effectTag);
            if (effect != null) {
                data.potionEffects.add(effect);
            }
        }

        // Load advancements
        if (tag.contains("Advancements")) {
            data.advancements = tag.getCompound("Advancements");
        }

        // Load dimension
        if (tag.contains("Dimension", Tag.TAG_STRING)) {
            String currentDimString = tag.getString("Dimension");
            try {
                // --- CORRECTED ResourceLocation creation ---
                data.dimension = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(currentDimString));
                // --- END CORRECTION ---
            } catch (Exception e) {
                // PD_LOGGER.error("Failed to parse player Dimension ResourceLocation string: {}", currentDimString, e);
                System.err.println("Failed to parse player Dimension ResourceLocation string: " + currentDimString + " - " + e.getMessage());
                data.dimension = null; // Or set to a default
            }
        }

        if (tag.contains("GameMode")) {
            data.gameMode = tag.getString("GameMode");
        }

        CompoundTag invTag = tag.getCompound("Inventory");
        data.inventory.clear();
        if (tag.contains("Inventory", Tag.TAG_COMPOUND)) {
            CompoundTag invSlotsTag = tag.getCompound("Inventory"); // Use the variable name you used in toNBT
            int i = 0;
            // Loop while "Slot" + i exists. Consider a max slot count for safety if NBT could be malformed.
            while (invSlotsTag.contains("Slot" + i, Tag.TAG_COMPOUND)) { // Check type with Tag.TAG_COMPOUND (which is 10)
                CompoundTag stackNbt = invSlotsTag.getCompound("Slot" + i);

                // --- CORRECTED ItemStack deserialization ---
                // Use ItemStack.parse(HolderLookup.Provider, CompoundTag) which returns Optional<ItemStack>
                Optional<ItemStack> parsedStackOptional = ItemStack.parse(lookupProvider, stackNbt);

                // Add the ItemStack if present, otherwise add ItemStack.EMPTY or handle error
                if (parsedStackOptional.isPresent()) {
                    data.inventory.add(parsedStackOptional.get());
                } else {
                    // If parsing fails, the Optional will be empty.
                    // You might want to add an empty stack to maintain slot count, or log an error.
                    data.inventory.add(ItemStack.EMPTY); // Add an empty stack to keep slot indices consistent if needed
                    // PD_LOGGER.warn("Failed to parse ItemStack from NBT for slot {}: {}", i, stackNbt);
                    System.err.println("Failed to parse ItemStack from NBT for slot " + i + ": " + stackNbt);
                }
                // --- END CORRECTION ---
                i++;
            }
        }


        return data;
    }
}
