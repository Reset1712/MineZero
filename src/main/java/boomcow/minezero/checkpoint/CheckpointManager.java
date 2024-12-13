package boomcow.minezero.checkpoint;

import boomcow.minezero.ModSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

public class CheckpointManager {

    public static void setCheckpoint(ServerPlayer anchorPlayer) {
        Logger logger = LogManager.getLogger();
        logger.info("Setting checkpoint...");
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
            pdata.dimension = player.level().dimension(); // Save dimension

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
        List<ResourceKey<Level>> entityDimensions = new ArrayList<>();
        for (ServerLevel serverLevel : level.getServer().getAllLevels()) {
            for (Entity entity : serverLevel.getAllEntities()) {
                if (entity instanceof Mob || entity instanceof ServerPlayer) {
                    CompoundTag entityNBT = new CompoundTag();
                    entity.save(entityNBT);

                    if (EntityType.byString(entityNBT.getString("id")).isPresent()) {
                        entityList.add(entityNBT);
                        entityDimensions.add(serverLevel.dimension()); // Save the dimension
                    }
                }
            }
        }

        data.setEntityDataWithDimensions(entityList, entityDimensions);

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
        Logger logger = LogManager.getLogger();
        logger.debug("Restoring checkpoint...");
        try {
            ServerLevel level = anchorPlayer.serverLevel();
            CheckpointData data = CheckpointData.get(level);
            logger.info("checkpoint health" + data.getCheckpointHealth());

            logger.info("anchor health" + data.getPlayerData(data.getAnchorPlayerUUID()).health);
            if (data.getPlayerData(data.getAnchorPlayerUUID()) == null) {
                logger.error("Player data is null!");
                return;
            }
            // Restore day time
            level.setDayTime(data.getCheckpointDayTime());

            // Restore all players from their saved data
            for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
                PlayerData pdata = data.getPlayerData(player.getUUID());
                if (pdata != null) {
                    // Switch player to the correct dimension
                    logger.info("Player dimension: " + player.level().dimension() + ", Checkpoint dimension: " + pdata.dimension);
                    if (!player.level().dimension().equals(pdata.dimension)) {
                        ServerLevel targetLevel = player.getServer().getLevel(pdata.dimension);
                        if (targetLevel != null) {
                            player.setHealth(pdata.health);
                            player.teleportTo(targetLevel, pdata.posX, pdata.posY, pdata.posZ, pdata.yaw, pdata.pitch);
                            targetLevel.playSound(
                                    null,
                                    player.blockPosition(),
                                    ModSoundEvents.DEATH_CHIME.get(),
                                    SoundSource.PLAYERS,
                                    0.8F,
                                    1.0F
                            );
                        }
                    } else {
                        // Restore position within the same dimension
                        player.setHealth(pdata.health);
                        ServerLevel targetLevel = player.getServer().getLevel(pdata.dimension);
                        player.teleportTo(targetLevel, pdata.posX, pdata.posY, pdata.posZ, pdata.yaw, pdata.pitch);
                        level.playSound(
                                null,
                                player.blockPosition(),
                                ModSoundEvents.DEATH_CHIME.get(),
                                SoundSource.PLAYERS,
                                0.8F,
                                1.0F
                        );
                    }

                    // Restore health, hunger, xp, fire ticks

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

            // Remove all non-player entities across all dimensions
            List<Entity> entitiesToRemove = new ArrayList<>();

            // Iterate over all loaded dimensions
            for (ServerLevel serverLevel : level.getServer().getAllLevels()) {
                // Collect non-player entities to remove
                for (Entity entity : serverLevel.getAllEntities()) {
                    if (!(entity instanceof ServerPlayer) && !entity.isRemoved()) {
                        entitiesToRemove.add(entity);
                    }
                }

                // Remove collected entities in this dimension
                for (Entity entity : entitiesToRemove) {
                    entity.discard(); // Safely discard the entity
                }

                // Clear the list for the next dimension
                entitiesToRemove.clear();
            }

            // Restore mobs and players from checkpoint data
            logger.info("Restoring entities...");
            List<CompoundTag> entities = data.getEntityData();
            List<ResourceKey<Level>> entityDimensions = data.getEntityDimensions();

            for (int i = 0; i < entities.size(); i++) {
                CompoundTag eNBT = entities.get(i);
                ResourceKey<Level> entityDim = entityDimensions.get(i);

                ServerLevel targetLevel = level.getServer().getLevel(entityDim);
                if (targetLevel != null) {
                    EntityType.loadEntityRecursive(eNBT, targetLevel, (entity) -> {
                        if (entity != null) {
                            targetLevel.addFreshEntity(entity);
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
            logger.error(e.getMessage(), e);
            e.printStackTrace();
        }
    }


}