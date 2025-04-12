package boomcow.minezero.checkpoint;

import boomcow.minezero.util.LightningScheduler;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

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
            GameType type = player.gameMode.getGameModeForPlayer();
            pdata.gameMode = type.getName().toLowerCase(Locale.ROOT);

            // Using player.getDeltaMovement() which returns a Vec3
            pdata.motionX = player.getDeltaMovement().x;
            pdata.motionY = player.getDeltaMovement().y;
            pdata.motionZ = player.getDeltaMovement().z;
            pdata.fallDistance = player.fallDistance;

            // Store health, hunger, xp
            pdata.health = player.getHealth();
            pdata.hunger = player.getFoodData().getFoodLevel();
            pdata.experienceLevel = player.experienceLevel;
            pdata.experienceProgress = player.experienceProgress;

            logger.info("Player XP: " + player.totalExperience);
            pdata.fireTicks = player.getRemainingFireTicks();

            BlockPos spawn = player.getRespawnPosition();
            ResourceKey<Level> spawnDim = player.getRespawnDimension();
            boolean forced = player.isRespawnForced();

            if (spawn != null && spawnDim != null) {
                pdata.spawnX = spawn.getX() + 0.5;
                pdata.spawnY = spawn.getY();
                pdata.spawnZ = spawn.getZ() + 0.5;
                pdata.spawnDimension = spawnDim;
                pdata.spawnForced = forced;
            }


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
        Map<UUID, UUID> entityAggroTargets = new HashMap<>();
        for (ServerLevel serverLevel : level.getServer().getAllLevels()) {
            for (Entity entity : serverLevel.getAllEntities()) {
                if (entity instanceof Mob mob) {
                    CompoundTag entityNBT = new CompoundTag();
                    entity.save(entityNBT);

                    if (EntityType.byString(entityNBT.getString("id")).isPresent()) {
                        entityList.add(entityNBT);
                        entityDimensions.add(serverLevel.dimension());

                        // Save aggro target
                        if (mob.getTarget() != null) {
                            entityAggroTargets.put(mob.getUUID(), mob.getTarget().getUUID());
                        }
                    }
                }
            }
        }

        data.setEntityAggroTargets(entityAggroTargets);
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

        long startTime = System.nanoTime();
        try {
            ServerLevel level = anchorPlayer.serverLevel();
            CheckpointData data = CheckpointData.get(level);
            WorldData worldData = data.getWorldData();

            if (data.getPlayerData(data.getAnchorPlayerUUID()) == null) {
                logger.error("Player data is null!");
                return;
            }
            // Restore day time
            level.setDayTime(worldData.getDayTime());

            if (level.getLevelData() instanceof ServerLevelData serverData) {
                serverData.setGameTime(worldData.getGameTime());
                if (serverData instanceof PrimaryLevelData primaryData) {
                    primaryData.setGameTime(worldData.getGameTime());
                }
            }

            if (level.getLevelData() instanceof ServerLevelData serverData) {
                if (serverData instanceof PrimaryLevelData primaryData) {
                    primaryData.setRaining(worldData.isRaining());
                    primaryData.setThundering(worldData.isThundering());
                }

                // Always set clear weather time
                serverData.setClearWeatherTime(worldData.getClearTime());


                if (worldData.isRaining()) {
                    serverData.setRainTime(worldData.getRainTime());
                    level.setRainLevel(1.0F);
                } else {
                    serverData.setRainTime(0);
                    level.setRainLevel(0);
                }

                if (worldData.isThundering()) {
                    serverData.setThunderTime(worldData.getThunderTime());
                    level.setThunderLevel(1.0F);
                } else {
                    serverData.setThunderTime(0);
                    level.setThunderLevel(0);
                }
            }

            for (ServerPlayer player : level.players()) {
                player.connection.send(new ClientboundGameEventPacket(
                        worldData.isRaining() ? ClientboundGameEventPacket.START_RAINING : ClientboundGameEventPacket.STOP_RAINING,
                        0.0F
                ));
                player.connection.send(new ClientboundGameEventPacket(
                        ClientboundGameEventPacket.RAIN_LEVEL_CHANGE,
                        worldData.isRaining() ? 1.0F : 0.0F
                ));

                player.connection.send(new ClientboundGameEventPacket(
                        ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE,
                        worldData.isThundering() ? 1.0F : 0.0F
                ));
            }


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


                if (!WorldData.blockDimensionIndices.containsKey(pos)) {
                    logger.info("No dimension index for modified fluid block at " + pos);
                    continue;
                }

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

                if (!WorldData.blockDimensionIndices.containsKey(pos)) {
                    logger.info("No dimension index for modified fluid block at " + pos);
                    continue;
                }

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
                    player.setExperienceLevels(pdata.experienceLevel);
                    player.experienceProgress = pdata.experienceProgress;  // direct field access


                    player.setRemainingFireTicks(pdata.fireTicks);
                    if (pdata.gameMode != null) {
                        switch (pdata.gameMode.toLowerCase()) {
                            case "survival" -> player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                            case "creative" -> player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
                            case "adventure" -> player.setGameMode(net.minecraft.world.level.GameType.ADVENTURE);
                            case "spectator" -> player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
                        }
                    }


                    if (pdata.spawnDimension != null) {
                        player.setRespawnPosition(
                                pdata.spawnDimension,
                                new BlockPos((int)pdata.spawnX, (int)pdata.spawnY, (int)pdata.spawnZ),
                                pdata.yaw,     // or a fixed value like 0.0f
                                true,
                                false          // update client
                        );
                    }



                    player.setDeltaMovement(new Vec3(pdata.motionX, pdata.motionY, pdata.motionZ));
                    player.fallDistance = pdata.fallDistance;

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

            List<CompoundTag> entities = data.getEntityData();
            List<ResourceKey<Level>> entityDimensions = data.getEntityDimensions();
            Map<UUID, UUID> entityAggroTargets = data.getEntityAggroTargets();
            Map<UUID, Mob> restoredMobs = new HashMap<>();
            for (int i = 0; i < entities.size(); i++) {
                CompoundTag eNBT = entities.get(i);
                ResourceKey<Level> entityDim = entityDimensions.get(i);

                ServerLevel targetLevel = level.getServer().getLevel(entityDim);
                if (targetLevel != null) {
                    EntityType.loadEntityRecursive(eNBT, targetLevel, (entity) -> {
                        if (entity != null) {
                            targetLevel.addFreshEntity(entity);
                            if (entity instanceof Mob mob) {
                                restoredMobs.put(mob.getUUID(), mob);
                            }
                        }
                        return entity;
                    });
                }
            }

            // Reassign aggro targets
            for (Map.Entry<UUID, UUID> entry : entityAggroTargets.entrySet()) {
                UUID mobUUID = entry.getKey();
                UUID targetUUID = entry.getValue();

                if (restoredMobs.containsKey(mobUUID) && restoredMobs.containsKey(targetUUID)) {
                    Mob mob = restoredMobs.get(mobUUID);
                    Entity target = restoredMobs.get(targetUUID);

                    if (target instanceof Mob || target instanceof ServerPlayer) {
                        mob.setTarget((Mob) target);
                    }
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

            List<WorldData.LightningStrike> strikes = worldData.getSavedLightnings();
            for (WorldData.LightningStrike strike : strikes) {

                long delay = strike.tickTime - level.getGameTime(); // adjust if needed
                if (delay < 0) delay = 1;

                LightningScheduler.schedule(level, strike.pos, strike.tickTime);

            }

            // 1. Remove new fire blocks
            for (BlockPos firePos : worldData.getNewFires()) {
                if (level.getBlockState(firePos).getBlock() == Blocks.FIRE) {
                    level.setBlockAndUpdate(firePos, Blocks.AIR.defaultBlockState());
                }
            }

            // 2. Restore blocks destroyed by fire



            long endTime = System.nanoTime();
            long durationMs = (endTime - startTime) / 1_000_000; // Convert to milliseconds
            logger.debug("Restoring states took {} ms", durationMs);


        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            e.printStackTrace();
        }
    }


}