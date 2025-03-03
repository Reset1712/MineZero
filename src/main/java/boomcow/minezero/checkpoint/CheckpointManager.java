package boomcow.minezero.checkpoint;

import boomcow.minezero.ModSoundEvents;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CheckpointManager {


    public static void setCheckpoint(ServerPlayer anchorPlayer) {
        Logger logger = LogManager.getLogger();
        logger.info("Setting checkpoint...");
        long startTime = System.nanoTime();
        ServerLevel level = anchorPlayer.serverLevel();
        CheckpointData data = CheckpointData.get(level);

        // Save world data
        data.saveWorldData(level);

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

            // Store potion effects
            pdata.potionEffects.clear();
            for (MobEffectInstance effect : player.getActiveEffects()) {
                pdata.potionEffects.add(new MobEffectInstance(effect));
            }

            // Store inventory
            pdata.inventory.clear();
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i).copy();
                pdata.inventory.add(stack);
            }

            CompoundTag advTag = new CompoundTag();
            for (Advancement advancement : player.server.getAdvancements().getAllAdvancements()) {
                AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
                // Create a tag to hold progress details.
                CompoundTag progressTag = new CompoundTag();

                // For each criterion, check if it's done.
                for (String criterion : progress.getCompletedCriteria()) {
                    progressTag.putBoolean(criterion, true);
                }

                // Save the progress tag under the advancement's ID.
                advTag.put(advancement.getId().toString(), progressTag);
            }
            pdata.advancements = advTag;

            data.savePlayerData(player.getUUID(), pdata);
        }

        // Save daytime
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
        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000; // Convert to milliseconds
        logger.debug("Saving states took {} ms", durationMs);
        logger.info("Checkpoint set");

    }

    public static void restoreCheckpoint(ServerPlayer anchorPlayer) {
        Logger logger = LogManager.getLogger();
        logger.debug("Restoring checkpoint...");
        logger.debug(" ");
        long startTime = System.nanoTime();
        try {
            ServerLevel level = anchorPlayer.serverLevel();
            CheckpointData data = CheckpointData.get(level);
            WorldData worldData = data.getWorldData();
            // logger.info("checkpoint health" + data.getCheckpointHealth());

//            logger.info("anchor health" + data.getPlayerData(data.getAnchorPlayerUUID()).health);
            if (data.getPlayerData(data.getAnchorPlayerUUID()) == null) {
                logger.error("Player data is null!");
                return;
            }
            // Restore day time
            level.setDayTime(worldData.getDayTime());

            // Restore only modified blocks to air in the correct dimensions
            for (BlockPos pos : WorldData.modifiedBlocks) {
                int dimIndex = WorldData.blockDimensionIndices.get(pos);
                ServerLevel dimLevel = level.getServer().getLevel(WorldData.getDimensionFromIndex(dimIndex));

                if (dimLevel != null) {
                    BlockState currentState = dimLevel.getBlockState(pos);
                    if (!currentState.isAir()) {
                        dimLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }

            // Restore mined blocks in the correct dimensions
            for (Map.Entry<BlockPos, BlockState> entry : WorldData.minedBlocks.entrySet()) {
                BlockPos pos = entry.getKey();
                BlockState originalState = entry.getValue();
                int dimIndex = WorldData.blockDimensionIndices.get(pos);
                ServerLevel dimLevel = level.getServer().getLevel(WorldData.getDimensionFromIndex(dimIndex));

                if (dimLevel != null) {
                    BlockState currentState = dimLevel.getBlockState(pos);
                    if (currentState.isAir()) {
                        dimLevel.setBlock(pos, originalState, 2);
                    }
                }
            }

            // Restore modified fluid blocks to air in the correct dimensions
            for (BlockPos pos : WorldData.modifiedFluidBlocks) {
                logger.debug("Restoring fluid block at " + pos);
                int dimIndex = WorldData.blockDimensionIndices.get(pos);
                ServerLevel dimLevel = level.getServer().getLevel(WorldData.getDimensionFromIndex(dimIndex));
                if (dimLevel != null) {
                    BlockState currentState = dimLevel.getBlockState(pos);
                    // If a fluid (or any block) is present, clear it by setting air.
                    if (!currentState.isAir()) {
                        dimLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }

// Restore mined fluid blocks (i.e. fluid that was removed) in the correct dimensions
            for (Map.Entry<BlockPos, BlockState> entry : WorldData.minedFluidBlocks.entrySet()) {
                BlockPos pos = entry.getKey();
                BlockState originalState = entry.getValue();
                int dimIndex = WorldData.blockDimensionIndices.get(pos);
                ServerLevel dimLevel = level.getServer().getLevel(WorldData.getDimensionFromIndex(dimIndex));
                if (dimLevel != null) {
                    BlockState currentState = dimLevel.getBlockState(pos);
                    // If the current block is air (fluid was removed), restore the original fluid state.
                    if (currentState.isAir()) {
                        dimLevel.setBlock(pos, originalState, 3);
                    }
                }
            }


            int totalSaved = 0;
            int updateCount = 0;

            // Assume you have a WorldData instance called worldData.
            Map<ChunkPos, List<WorldData.SavedBlock>> savedBlocksByChunk = worldData.getSavedBlocksByChunk();

            for (Map.Entry<ChunkPos, List<WorldData.SavedBlock>> entry : savedBlocksByChunk.entrySet()) {
                List<WorldData.SavedBlock> savedBlocks = entry.getValue();
                totalSaved += savedBlocks.size();

                // We assume all saved blocks in this list are from the same dimension.
                ResourceKey<Level> dimension = savedBlocks.get(0).dimension();
                ServerLevel dimLevel = level.getServer().getLevel(dimension);

                if (dimLevel == null) continue;

                for (WorldData.SavedBlock saved : savedBlocks) {
                    BlockState currentState = dimLevel.getBlockState(saved.pos());
                    if (!currentState.getBlock().equals(saved.state().getBlock())) {
                        updateCount++;
                        dimLevel.setBlock(saved.pos(), saved.state(), 2); // Use flag 2 to minimize neighbor updates.
                    }
                }
            }

//            logger.debug("Total saved block states (chunked): {}", totalSaved);
//            logger.debug("Performed {} block updates.", updateCount);

            // Restore block entities in the correct dimensions
            for (Map.Entry<BlockPos, CompoundTag> entry : worldData.getBlockEntityData().entrySet()) {
                BlockPos pos = entry.getKey();
                int dimIndex = WorldData.blockDimensionIndices.get(pos);
                ServerLevel dimLevel = level.getServer().getLevel(WorldData.getDimensionFromIndex(dimIndex));

                if (dimLevel != null) {
                    BlockEntity blockEntity = dimLevel.getBlockEntity(pos);
                    if (blockEntity != null) {
                        blockEntity.load(entry.getValue());
                        blockEntity.setChanged();
                    }
                }
            }


            // Restore all players from their saved data
            for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
                PlayerData pdata = data.getPlayerData(player.getUUID());
                if (pdata != null) {
                    // Switch player to the correct dimension
//                    logger.info("Player dimension: " + player.level().dimension() + ", Checkpoint dimension: " + pdata.dimension);
                    if (!player.level().dimension().equals(pdata.dimension)) {
                        ServerLevel targetLevel = player.getServer().getLevel(pdata.dimension);
                        if (targetLevel != null) {
                            player.setHealth(pdata.health);
                            player.teleportTo(targetLevel, pdata.posX, pdata.posY, pdata.posZ, pdata.yaw, pdata.pitch);
                        }
                    } else {
                        // Restore position within the same dimension
                        player.setHealth(pdata.health);
                        ServerLevel targetLevel = player.getServer().getLevel(pdata.dimension);
                        player.teleportTo(targetLevel, pdata.posX, pdata.posY, pdata.posZ, pdata.yaw, pdata.pitch);

                    }

                    // Restore health, hunger, xp, fire ticks

                    player.getFoodData().setFoodLevel(pdata.hunger);
                    player.setExperiencePoints(pdata.xp);
                    player.setRemainingFireTicks(pdata.fireTicks);
//                    logger.info("remaining fire ticks: " + pdata.fireTicks);

                    // Restore potion effects
                    player.removeAllEffects();
                    for (MobEffectInstance effect : pdata.potionEffects) {
                        player.addEffect(new MobEffectInstance(effect));
                    }

                    // Restore advancements exactly as saved in the checkpoint.
                    CompoundTag savedAdvTag = pdata.advancements;
                    ServerAdvancementManager advManager = level.getServer().getAdvancements();

                    for (Advancement advancement : advManager.getAllAdvancements()) {
                        AdvancementProgress currentProgress = player.getAdvancements().getOrStartProgress(advancement);
                        CompoundTag savedProgressTag = null;
                        String advKey = advancement.getId().toString();
                        if (savedAdvTag.contains(advKey)) {
                            savedProgressTag = savedAdvTag.getCompound(advKey);
                        }

                        for (String criterion : advancement.getCriteria().keySet()) {
                            boolean wasCompleted = savedProgressTag != null && savedProgressTag.getBoolean(criterion);
                            boolean isCompleted = false;
                            for (String compCriterion : currentProgress.getCompletedCriteria()) {
                                if (compCriterion.equals(criterion)) {
                                    isCompleted = true;
                                    break;
                                }
                            }

                            if (isCompleted && !wasCompleted) {
                                // Revoke criteria gained after checkpoint.
                                player.getAdvancements().revoke(advancement, criterion);
                            } else if (!isCompleted && wasCompleted) {
                                // Award criteria that were saved in the checkpoint.
                                player.getAdvancements().award(advancement, criterion);
                            }
                        }
                    }

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
//            logger.info("Restoring entities...");
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
            long endTime = System.nanoTime();
            long durationMs = (endTime - startTime) / 1_000_000; // Convert to milliseconds
            logger.debug("Restoring states took {} ms", durationMs);
            logger.info("Checkpoint restored");

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            e.printStackTrace();
        }
    }


}