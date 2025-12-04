package boomcow.minezero.checkpoint;

import boomcow.minezero.util.LightningScheduler;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.advancements.PlayerAdvancements;
import net.minecraft.block.BlockEndPortalFrame;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.item.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityEvokerFangs;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.entity.projectile.EntityPotion;
import net.minecraft.entity.projectile.EntityTippedArrow;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketChangeGameState;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.GameType;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class CheckpointManager {

    // SRG names for private fields in 1.12.2
    private static final String FIELD_ENTITY_FIRE = "field_70151_c"; // Entity.fire
    private static final String FIELD_PLAYER_SPAWN_DIMENSION = "field_71078_a"; // EntityPlayer.spawnDimension

    public static void setCheckpoint(EntityPlayerMP anchorPlayer) {
        Logger logger = LogManager.getLogger();
        logger.info("Setting checkpoint...");
        long startTime = System.nanoTime();

        WorldServer level = anchorPlayer.getServerWorld();
        CheckpointData data = CheckpointData.get(level);

        data.saveWorldData(level);
        data.setAnchorPlayerUUID(anchorPlayer.getUniqueID());

        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();

        // Save Players
        for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
            PlayerData pdata = new PlayerData();
            pdata.posX = player.posX;
            pdata.posY = player.posY;
            pdata.posZ = player.posZ;
            pdata.yaw = player.rotationYaw;
            pdata.pitch = player.rotationPitch;
            pdata.dimension = player.dimension;

            GameType type = player.interactionManager.getGameType();
            pdata.gameMode = type.getName().toLowerCase(Locale.ROOT);

            pdata.motionX = player.motionX;
            pdata.motionY = player.motionY;
            pdata.motionZ = player.motionZ;
            pdata.fallDistance = player.fallDistance;
            pdata.health = player.getHealth();
            pdata.hunger = player.getFoodStats().getFoodLevel();
            pdata.experienceLevel = player.experienceLevel;
            pdata.experienceProgress = player.experience;

            // FIX: Access private 'fire' field via Reflection
            try {
                pdata.fireTicks = ObfuscationReflectionHelper.getPrivateValue(Entity.class, player, FIELD_ENTITY_FIRE);
            } catch (Exception e) {
                pdata.fireTicks = 0;
                logger.warn("Failed to reflectively get fire ticks", e);
            }

            // In 1.12, bed location is per dimension, but usually checked against the player's spawn dim
            BlockPos spawn = player.getBedLocation(player.dimension);
            boolean forced = player.isSpawnForced(player.dimension);

            if (spawn != null) {
                pdata.spawnX = spawn.getX() + 0.5;
                pdata.spawnY = spawn.getY();
                pdata.spawnZ = spawn.getZ() + 0.5;

                // FIX: Access private 'spawnDimension' field via Reflection
                try {
                    pdata.spawnDimension = ObfuscationReflectionHelper.getPrivateValue(EntityPlayer.class, player, FIELD_PLAYER_SPAWN_DIMENSION);
                } catch (Exception e) {
                    pdata.spawnDimension = 0; // Default to overworld
                }

                pdata.spawnForced = forced;
            }

            pdata.potionEffects.clear();
            for (PotionEffect effect : player.getActivePotionEffects()) {
                pdata.potionEffects.add(new PotionEffect(effect));
            }

            pdata.inventory.clear();
            for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
                ItemStack stack = player.inventory.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    pdata.inventory.add(stack.copy());
                } else {
                    pdata.inventory.add(ItemStack.EMPTY);
                }
            }

            // Advancements
            NBTTagCompound advTag = new NBTTagCompound();
            PlayerAdvancements playerAdvancements = player.getAdvancements();
            for (Advancement advancement : server.getAdvancementManager().getAdvancements()) {
                AdvancementProgress progress = playerAdvancements.getProgress(advancement);
                if (progress.hasProgress()) {
                    NBTTagCompound progressTag = new NBTTagCompound();
                    for (String criterion : progress.getCompletedCriteria()) {
                        progressTag.setBoolean(criterion, true);
                    }
                    advTag.setTag(advancement.getId().toString(), progressTag);
                }
            }
            pdata.advancements = advTag;

            data.savePlayerData(player.getUniqueID(), pdata);
        }

        data.setCheckpointDayTime(level.getWorldTime());

        // Save Entities
        List<NBTTagCompound> entityList = new ArrayList<>();
        List<Integer> entityDimensions = new ArrayList<>();
        Map<UUID, UUID> entityAggroTargets = new HashMap<>();

        // Loop through all loaded dimensions
        for (WorldServer serverLevel : DimensionManager.getWorlds()) {
            for (Entity entity : serverLevel.getLoadedEntityList()) {
                // Filter Mobs
                if (entity instanceof EntityLiving) {
                    EntityLiving mob = (EntityLiving) entity;
                    NBTTagCompound entityNBT = new NBTTagCompound();
                    if (mob.writeToNBTOptional(entityNBT)) {
                        entityList.add(entityNBT);
                        entityDimensions.add(serverLevel.provider.getDimension());
                        if (mob.getAttackTarget() != null) {
                            entityAggroTargets.put(mob.getUniqueID(), mob.getAttackTarget().getUniqueID());
                        }
                    }
                }

                // Filter non-mobs (Projectiles, Items, etc)
                else if (entity instanceof EntityMinecartContainer ||
                        entity instanceof EntityBoat ||
                        entity instanceof EntityEnderCrystal ||
                        entity instanceof EntityEvokerFangs ||
                        entity instanceof EntityXPOrb ||
                        entity instanceof EntityEnderEye ||
                        entity instanceof EntityFallingBlock ||
                        entity instanceof EntityItem ||
                        entity instanceof EntityLightningBolt ||
                        entity instanceof EntityTNTPrimed ||
                        entity instanceof EntityArmorStand ||
                        entity instanceof EntityFishHook ||
                        entity instanceof EntityPotion ||
                        entity instanceof EntityTippedArrow) {

                    NBTTagCompound entityNBT = new NBTTagCompound();
                    if (entity.writeToNBTOptional(entityNBT)) {
                        entityList.add(entityNBT);
                        entityDimensions.add(serverLevel.provider.getDimension());
                    }
                }
            }
        }

        data.setEntityAggroTargets(entityAggroTargets);
        data.setEntityDataWithDimensions(entityList, entityDimensions);

        List<NBTTagCompound> groundItemsList = new ArrayList<>();
        for (Entity entity : level.getLoadedEntityList()) {
            if (entity instanceof EntityItem) {
                NBTTagCompound itemNBT = new NBTTagCompound();
                entity.writeToNBT(itemNBT);
                groundItemsList.add(itemNBT);
            }
        }
        data.setGroundItems(groundItemsList);

        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;
        logger.debug("Saving states took {} ms", durationMs);
        logger.info("Checkpoint set");
    }

    public static void restoreCheckpoint(EntityPlayerMP anchorPlayer) {
        Logger logger = LogManager.getLogger();
        logger.debug("Restoring checkpoint...");

        long startTime = System.nanoTime();
        try {
            MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
            WorldServer level = anchorPlayer.getServerWorld();
            CheckpointData data = CheckpointData.get(level);
            WorldData worldData = data.getWorldData();

            if (data.getPlayerData(data.getAnchorPlayerUUID()) == null) {
                logger.error("Player data is null!");
                return;
            }

            // Restore Time and Weather
            WorldInfo worldInfo = level.getWorldInfo();
            level.setWorldTime(worldData.getDayTime());

            for(WorldServer ws : DimensionManager.getWorlds()) {
                ws.setWorldTime(worldData.getDayTime());
            }

            worldInfo.setRaining(worldData.isRaining());
            worldInfo.setThundering(worldData.isThundering());
            worldInfo.setCleanWeatherTime(worldData.getClearTime());

            if (worldData.isRaining()) {
                worldInfo.setRainTime(worldData.getRainTime());
                level.setRainStrength(1.0F);
            } else {
                worldInfo.setRainTime(0);
                level.setRainStrength(0.0F);
            }

            if (worldData.isThundering()) {
                worldInfo.setThunderTime(worldData.getThunderTime());
                level.setThunderStrength(1.0F);
            } else {
                worldInfo.setThunderTime(0);
                level.setThunderStrength(0.0F);
            }

            // Send Weather Packets
            for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
                if (worldData.isRaining()) {
                    player.connection.sendPacket(new SPacketChangeGameState(1, 0.0F));
                    player.connection.sendPacket(new SPacketChangeGameState(7, 1.0F));
                } else {
                    player.connection.sendPacket(new SPacketChangeGameState(2, 0.0F));
                    player.connection.sendPacket(new SPacketChangeGameState(7, 0.0F));
                }

                if (worldData.isThundering()) {
                    player.connection.sendPacket(new SPacketChangeGameState(8, 1.0F));
                } else {
                    player.connection.sendPacket(new SPacketChangeGameState(8, 0.0F));
                }
            }

            // Restore Modified Blocks (Set to Air)
            for (BlockPos pos : worldData.modifiedBlocks) {
                int dimIndex = worldData.blockDimensionIndices.get(pos);
                int dimID = WorldData.getDimensionFromIndex(dimIndex);
                WorldServer dimLevel = server.getWorld(dimID);

                if (dimLevel != null) {
                    IBlockState currentState = dimLevel.getBlockState(pos);
                    if (currentState.getBlock() != Blocks.AIR) {
                        dimLevel.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
                    }
                }
            }

            // Restore Mined Blocks
            for (Map.Entry<BlockPos, IBlockState> entry : worldData.minedBlocks.entrySet()) {
                BlockPos pos = entry.getKey();
                IBlockState originalState = entry.getValue();
                int dimIndex = worldData.blockDimensionIndices.get(pos);
                int dimID = WorldData.getDimensionFromIndex(dimIndex);
                WorldServer dimLevel = server.getWorld(dimID);

                if (dimLevel != null) {
                    IBlockState currentState = dimLevel.getBlockState(pos);
                    if (currentState.getBlock() == Blocks.AIR) {
                        dimLevel.setBlockState(pos, originalState, 2);
                    }
                }
            }

            // Restore Fluid Blocks (Modified)
            for (BlockPos pos : worldData.modifiedFluidBlocks) {
                if (!worldData.blockDimensionIndices.containsKey(pos)) continue;
                int dimIndex = worldData.blockDimensionIndices.get(pos);
                int dimID = WorldData.getDimensionFromIndex(dimIndex);
                WorldServer dimLevel = server.getWorld(dimID);

                if (dimLevel != null) {
                    IBlockState currentState = dimLevel.getBlockState(pos);
                    if (currentState.getBlock() != Blocks.AIR) {
                        dimLevel.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
                    }
                }
            }

            // Restore Fluid Blocks (Mined/Removed)
            for (Map.Entry<BlockPos, IBlockState> entry : worldData.minedFluidBlocks.entrySet()) {
                BlockPos pos = entry.getKey();
                IBlockState originalState = entry.getValue();

                if (!worldData.blockDimensionIndices.containsKey(pos)) continue;
                int dimIndex = worldData.blockDimensionIndices.get(pos);
                int dimID = WorldData.getDimensionFromIndex(dimIndex);
                WorldServer dimLevel = server.getWorld(dimID);

                if (dimLevel != null) {
                    IBlockState currentState = dimLevel.getBlockState(pos);
                    if (currentState.getBlock() == Blocks.AIR) {
                        dimLevel.setBlockState(pos, originalState, 3);
                    }
                }
            }

            // Restore Saved Chunks
            Map<ChunkPos, List<WorldData.SavedBlock>> savedBlocksByChunk = worldData.getSavedBlocksByChunk();
            for (Map.Entry<ChunkPos, List<WorldData.SavedBlock>> entry : savedBlocksByChunk.entrySet()) {
                List<WorldData.SavedBlock> savedBlocks = entry.getValue();
                if (savedBlocks.isEmpty()) continue;

                int dimID = savedBlocks.get(0).dimension;
                WorldServer dimLevel = server.getWorld(dimID);

                if (dimLevel == null) continue;

                for (WorldData.SavedBlock saved : savedBlocks) {
                    IBlockState currentState = dimLevel.getBlockState(saved.pos);
                    if (!currentState.getBlock().equals(saved.state.getBlock())) {
                        dimLevel.setBlockState(saved.pos, saved.state, 2);
                    }
                }
            }

            // Restore TileEntities
            for (Map.Entry<BlockPos, NBTTagCompound> entry : worldData.getBlockEntityData().entrySet()) {
                BlockPos pos = entry.getKey();
                int dimIndex = worldData.blockDimensionIndices.get(pos);
                int dimID = WorldData.getDimensionFromIndex(dimIndex);
                WorldServer dimLevel = server.getWorld(dimID);

                if (dimLevel != null) {
                    TileEntity tileEntity = dimLevel.getTileEntity(pos);
                    if (tileEntity != null) {
                        tileEntity.readFromNBT(entry.getValue());
                        tileEntity.markDirty();
                        dimLevel.notifyBlockUpdate(pos, dimLevel.getBlockState(pos), dimLevel.getBlockState(pos), 3);
                    }
                }
            }

            // Restore Players
            for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
                PlayerData pdata = data.getPlayerData(player.getUniqueID());
                if (pdata != null) {
                    if (player.dimension != pdata.dimension) {
                        server.getPlayerList().transferPlayerToDimension(player, pdata.dimension, new net.minecraft.world.Teleporter(server.getWorld(pdata.dimension)));
                        player.connection.setPlayerLocation(pdata.posX, pdata.posY, pdata.posZ, pdata.yaw, pdata.pitch);
                    } else {
                        player.connection.setPlayerLocation(pdata.posX, pdata.posY, pdata.posZ, pdata.yaw, pdata.pitch);
                    }

                    player.setHealth(pdata.health);
                    player.getFoodStats().setFoodLevel(pdata.hunger);
                    player.experienceLevel = pdata.experienceLevel;
                    player.experience = pdata.experienceProgress;

                    // FIX: Restore Fire ticks using reflection
                    try {
                        ObfuscationReflectionHelper.setPrivateValue(Entity.class, player, pdata.fireTicks, FIELD_ENTITY_FIRE);
                    } catch (Exception e) {
                        logger.warn("Failed to set fire ticks", e);
                    }

                    if (pdata.gameMode != null) {
                        GameType gt = GameType.getByName(pdata.gameMode);
                        if (gt != null) {
                            player.setGameType(gt);
                        }
                    }


                    // Restore Spawn Location
                    if (pdata.spawnDimension != 0 || pdata.spawnX != 0) {
                        // Forge 1.12.2 signature: setSpawnChunk(BlockPos pos, boolean forced, int dimension)
                        player.setSpawnChunk(
                                new BlockPos(pdata.spawnX, pdata.spawnY, pdata.spawnZ),
                                pdata.spawnForced,
                                pdata.spawnDimension
                        );

                        // The reflection block for 'FIELD_PLAYER_SPAWN_DIMENSION' is no longer needed
                        // because the 3rd argument above handles it automatically.
                    }

                    player.motionX = pdata.motionX;
                    player.motionY = pdata.motionY;
                    player.motionZ = pdata.motionZ;
                    player.fallDistance = pdata.fallDistance;

                    player.clearActivePotions();
                    for (PotionEffect effect : pdata.potionEffects) {
                        player.addPotionEffect(new PotionEffect(effect));
                    }

                    // Restore Advancements
                    NBTTagCompound savedAdvTag = pdata.advancements;
                    PlayerAdvancements playerAdvancements = player.getAdvancements();

                    for (Advancement advancement : server.getAdvancementManager().getAdvancements()) {
                        AdvancementProgress currentProgress = playerAdvancements.getProgress(advancement);
                        NBTTagCompound savedProgressTag = null;
                        String advKey = advancement.getId().toString();

                        if (savedAdvTag.hasKey(advKey)) {
                            savedProgressTag = savedAdvTag.getCompoundTag(advKey);
                        }

                        for (String criterion : advancement.getCriteria().keySet()) {
                            boolean wasCompleted = savedProgressTag != null && savedProgressTag.getBoolean(criterion);

                            // FIX: isCriterionObtained not available in 1.12. Using getCriterionProgress
                            CriterionProgress cp = currentProgress.getCriterionProgress(criterion);
                            boolean isCompleted = cp != null && cp.isObtained();

                            if (isCompleted && !wasCompleted) {
                                playerAdvancements.revokeCriterion(advancement, criterion);
                            } else if (!isCompleted && wasCompleted) {
                                playerAdvancements.grantCriterion(advancement, criterion);
                            }
                        }
                    }

                    player.inventory.clear();
                    for (int i = 0; i < pdata.inventory.size(); i++) {
                        if (i < player.inventory.getSizeInventory()) {
                            player.inventory.setInventorySlotContents(i, pdata.inventory.get(i));
                        }
                    }
                }
            }

            // Remove existing entities
            for (WorldServer serverLevel : DimensionManager.getWorlds()) {
                List<Entity> entitiesToRemove = new ArrayList<>();
                for (Entity entity : serverLevel.getLoadedEntityList()) {
                    if (!(entity instanceof EntityPlayer) && !entity.isDead) {
                        entitiesToRemove.add(entity);
                    }
                }
                for (Entity entity : entitiesToRemove) {
                    entity.setDead();
                }
            }

            // Restore Saved Entities
            List<NBTTagCompound> entities = data.getEntityData();
            List<Integer> entityDimensions = data.getEntityDimensions();
            Map<UUID, UUID> entityAggroTargets = data.getEntityAggroTargets();
            Map<UUID, EntityLiving> restoredMobs = new HashMap<>();

            for (int i = 0; i < entities.size(); i++) {
                NBTTagCompound eNBT = entities.get(i);
                int entityDim = entityDimensions.get(i);

                WorldServer targetLevel = server.getWorld(entityDim);
                if (targetLevel != null) {
                    Entity entity = EntityList.createEntityFromNBT(eNBT, targetLevel);
                    if (entity != null) {
                        targetLevel.spawnEntity(entity);
                        if (entity instanceof EntityLiving) {
                            restoredMobs.put(entity.getUniqueID(), (EntityLiving) entity);
                        }
                    }
                }
            }

            // Restore Aggro
            for (Map.Entry<UUID, UUID> entry : entityAggroTargets.entrySet()) {
                UUID mobUUID = entry.getKey();
                UUID targetUUID = entry.getValue();

                if (restoredMobs.containsKey(mobUUID)) {
                    EntityLiving mob = restoredMobs.get(mobUUID);
                    EntityLivingBase target = restoredMobs.get(targetUUID);

                    if (target == null) {
                        EntityPlayer p = server.getPlayerList().getPlayerByUUID(targetUUID);
                        if (p != null) target = p;
                    }

                    if (target != null) {
                        mob.setAttackTarget(target);
                    }
                }
            }

            // Restore Ground Items
            List<NBTTagCompound> groundItemsList = data.getGroundItems();
            if (groundItemsList != null) {
                for (NBTTagCompound itemNBT : groundItemsList) {
                    Entity entity = EntityList.createEntityFromNBT(itemNBT, level);
                    if (entity instanceof EntityItem) {
                        level.spawnEntity(entity);
                    }
                }
            }

            // Reset End Portal Frames
            for (BlockPos eyePos : worldData.addedEyes) {
                IBlockState state = level.getBlockState(eyePos);
                if (state.getBlock() == Blocks.END_PORTAL_FRAME && state.getValue(BlockEndPortalFrame.EYE)) {
                    level.setBlockState(eyePos, state.withProperty(BlockEndPortalFrame.EYE, false), 3);
                }
            }

            // Restore Lightning
            List<WorldData.LightningStrike> strikes = worldData.getSavedLightnings();
            for (WorldData.LightningStrike strike : strikes) {
                long delay = strike.tickTime - level.getWorldTime();
                if (delay < 0) delay = 1;
                // You must ensure LightningScheduler accepts net.minecraft.world.World, not ServerLevel
                LightningScheduler.schedule((World) level, strike.pos, strike.tickTime);
            }

            // Extinguish Fires
            for (BlockPos firePos : worldData.getNewFires()) {
                if (level.getBlockState(firePos).getBlock() == Blocks.FIRE) {
                    level.setBlockState(firePos, Blocks.AIR.getDefaultState());
                }
            }

            long endTime = System.nanoTime();
            long durationMs = (endTime - startTime) / 1_000_000;
            logger.debug("Restoring states took {} ms", durationMs);

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            e.printStackTrace();
        }
    }
}