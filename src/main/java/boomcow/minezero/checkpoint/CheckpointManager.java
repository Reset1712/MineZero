package boomcow.minezero.checkpoint;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

public class CheckpointManager {

    public static void setCheckpoint(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        CheckpointData data = CheckpointData.get(level);

        // Save player position
        BlockPos pos = player.blockPosition();
        data.setCheckpointPos(pos);

        // Save player inventory
        List<ItemStack> inv = new ArrayList<>();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            inv.add(player.getInventory().getItem(i).copy());
        }
        data.setCheckpointInventory(inv);

        // Save health, hunger, xp
        data.setCheckpointHealth(player.getHealth());
        data.setCheckpointHunger(player.getFoodData().getFoodLevel());
        data.setCheckpointXP(player.experienceLevel);

        // Save day time
        long dayTime = level.getDayTime();
        data.setCheckpointDayTime(dayTime);

        // Save only mobs and players
        List<CompoundTag> entityList = new ArrayList<>();
        for (Entity e : level.getAllEntities()) {
            //System.out.println(e.getName() + " " + e.getType().getCategory() + "");
            if (e instanceof Mob || e instanceof ServerPlayer) {

                CompoundTag entityNBT = new CompoundTag();
                e.save(entityNBT);
                if (EntityType.byString(entityNBT.getString("id")).isPresent()) {
                    entityList.add(entityNBT);
                }
            }
        }
        data.setEntityData(entityList);

        // Save items on the ground
        List<CompoundTag> groundItemsList = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof ItemEntity itemEntity) {
                CompoundTag itemNBT = new CompoundTag();
                itemEntity.save(itemNBT);
                groundItemsList.add(itemNBT);
            }
        }
        data.setGroundItems(groundItemsList);
        data.setFireTicks(player.getRemainingFireTicks());
        // Set the anchor player
        data.setAnchorPlayerUUID(player.getUUID());

    }

    public static void restoreCheckpoint(ServerPlayer player) {
        try {
            ServerLevel level = player.serverLevel();
            CheckpointData data = CheckpointData.get(level);

            // Validate checkpoint data
            if (data.getCheckpointPos() == null) {
                return; // No checkpoint position found, exit.
            }

            // Teleport player to checkpoint position
            BlockPos pos = data.getCheckpointPos();
            player.teleportTo(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.getYRot(), player.getXRot());

            // Restore player health
            player.setHealth(data.getCheckpointHealth());

            // Restore player hunger
            player.getFoodData().setFoodLevel(data.getCheckpointHunger());

            // Restore player XP
            player.setExperiencePoints(data.getCheckpointXP());
            player.setRemainingFireTicks(data.getFireTicks());

            // Restore player inventory
            List<ItemStack> inv = data.getCheckpointInventory();
            if (inv != null) {
                player.getInventory().clearContent();
                for (int i = 0; i < inv.size(); i++) {
                    player.getInventory().setItem(i, inv.get(i).copy());
                }
            }

            // Remove all non-player entities
            List<Entity> entitiesToRemove = new ArrayList<>();
            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof ServerPlayer) && !entity.isRemoved()) {
                    entitiesToRemove.add(entity);
                }
            }
            for (Entity entity : entitiesToRemove) {
                entity.discard(); // Safely remove the entity
            }

            // Restore mobs and players from checkpoint data
            List<CompoundTag> entities = data.getEntityData();
            if (entities != null) {
                for (CompoundTag eNBT : entities) {
                    boolean alreadyExists = StreamSupport.stream(level.getAllEntities().spliterator(), false)
                            .anyMatch(e -> e.getId() == eNBT.getInt("id")); // Replace with appropriate comparison

                    if (!alreadyExists) {
                        EntityType.loadEntityRecursive(eNBT, level, (entity) -> {
                            if (entity != null) {
                                level.addFreshEntity(entity); // Safely add entity
                            }
                            return entity;
                        });
                    }
                }
            }

            // Restore items on the ground
            List<CompoundTag> groundItemsList = data.getGroundItems();
            if (groundItemsList != null) {
                for (CompoundTag itemNBT : groundItemsList) {
                    System.out.println("weener");
                    EntityType.loadEntityRecursive(itemNBT, level, (entity) -> {
                        System.out.println(entity);
                        System.out.println(entity.getType());
                        if (entity instanceof ItemEntity) {
                            level.addFreshEntity(entity); // Safely add item entity
                        }
                        return entity;
                    });
                }
            }

            // Restore day time
            level.setDayTime(data.getCheckpointDayTime());

        } catch (Exception e) {
            e.printStackTrace(); // Log the exception for debugging
        }
    }


}