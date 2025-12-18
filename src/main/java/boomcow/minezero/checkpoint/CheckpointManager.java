package boomcow.minezero.checkpoint;

import boomcow.minezero.util.LightningScheduler;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.projectile.EyeOfEnder;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EndPortalFrameBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.function.Function;

public class CheckpointManager {

    private static final Logger LOGGER = LogManager.getLogger();
    public static boolean isRestoring = false;
    public static boolean wasRestoredThisTick = false;

    // --- SET CHECKPOINT ---
    public static void setCheckpoint(ServerPlayer anchorPlayer) {
        Logger logger = LogManager.getLogger();
        logger.info("Setting checkpoint...");
        long startTime = System.nanoTime();
        ServerLevel level = anchorPlayer.serverLevel();
        CheckpointData data = CheckpointData.get(level);

        // 1. Reset previous data
        data.clearAllEntityData();
        data.saveWorldData(level);
        data.setAnchorPlayerUUID(anchorPlayer.getUUID());
        data.setCheckpointDayTime(level.getDayTime());

        // 2. Save Players
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            saveSinglePlayer(data, player);
        }

        // 3. Save Entities & Ground Items
        saveEntities(level, data);

        long endTime = System.nanoTime();
        logger.debug("Checkpoint set in {} ms", (endTime - startTime) / 1_000_000);
    }

    // --- RESTORE CHECKPOINT ---
    public static void restoreCheckpoint(ServerPlayer anchorPlayer) {
        isRestoring = true;
        wasRestoredThisTick = true;
        Logger logger = LogManager.getLogger();
        logger.debug("Restoring checkpoint...");
        long startTime = System.nanoTime();

        try {
            ServerLevel level = anchorPlayer.serverLevel();
            CheckpointData data = CheckpointData.get(level);
            
            if (data.getPlayerData(data.getAnchorPlayerUUID()) == null) {
                logger.error("Anchor player data is missing. Cannot restore.");
                return;
            }

            // 1. World Environment (Time, Weather)
            restoreTimeAndWeather(level, data.getWorldData());

            // 2. Blocks & Fluids (The heavy lifting)
            restoreBlocksAndFluids(level, data.getWorldData());

            // 3. Players
            restorePlayers(level, data);

            // 4. Entities (Wipe & Restore)
            restoreEntities(level, data);

            // 5. Extras (Events, Lightning, Fire)
            restoreWorldExtras(level, data.getWorldData());

            long endTime = System.nanoTime();
            logger.debug("Checkpoint restored in {} ms", (endTime - startTime) / 1_000_000);

        } catch (Exception e) {
            logger.error("Critical error restoring checkpoint", e);
        } finally {
            isRestoring = false;
        }
    }

    // ============================================================================================
    //                                     INTERNAL HELPERS
    // ============================================================================================

    private static void saveSinglePlayer(CheckpointData data, ServerPlayer player) {
        PlayerData pdata = new PlayerData();
        pdata.posX = player.getX();
        pdata.posY = player.getY();
        pdata.posZ = player.getZ();
        pdata.yaw = player.getYRot();
        pdata.pitch = player.getXRot();
        pdata.dimension = player.level().dimension();
        pdata.gameMode = player.gameMode.getGameModeForPlayer().getName().toLowerCase(Locale.ROOT);
        pdata.motionX = player.getDeltaMovement().x;
        pdata.motionY = player.getDeltaMovement().y;
        pdata.motionZ = player.getDeltaMovement().z;
        pdata.fallDistance = player.fallDistance;
        pdata.health = player.getHealth();
        pdata.hunger = player.getFoodData().getFoodLevel();
        pdata.experienceLevel = player.experienceLevel;
        pdata.experienceProgress = player.experienceProgress;
        pdata.fireTicks = player.getRemainingFireTicks();

        BlockPos spawn = player.getRespawnPosition();
        ResourceKey<Level> spawnDim = player.getRespawnDimension();
        if (spawn != null && spawnDim != null) {
            pdata.spawnX = spawn.getX() + 0.5;
            pdata.spawnY = spawn.getY();
            pdata.spawnZ = spawn.getZ() + 0.5;
            pdata.spawnDimension = spawnDim;
            pdata.spawnForced = player.isRespawnForced();
        }

        pdata.potionEffects.clear();
        for (MobEffectInstance effect : player.getActiveEffects()) {
            pdata.potionEffects.add(new MobEffectInstance(effect));
        }

        pdata.inventory.clear();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            pdata.inventory.add(player.getInventory().getItem(i).copy());
        }

        CompoundTag advTag = new CompoundTag();
        for (Advancement advancement : player.server.getAdvancements().getAllAdvancements()) {
            AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
            CompoundTag progressTag = new CompoundTag();
            for (String criterion : progress.getCompletedCriteria()) {
                progressTag.putBoolean(criterion, true);
            }
            advTag.put(advancement.getId().toString(), progressTag);
        }
        pdata.advancements = advTag;

        data.savePlayerData(player.getUUID(), pdata);
    }

    private static void saveEntities(ServerLevel level, CheckpointData data) {
        List<CompoundTag> entityList = new ArrayList<>();
        List<ResourceKey<Level>> entityDimensions = new ArrayList<>();
        Map<UUID, UUID> entityAggroTargets = new HashMap<>();
        List<CompoundTag> groundItemsList = new ArrayList<>();

        for (ServerLevel serverLevel : level.getServer().getAllLevels()) {
            for (Entity entity : serverLevel.getAllEntities()) {
                if (entity instanceof ServerPlayer) continue;
                
                // Skip transient danger entities
                if (entity instanceof PrimedTnt || entity instanceof LightningBolt || entity instanceof Projectile) continue;

                if (entity instanceof ItemEntity) {
                    CompoundTag itemNBT = new CompoundTag();
                    if (entity.save(itemNBT)) groundItemsList.add(itemNBT);
                    continue; 
                }

                // General Entities
                if (shouldSaveEntity(entity)) {
                    CompoundTag entityNBT = new CompoundTag();
                    if (entity.save(entityNBT)) {
                        entityList.add(entityNBT);
                        entityDimensions.add(serverLevel.dimension());
                        if (entity instanceof Mob mob && mob.getTarget() != null) {
                            entityAggroTargets.put(mob.getUUID(), mob.getTarget().getUUID());
                        }
                    }
                }
            }
        }

        data.setEntityAggroTargets(entityAggroTargets);
        data.setEntityDataWithDimensions(entityList, entityDimensions);
        data.setGroundItems(groundItemsList);
    }

    private static boolean shouldSaveEntity(Entity entity) {
        return entity instanceof Mob ||
               entity instanceof AbstractMinecart ||
               entity instanceof AreaEffectCloud ||
               entity instanceof Boat ||
               entity instanceof EndCrystal ||
               entity instanceof EvokerFangs ||
               entity instanceof ExperienceOrb ||
               entity instanceof EyeOfEnder ||
               entity instanceof FallingBlockEntity ||
               entity instanceof HangingEntity ||
               entity instanceof Marker ||
               entity instanceof PartEntity ||
               entity instanceof ArmorStand;
    }

    private static void restoreTimeAndWeather(ServerLevel level, WorldData worldData) {
        level.setDayTime(worldData.getDayTime());
        
        if (level.getLevelData() instanceof ServerLevelData serverData) {
            serverData.setGameTime(worldData.getGameTime());
            if (serverData instanceof PrimaryLevelData primaryData) {
                primaryData.setGameTime(worldData.getGameTime());
            }
            
            serverData.setClearWeatherTime(worldData.getClearTime());
            if (level.getLevelData() instanceof PrimaryLevelData primaryData) {
                primaryData.setRaining(worldData.isRaining());
                primaryData.setThundering(worldData.isThundering());
            }
            
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

        // Sync packets
        for (ServerPlayer player : level.players()) {
            player.connection.send(new ClientboundGameEventPacket(
                    worldData.isRaining() ? ClientboundGameEventPacket.START_RAINING : ClientboundGameEventPacket.STOP_RAINING, 0.0F));
            player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, worldData.isRaining() ? 1.0F : 0.0F));
            player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, worldData.isThundering() ? 1.0F : 0.0F));
        }
    }

    private static void restoreBlocksAndFluids(ServerLevel rootLevel, WorldData worldData) {
        Map<ResourceKey<Level>, ServerLevel> levelCache = new HashMap<>();

        // Helper to get cached level
        Function<BlockPos, ServerLevel> getLevel = (pos) -> {
            if (!worldData.blockDimensionIndices.containsKey(pos)) return null;
            int dimIndex = worldData.blockDimensionIndices.get(pos);
            ResourceKey<Level> dimKey = WorldData.getDimensionFromIndex(dimIndex);
            return levelCache.computeIfAbsent(dimKey, k -> rootLevel.getServer().getLevel(k));
        };

        // 1. Cleanup modified blocks (turn to air if not mined)
        for (BlockPos pos : worldData.modifiedBlocks) {
            if (worldData.minedBlocks.containsKey(pos)) continue;
            ServerLevel dimLevel = getLevel.apply(pos);
            if (dimLevel != null && !dimLevel.getBlockState(pos).isAir()) {
                dimLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
        }

        // 2. Cleanup Fluids
        for (BlockPos pos : worldData.modifiedBlocks) {
            ServerLevel dimLevel = getLevel.apply(pos);
            if (dimLevel != null) cleanupFlowingFluid(dimLevel, pos);
        }

        // 3. Restore Mined Blocks (Sorted Y for stability) & Tile Entities
        List<Map.Entry<BlockPos, BlockState>> sortedRestoration = new ArrayList<>(worldData.minedBlocks.entrySet());
        sortedRestoration.sort(Comparator.comparingInt(entry -> entry.getKey().getY()));

        for (Map.Entry<BlockPos, BlockState> entry : sortedRestoration) {
            BlockPos pos = entry.getKey();
            ServerLevel dimLevel = getLevel.apply(pos);
            if (dimLevel != null) {
                dimLevel.setBlock(pos, entry.getValue(), 18); // 18 = No update, No Physics
                
                // Restore TileEntity if data exists
                if (worldData.getBlockEntityData().containsKey(pos)) {
                    restoreTileEntity(dimLevel, pos, worldData.getBlockEntityData().get(pos));
                }
            }
        }

        // 4. Restore Fluid Blocks (Legacy support)
        List<Map.Entry<BlockPos, BlockState>> sortedFluid = new ArrayList<>(worldData.minedFluidBlocks.entrySet());
        sortedFluid.sort(Comparator.comparingInt(entry -> entry.getKey().getY()));
        for (Map.Entry<BlockPos, BlockState> entry : sortedFluid) {
            ServerLevel dimLevel = getLevel.apply(entry.getKey());
            if (dimLevel != null) dimLevel.setBlock(entry.getKey(), entry.getValue(), 18);
        }

        // 5. Restore Chunk Snapshots
        for (Map.Entry<ChunkPos, List<WorldData.SavedBlock>> entry : worldData.getSavedBlocksByChunk().entrySet()) {
            List<WorldData.SavedBlock> savedBlocks = entry.getValue();
            if (savedBlocks.isEmpty()) continue;
            
            ServerLevel dimLevel = rootLevel.getServer().getLevel(savedBlocks.get(0).dimension());
            if (dimLevel == null) continue;

            for (WorldData.SavedBlock saved : savedBlocks) {
                if (!dimLevel.getBlockState(saved.pos()).getBlock().equals(saved.state().getBlock())) {
                    dimLevel.setBlock(saved.pos(), saved.state(), 2);
                }
            }
        }

        // 6. Restore independent TileEntities (those not in minedBlocks)
        for (Map.Entry<BlockPos, CompoundTag> entry : worldData.getBlockEntityData().entrySet()) {
            if (worldData.minedBlocks.containsKey(entry.getKey())) continue; // Already handled
            ServerLevel dimLevel = getLevel.apply(entry.getKey());
            if (dimLevel != null) restoreTileEntity(dimLevel, entry.getKey(), entry.getValue());
        }
    }

    private static void restoreTileEntity(ServerLevel level, BlockPos pos, CompoundTag tag) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity != null) {
            blockEntity.load(tag);
            blockEntity.setChanged();
            level.sendBlockUpdated(pos, blockEntity.getBlockState(), blockEntity.getBlockState(), 3);
        }
    }

    private static void cleanupFlowingFluid(ServerLevel level, BlockPos pos) {
        BlockState currentState = level.getBlockState(pos);
        if (currentState.getBlock() instanceof LiquidBlock) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        } else if (currentState.hasProperty(BlockStateProperties.WATERLOGGED) && currentState.getValue(BlockStateProperties.WATERLOGGED)) {
            level.setBlock(pos, currentState.setValue(BlockStateProperties.WATERLOGGED, false), 3);
        }
    }

    private static void restorePlayers(ServerLevel rootLevel, CheckpointData data) {
        for (ServerPlayer player : rootLevel.getServer().getPlayerList().getPlayers()) {
            PlayerData pdata = data.getPlayerData(player.getUUID());
            if (pdata == null) continue;

            ServerLevel targetLevel = rootLevel.getServer().getLevel(pdata.dimension);
            if (targetLevel != null) {
                player.teleportTo(targetLevel, pdata.posX, pdata.posY, pdata.posZ, pdata.yaw, pdata.pitch);
            }

            player.setHealth(pdata.health);
            player.getFoodData().setFoodLevel(pdata.hunger);
            player.setExperienceLevels(pdata.experienceLevel);
            player.experienceProgress = pdata.experienceProgress;
            player.setRemainingFireTicks(pdata.fireTicks);
            
            if (pdata.gameMode != null) {
                switch (pdata.gameMode.toLowerCase()) {
                    case "survival" -> player.setGameMode(GameType.SURVIVAL);
                    case "creative" -> player.setGameMode(GameType.CREATIVE);
                    case "adventure" -> player.setGameMode(GameType.ADVENTURE);
                    case "spectator" -> player.setGameMode(GameType.SPECTATOR);
                }
            }

            if (pdata.spawnDimension != null) {
                player.setRespawnPosition(pdata.spawnDimension, new BlockPos((int) pdata.spawnX, (int) pdata.spawnY, (int) pdata.spawnZ), pdata.yaw, true, false);
            }

            player.setDeltaMovement(new Vec3(pdata.motionX, pdata.motionY, pdata.motionZ));
            player.fallDistance = pdata.fallDistance;
            
            player.removeAllEffects();
            for (MobEffectInstance effect : pdata.potionEffects) {
                player.addEffect(new MobEffectInstance(effect));
            }

            // Restore Inventory using copies to preserve durability state in save
            player.getInventory().clearContent();
            for (int i = 0; i < pdata.inventory.size(); i++) {
                player.getInventory().setItem(i, pdata.inventory.get(i).copy());
            }
            
            // Restore Advancements
            restoreAdvancements(player, rootLevel.getServer().getAdvancements(), pdata.advancements);
        }
    }

    private static void restoreAdvancements(ServerPlayer player, ServerAdvancementManager manager, CompoundTag savedData) {
        for (Advancement advancement : manager.getAllAdvancements()) {
            AdvancementProgress currentProgress = player.getAdvancements().getOrStartProgress(advancement);
            CompoundTag savedProgressTag = null;
            String advKey = advancement.getId().toString();
            if (savedData.contains(advKey)) savedProgressTag = savedData.getCompound(advKey);

            for (String criterion : advancement.getCriteria().keySet()) {
                boolean wasCompleted = savedProgressTag != null && savedProgressTag.getBoolean(criterion);
                boolean isCompleted = currentProgress.getCriterion(criterion).isDone();

                if (isCompleted && !wasCompleted) player.getAdvancements().revoke(advancement, criterion);
                else if (!isCompleted && wasCompleted) player.getAdvancements().award(advancement, criterion);
            }
        }
    }

    private static void restoreEntities(ServerLevel rootLevel, CheckpointData data) {
        // 1. Wipe Phase
        List<Entity> entitiesToRemove = new ArrayList<>();
        for (ServerLevel serverLevel : rootLevel.getServer().getAllLevels()) {
            for (Entity entity : serverLevel.getAllEntities()) {
                if (entity instanceof ServerPlayer || entity.isRemoved()) continue;

                if (data.isEntitySaved(entity.getUUID())) {
                    // It's in the save, wipe it so we can replace it with the saved version
                    entitiesToRemove.add(entity);
                } else {
                    // Not in save. If it's a "bad" entity or transient, wipe it.
                    // "Good" entities (Villagers from new chunks) stay.
                    if (entity instanceof Enemy || 
                        entity instanceof Projectile || 
                        entity instanceof ItemEntity || 
                        entity instanceof ExperienceOrb || 
                        entity instanceof PrimedTnt || 
                        entity instanceof FallingBlockEntity) {
                        entitiesToRemove.add(entity);
                    }
                }
            }
            for (Entity e : entitiesToRemove) e.discard();
            entitiesToRemove.clear();
        }

        Map<UUID, Mob> restoredMobs = new HashMap<>();

        // 2. Restore Initial Entities
        restoreEntityList(rootLevel, data.getEntityData(), data.getEntityDimensions(), restoredMobs);

        // 3. Restore Dynamic Entities (Absolute Return)
        Map<UUID, CompoundTag> dynamicEntities = data.getDynamicEntityData();
        Map<UUID, String> dynamicDims = data.getDynamicEntityDimensions();
        
        for (Map.Entry<UUID, CompoundTag> entry : dynamicEntities.entrySet()) {
            UUID uuid = entry.getKey();
            if (restoredMobs.containsKey(uuid)) continue; // Already restored

            CompoundTag nbt = entry.getValue();
            String dimStr = dynamicDims.get(uuid);
            ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(dimStr));
            ServerLevel level = rootLevel.getServer().getLevel(dimKey);

            if (level != null) {
                // FORCE LOAD chunk if needed to check for dupes
                if (nbt.contains("Pos", 9)) {
                    ListTag pos = nbt.getList("Pos", 6);
                    int cx = SectionPos.blockToSectionCoord(pos.getDouble(0));
                    int cz = SectionPos.blockToSectionCoord(pos.getDouble(2));
                    level.getChunk(cx, cz); 
                }
                
                // Double check for duplicate in now-loaded chunk
                Entity existing = level.getEntity(uuid);
                if (existing != null && !existing.isRemoved()) existing.discard();

                EntityType.loadEntityRecursive(nbt, level, (e) -> {
                    if (e != null) {
                        level.addFreshEntity(e);
                        if (e instanceof Mob m) restoredMobs.put(m.getUUID(), m);
                    }
                    return e;
                });
            }
        }

        // 4. Restore Aggro Targets
        for (Map.Entry<UUID, UUID> entry : data.getEntityAggroTargets().entrySet()) {
            if (restoredMobs.containsKey(entry.getKey()) && restoredMobs.containsKey(entry.getValue())) {
                restoredMobs.get(entry.getKey()).setTarget(restoredMobs.get(entry.getValue()));
            }
        }

        // 5. Ground Items
        if (data.getGroundItems() != null) {
            for (CompoundTag nbt : data.getGroundItems()) {
                EntityType.loadEntityRecursive(nbt, rootLevel, (e) -> {
                    if (e instanceof ItemEntity) rootLevel.addFreshEntity(e);
                    return e;
                });
            }
        }
    }

    private static void restoreEntityList(ServerLevel rootLevel, List<CompoundTag> nbts, List<ResourceKey<Level>> dims, Map<UUID, Mob> mobMap) {
        for (int i = 0; i < nbts.size(); i++) {
            ServerLevel level = rootLevel.getServer().getLevel(dims.get(i));
            if (level != null) {
                EntityType.loadEntityRecursive(nbts.get(i), level, (e) -> {
                    if (e != null) {
                        level.addFreshEntity(e);
                        if (e instanceof Mob m) mobMap.put(m.getUUID(), m);
                    }
                    return e;
                });
            }
        }
    }

    private static void restoreWorldExtras(ServerLevel level, WorldData worldData) {
        for (BlockPos eyePos : worldData.addedEyes) {
            BlockState state = level.getBlockState(eyePos);
            if (state.getBlock() == Blocks.END_PORTAL_FRAME && state.getValue(EndPortalFrameBlock.HAS_EYE)) {
                level.setBlock(eyePos, state.setValue(EndPortalFrameBlock.HAS_EYE, false), 3);
            }
        }
        for (WorldData.LightningStrike strike : worldData.getSavedLightnings()) {
            long delay = strike.tickTime - level.getGameTime();
            if (delay < 0) delay = 1;
            LightningScheduler.schedule(level, strike.pos, strike.tickTime);
        }
        for (BlockPos firePos : worldData.getNewFires()) {
            if (level.getBlockState(firePos).getBlock() == Blocks.FIRE) {
                level.setBlockAndUpdate(firePos, Blocks.AIR.defaultBlockState());
            }
        }
    }
}