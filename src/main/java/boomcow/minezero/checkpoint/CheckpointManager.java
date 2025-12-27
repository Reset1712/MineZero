package boomcow.minezero.checkpoint;

import boomcow.minezero.util.LightningScheduler;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementEntry;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.EndPortalFrameBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.*;
import net.minecraft.entity.decoration.AbstractDecorationEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.GameStateChangeS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.ServerAdvancementLoader;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class CheckpointManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("MineZeroManager");
    public static boolean isRestoring = false;
    public static boolean wasRestoredThisTick = false;

    public static void setCheckpoint(ServerPlayerEntity anchorPlayer) {
        LOGGER.info("Setting checkpoint...");
        long startTime = System.nanoTime();
        ServerWorld world = anchorPlayer.getServerWorld();
        CheckpointData data = CheckpointData.get(world);
        RegistryWrapper.WrapperLookup lookupProvider = world.getRegistryManager();
        
        data.saveWorldData(world);
        data.setAnchorPlayerUUID(anchorPlayer.getUuid());

        for (ServerPlayerEntity player : world.getServer().getPlayerManager().getPlayerList()) {
            PlayerData pdataObject = new PlayerData();
            pdataObject.posX = player.getX();
            pdataObject.posY = player.getY();
            pdataObject.posZ = player.getZ();
            pdataObject.yaw = player.getYaw();
            pdataObject.pitch = player.getPitch();
            pdataObject.dimension = player.getWorld().getRegistryKey();
            GameMode type = player.interactionManager.getGameMode();
            pdataObject.gameMode = type.getName().toLowerCase(Locale.ROOT);

            Vec3d velocity = player.getVelocity();
            pdataObject.motionX = velocity.x;
            pdataObject.motionY = velocity.y;
            pdataObject.motionZ = velocity.z;
            pdataObject.fallDistance = player.fallDistance;

            pdataObject.health = player.getHealth();
            pdataObject.hunger = player.getHungerManager().getFoodLevel();
            pdataObject.experienceLevel = player.experienceLevel;
            pdataObject.experienceProgress = player.experienceProgress;
            pdataObject.fireTicks = player.getFireTicks();

            BlockPos spawn = player.getSpawnPointPosition();
            RegistryKey<World> spawnDim = player.getSpawnPointDimension();
            if (spawn != null && spawnDim != null) {
                pdataObject.spawnX = spawn.getX() + 0.5;
                pdataObject.spawnY = spawn.getY();
                pdataObject.spawnZ = spawn.getZ() + 0.5;
                pdataObject.spawnDimension = spawnDim;
                pdataObject.spawnForced = player.isSpawnForced();
            }

            pdataObject.potionEffects.clear();
            for (StatusEffectInstance effect : player.getStatusEffects()) {
                pdataObject.potionEffects.add(new StatusEffectInstance(effect));
            }

            pdataObject.inventory.clear();
            for (int i = 0; i < player.getInventory().size(); i++) {
                pdataObject.inventory.add(player.getInventory().getStack(i).copy());
            }

            NbtCompound capturedAdvancementsNBT = new NbtCompound();
            if (player.server != null) {
                for (AdvancementEntry advancementHolder : player.server.getAdvancementLoader().getAdvancements()) {
                    AdvancementProgress progress = player.getAdvancementTracker().getProgress(advancementHolder);
                    NbtCompound individualAdvancementProgressTag = new NbtCompound();
                    if (progress != null) {
                        for (String criterionName : progress.getObtainedCriteria()) {
                            individualAdvancementProgressTag.putBoolean(criterionName, true);
                        }
                    }
                    if (!individualAdvancementProgressTag.isEmpty()) {
                        capturedAdvancementsNBT.put(advancementHolder.id().toString(), individualAdvancementProgressTag);
                    }
                }
            }
            pdataObject.advancements = capturedAdvancementsNBT;
            NbtCompound playerDataNbt = pdataObject.toNBT(lookupProvider);
            data.savePlayerData(player.getUuid(), playerDataNbt);
        }
        data.setCheckpointDayTime(world.getTimeOfDay());

        List<NbtCompound> entityList = new ArrayList<>();
        List<RegistryKey<World>> entityDimensions = new ArrayList<>();
        Map<UUID, UUID> entityAggroTargets = new HashMap<>();

        for (ServerWorld serverWorld : world.getServer().getWorlds()) {
            for (Entity entity : serverWorld.iterateEntities()) {
                if (entity instanceof MobEntity mob) {
                    NbtCompound entityNBT = new NbtCompound();
                    if (entity.saveNbt(entityNBT)) {
                        entityList.add(entityNBT);
                        entityDimensions.add(serverWorld.getRegistryKey());
                        if (mob.getTarget() != null) {
                            entityAggroTargets.put(mob.getUuid(), mob.getTarget().getUuid());
                        }
                    }
                }
            }
        }
        for (ServerWorld serverWorld : world.getServer().getWorlds()) {
            for (Entity entity : serverWorld.iterateEntities()) {
                if (entity instanceof MobEntity) continue;
                if (entity instanceof ServerPlayerEntity) continue;

                if (entity instanceof AbstractMinecartEntity ||
                        entity instanceof BoatEntity ||
                        entity instanceof AbstractDecorationEntity ||
                        entity instanceof ArmorStandEntity ||
                        entity instanceof ExperienceOrbEntity || 
                        entity instanceof EyeOfEnderEntity ||
                        entity instanceof AreaEffectCloudEntity ||
                        entity instanceof EvokerFangsEntity ||
                        entity instanceof MarkerEntity ||
                        entity instanceof TntEntity ||
                        entity instanceof FallingBlockEntity ||
                        entity instanceof ProjectileEntity ||
                        entity instanceof ItemEntity) {

                    NbtCompound entityNBT = new NbtCompound();
                    if (entity.saveNbt(entityNBT)) {
                        entityList.add(entityNBT);
                        entityDimensions.add(serverWorld.getRegistryKey());
                    }
                }
            }
        }

        data.setEntityAggroTargets(entityAggroTargets);
        data.setEntityDataWithDimensions(entityList, entityDimensions);
        List<NbtCompound> groundItemsList = new ArrayList<>();
        for (Entity entity : world.iterateEntities()) {
            if (entity instanceof ItemEntity itemEntity) {
                NbtCompound itemNBT = new NbtCompound();
                itemEntity.saveNbt(itemNBT);
                groundItemsList.add(itemNBT);
            }
        }
        data.setGroundItems(groundItemsList);
        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;
        LOGGER.debug("Saving states took {} ms", durationMs);
        LOGGER.info("Checkpoint set");
    }

    public static void restoreCheckpoint(ServerPlayerEntity anchorPlayer) {
        isRestoring = true;
        wasRestoredThisTick = true;
        LOGGER.debug("Restoring checkpoint...");

        long startTime = System.nanoTime();
        try {
            ServerWorld world = anchorPlayer.getServerWorld();
            CheckpointData data = CheckpointData.get(world);
            WorldData worldData = data.getWorldData();
            RegistryWrapper.WrapperLookup lookupProvider = world.getRegistryManager();

            if (data.getPlayerData(data.getAnchorPlayerUUID(), lookupProvider) == null) {
                LOGGER.error("Player data is null!");
                isRestoring = false;
                return;
            }
            world.setTimeOfDay(worldData.getDayTime());
            
            ServerWorld overworld = world.getServer().getOverworld();
            overworld.getLevelProperties().setTime(worldData.getGameTime());
            overworld.getLevelProperties().setRaining(worldData.isRaining());
            overworld.getLevelProperties().setThundering(worldData.isThundering());
            overworld.getLevelProperties().setClearWeatherTime(worldData.getClearTime());

            if (worldData.isRaining()) {
                overworld.setRainGradient(1.0F);
                overworld.getLevelProperties().setRainTime(worldData.getRainTime());
            } else {
                overworld.setRainGradient(0.0F);
                overworld.getLevelProperties().setRainTime(0);
            }

            if (worldData.isThundering()) {
                overworld.setThunderGradient(1.0F);
                overworld.getLevelProperties().setThunderTime(worldData.getThunderTime());
            } else {
                overworld.setThunderGradient(0.0F);
                overworld.getLevelProperties().setThunderTime(0);
            }

            for (ServerPlayerEntity player : world.getPlayers()) {
                 player.networkHandler.sendPacket(new GameStateChangeS2CPacket(
                         worldData.isRaining() ? GameStateChangeS2CPacket.RAIN_STARTED : GameStateChangeS2CPacket.RAIN_STOPPED, 0.0F
                 ));
                 player.networkHandler.sendPacket(new GameStateChangeS2CPacket(
                         GameStateChangeS2CPacket.RAIN_GRADIENT_CHANGED, worldData.isRaining() ? 1.0F : 0.0F
                 ));
                 player.networkHandler.sendPacket(new GameStateChangeS2CPacket(
                         GameStateChangeS2CPacket.THUNDER_GRADIENT_CHANGED, worldData.isThundering() ? 1.0F : 0.0F
                 ));
            }

            for (BlockPos pos : worldData.modifiedBlocks) {
                int dimIndex = worldData.blockDimensionIndices.get(pos);
                ServerWorld dimLevel = world.getServer().getWorld(WorldData.getDimensionFromIndex(dimIndex));

                if (dimLevel != null) {
                    BlockState currentState = dimLevel.getBlockState(pos);
                    if (!currentState.isAir()) {
                        dimLevel.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
                    }
                }
            }
            for (Map.Entry<BlockPos, BlockState> entry : worldData.minedBlocks.entrySet()) {
                BlockPos pos = entry.getKey();
                BlockState originalState = entry.getValue();
                int dimIndex = worldData.blockDimensionIndices.get(pos);
                ServerWorld dimLevel = world.getServer().getWorld(WorldData.getDimensionFromIndex(dimIndex));

                if (dimLevel != null) {
                    BlockState currentState = dimLevel.getBlockState(pos);
                    if (currentState.isAir()) {
                        dimLevel.setBlockState(pos, originalState, 2);
                    }
                }
            }

            for (BlockPos pos : worldData.modifiedFluidBlocks) {
                if (!worldData.blockDimensionIndices.containsKey(pos)) continue;
                int dimIndex = worldData.blockDimensionIndices.get(pos);
                ServerWorld dimLevel = world.getServer().getWorld(WorldData.getDimensionFromIndex(dimIndex));
                if (dimLevel != null) {
                    dimLevel.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
                }
            }
            for (Map.Entry<BlockPos, BlockState> entry : worldData.minedFluidBlocks.entrySet()) {
                BlockPos pos = entry.getKey();
                if (!worldData.blockDimensionIndices.containsKey(pos)) continue;
                int dimIndex = worldData.blockDimensionIndices.get(pos);
                ServerWorld dimLevel = world.getServer().getWorld(WorldData.getDimensionFromIndex(dimIndex));
                if (dimLevel != null) {
                    dimLevel.setBlockState(pos, entry.getValue(), 3);
                }
            }

            Map<ChunkPos, List<WorldData.SavedBlock>> savedBlocksByChunk = worldData.getSavedBlocksByChunk();
            for (Map.Entry<ChunkPos, List<WorldData.SavedBlock>> entry : savedBlocksByChunk.entrySet()) {
                List<WorldData.SavedBlock> savedBlocks = entry.getValue();
                RegistryKey<World> dimension = savedBlocks.get(0).dimension();
                ServerWorld dimLevel = world.getServer().getWorld(dimension);

                if (dimLevel == null) continue;

                for (WorldData.SavedBlock saved : savedBlocks) {
                    BlockState currentState = dimLevel.getBlockState(saved.pos());
                    if (!currentState.getBlock().equals(saved.state().getBlock())) {
                        dimLevel.setBlockState(saved.pos(), saved.state(), 2);
                    }
                }
            }

            for (Map.Entry<BlockPos, NbtCompound> entry : worldData.getBlockEntityData().entrySet()) {
                BlockPos pos = entry.getKey();
                NbtCompound blockEntityNbt = entry.getValue();

                Integer dimIndexInt = worldData.getInstanceBlockDimensionIndices().get(pos);
                if (dimIndexInt == null) continue;
                
                ServerWorld dimLevel = world.getServer().getWorld(WorldData.getDimensionFromIndex(dimIndexInt));

                if (dimLevel != null) {
                    BlockEntity blockEntity = dimLevel.getBlockEntity(pos);
                    if (blockEntity != null) {
                        blockEntity.read(blockEntityNbt, lookupProvider);
                        blockEntity.markDirty();
                        dimLevel.updateListeners(pos, blockEntity.getCachedState(), blockEntity.getCachedState(), 3);
                    }
                }
            }

            for (ServerPlayerEntity player : world.getServer().getPlayerManager().getPlayerList()) {
                PlayerData pdata = data.getPlayerData(player.getUuid(), lookupProvider);
                if (pdata != null) {
                    ServerWorld targetLevel = player.getServer().getWorld(pdata.dimension);
                    if (targetLevel != null) {
                        player.teleport(targetLevel, pdata.posX, pdata.posY, pdata.posZ, Set.of(), pdata.yaw, pdata.pitch);
                    }
                    
                    player.setHealth(pdata.health);
                    player.getHungerManager().setFoodLevel(pdata.hunger);
                    player.setExperienceLevel(pdata.experienceLevel);
                    player.experienceProgress = pdata.experienceProgress;
                    player.setFireTicks(pdata.fireTicks);

                    if (pdata.gameMode != null) {
                        switch (pdata.gameMode.toLowerCase()) {
                            case "survival" -> player.changeGameMode(GameMode.SURVIVAL);
                            case "creative" -> player.changeGameMode(GameMode.CREATIVE);
                            case "adventure" -> player.changeGameMode(GameMode.ADVENTURE);
                            case "spectator" -> player.changeGameMode(GameMode.SPECTATOR);
                        }
                    }

                    if (pdata.spawnDimension != null) {
                        player.setSpawnPoint(
                                pdata.spawnDimension,
                                new BlockPos((int)pdata.spawnX, (int)pdata.spawnY, (int)pdata.spawnZ),
                                pdata.yaw,
                                true,
                                false
                        );
                    }

                    player.setVelocity(new Vec3d(pdata.motionX, pdata.motionY, pdata.motionZ));
                    player.fallDistance = pdata.fallDistance;
                    player.clearStatusEffects();
                    for (StatusEffectInstance effect : pdata.potionEffects) {
                        player.addStatusEffect(new StatusEffectInstance(effect));
                    }

                    NbtCompound savedAdvTag = pdata.advancements;
                    ServerAdvancementLoader advManager = world.getServer().getAdvancementLoader();
                    for (AdvancementEntry advancementHolder : advManager.getAdvancements()) {
                        AdvancementProgress currentProgress = player.getAdvancementTracker().getProgress(advancementHolder);

                        NbtCompound savedProgressTagForThisAdv = null;
                        String advKey = advancementHolder.id().toString();
                        if (savedAdvTag.contains(advKey)) {
                            savedProgressTagForThisAdv = savedAdvTag.getCompound(advKey);
                        }
                        Advancement advancementInstance = advancementHolder.value();
                        for (String criterionName : advancementInstance.criteria().keySet()) {
                            boolean wasCompletedInSave = savedProgressTagForThisAdv != null && savedProgressTagForThisAdv.getBoolean(criterionName);
                            boolean isCurrentlyCompleted = currentProgress.getCriterionProgress(criterionName) != null && currentProgress.getCriterionProgress(criterionName).isObtained();

                            if (isCurrentlyCompleted && !wasCompletedInSave) {
                                player.getAdvancementTracker().revokeCriterion(advancementHolder, criterionName);
                            } else if (!isCurrentlyCompleted && wasCompletedInSave) {
                                player.getAdvancementTracker().grantCriterion(advancementHolder, criterionName);
                            }
                        }
                    }
                    player.getInventory().clear();
                    for (int i = 0; i < pdata.inventory.size(); i++) {
                        player.getInventory().setStack(i, pdata.inventory.get(i));
                    }
                }
            }

            List<Entity> entitiesToRemove = new ArrayList<>();
            for (ServerWorld serverWorld : world.getServer().getWorlds()) {
                for (Entity entity : serverWorld.iterateEntities()) {
                    if (!(entity instanceof ServerPlayerEntity) && !entity.isRemoved()) {
                        entitiesToRemove.add(entity);
                    }
                }
                for (Entity entity : entitiesToRemove) {
                    entity.discard();
                }
                entitiesToRemove.clear();
            }

            List<NbtCompound> entities = data.getEntityData();
            List<RegistryKey<World>> entityDimensions = data.getEntityDimensions();
            Map<UUID, UUID> entityAggroTargets = data.getEntityAggroTargets();
            Map<UUID, MobEntity> restoredMobs = new HashMap<>();

            for (int i = 0; i < entities.size(); i++) {
                NbtCompound eNBT = entities.get(i);
                RegistryKey<World> entityDim = entityDimensions.get(i);

                ServerWorld targetLevel = world.getServer().getWorld(entityDim);
                if (targetLevel != null) {
                    EntityType.loadEntityWithPassengers(eNBT, targetLevel, (entity) -> {
                        if (entity != null) {
                            targetLevel.spawnEntity(entity);
                            if (entity instanceof MobEntity mob) {
                                restoredMobs.put(mob.getUuid(), mob);
                            }
                        }
                        return entity;
                    });
                }
            }
            for (Map.Entry<UUID, UUID> entry : entityAggroTargets.entrySet()) {
                UUID mobUUID = entry.getKey();
                UUID targetUUID = entry.getValue();

                if (restoredMobs.containsKey(mobUUID) && restoredMobs.containsKey(targetUUID)) {
                    MobEntity mob = restoredMobs.get(mobUUID);
                    Entity target = restoredMobs.get(targetUUID);

                    if (target instanceof LivingEntity livingTarget) {
                        mob.setTarget(livingTarget);
                    }
                }
            }
            List<NbtCompound> groundItemsList = data.getGroundItems();
            if (groundItemsList != null) {
                for (NbtCompound itemNBT : groundItemsList) {
                    EntityType.loadEntityWithPassengers(itemNBT, world, (entity) -> {
                        if (entity instanceof ItemEntity) {
                            world.spawnEntity(entity);
                        }
                        return entity;
                    });
                }
            }

            for (BlockPos eyePos : worldData.addedEyes) {
                BlockState state = world.getBlockState(eyePos);
                if (state.getBlock() == Blocks.END_PORTAL_FRAME && state.get(EndPortalFrameBlock.EYE)) {
                    world.setBlockState(eyePos, state.with(EndPortalFrameBlock.EYE, false), 3);
                }
            }

            List<WorldData.LightningStrike> strikes = worldData.getSavedLightnings();
            for (WorldData.LightningStrike strike : strikes) {
                LightningScheduler.schedule(world, strike.pos, strike.tickTime);
            }
            for (BlockPos firePos : worldData.getNewFires()) {
                if (world.getBlockState(firePos).getBlock() == Blocks.FIRE) {
                    world.setBlockState(firePos, Blocks.AIR.getDefaultState());
                }
            }

            long endTime = System.nanoTime();
            long durationMs = (endTime - startTime) / 1_000_000;
            LOGGER.debug("Restoring states took {} ms", durationMs);

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            e.printStackTrace();
        } finally {
            isRestoring = false;
        }
    }
}
