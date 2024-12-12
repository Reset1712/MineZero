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

    public static void setCheckpoint(ServerPlayer anchorPlayer) {
        ServerLevel level = anchorPlayer.serverLevel();
        CheckpointData data = CheckpointData.get(level);

        // Set the anchor player
        data.setAnchorPlayerUUID(anchorPlayer.getUUID());

        // Capture all currently online players' data
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            PlayerData pdata = new PlayerData();
            // Store player position
            pdata.posX = player.getX();
            pdata.posY = player.getY();
            pdata.posZ = player.getZ();
            pdata.yaw = player.getYRot();
            pdata.pitch = player.getXRot();

            // Store health, hunger, xp
            pdata.health = player.getHealth();
            pdata.hunger = player.getFoodData().getFoodLevel();
            pdata.xp = player.experienceLevel; // or setExperiencePoints if needed
            pdata.fireTicks = player.getRemainingFireTicks();

            // Store inventory
            pdata.inventory.clear();
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i).copy();
                pdata.inventory.add(stack);
            }

            data.savePlayerData(player.getUUID(), pdata);
        }

        // Save day time
        data.setCheckpointDayTime(level.getDayTime());

        // Save only mobs and players
        List<CompoundTag> entityList = new ArrayList<>();
        for (Entity e : level.getAllEntities()) {
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
    }

    public static void restoreCheckpoint(ServerPlayer anchorPlayer) {
        try {
            ServerLevel level = anchorPlayer.serverLevel();
            CheckpointData data = CheckpointData.get(level);
            if (data.getCheckpointPos() == null) {
                return;
            }
            // Restore day time
            level.setDayTime(data.getCheckpointDayTime());

            // Restore all players from their saved data
            for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
                PlayerData pdata = data.getPlayerData(player.getUUID());
                if (pdata != null) {
                    // Restore position
                    player.teleportTo(level, pdata.posX, pdata.posY, pdata.posZ, pdata.yaw, pdata.pitch);

                    // Restore health, hunger, xp, fire ticks
                    player.setHealth(pdata.health);
                    player.getFoodData().setFoodLevel(pdata.hunger);
                    player.setExperiencePoints(pdata.xp);
                    player.setRemainingFireTicks(pdata.fireTicks);

                    // Restore inventory
                    player.getInventory().clearContent();
                    for (int i = 0; i < pdata.inventory.size(); i++) {
                        player.getInventory().setItem(i, pdata.inventory.get(i));
                    }
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
                entity.discard();
            }

            // Restore mobs and players from checkpoint data
            List<CompoundTag> entities = data.getEntityData();
            if (entities != null) {
                for (CompoundTag eNBT : entities) {
                    EntityType.loadEntityRecursive(eNBT, level, (entity) -> {
                        if (entity != null) {
                            level.addFreshEntity(entity);
                        }
                        return entity;
                    });
                }
            }

            // Restore items on the ground
            List<CompoundTag> groundItemsList = data.getGroundItems();
            if (groundItemsList != null) {
                for (CompoundTag itemNBT : groundItemsList) {
                    EntityType.loadEntityRecursive(itemNBT, level, (entity) -> {
                        if (entity instanceof ItemEntity) {
                            level.addFreshEntity(entity);
                        }
                        return entity;
                    });
                }
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}