package boomcow.minezero.event;

import boomcow.minezero.checkpoint.CheckpointData;
import boomcow.minezero.checkpoint.WorldData;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber
public class NonPlayerChangeHandler {

    private static WorldData getActiveWorldData(World level) {
        if (level == null || level.getMinecraftServer() == null) return null;
        CheckpointData checkpointData = CheckpointData.get(level);
        return checkpointData.getWorldData();
    }

    @SubscribeEvent
    public static void onFirePlaced(BlockEvent.PlaceEvent event) {
        World world = event.getWorld();
        if (world.isRemote || !(world instanceof WorldServer)) return;

        WorldServer level = (WorldServer) world;

        if (event.getPlacedBlock().getBlock() == Blocks.FIRE) {
            CheckpointData data = CheckpointData.get(level);
            long now = level.getTotalWorldTime();

            if (now > data.getWorldData().getCheckpointTick()) {
                data.getWorldData().getNewFires().add(event.getPos());
            }
        }
    }

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        World world = event.getWorld();
        if (world.isRemote || !(world instanceof WorldServer)) return;
        WorldServer level = (WorldServer) world;

        CheckpointData data = CheckpointData.get(level);
        WorldData worldDataInstance = getActiveWorldData(level);

        long now = level.getTotalWorldTime();
        int dimensionIndex = WorldData.getDimensionIndex(level.provider.getDimension());

        for (BlockPos pos : event.getAffectedBlocks()) {
            IBlockState state = level.getBlockState(pos);

            // In 1.12, use getBlock() == Blocks.AIR check
            if (state.getBlock() == Blocks.AIR || now <= data.getWorldData().getCheckpointTick()) continue;

            List<BlockPos> affectedPositions = new ArrayList<>();
            affectedPositions.add(pos);

            Block block = state.getBlock();
            if (block instanceof BlockDoor) {
                if (state.getValue(BlockDoor.HALF) == BlockDoor.EnumDoorHalf.LOWER) {
                    affectedPositions.add(pos.up());
                } else {
                    affectedPositions.add(pos.down());
                }
            }
            else if (block instanceof BlockBed) {
                EnumFacing facing = state.getValue(BlockBed.FACING);
                if (state.getValue(BlockBed.PART) == BlockBed.EnumPartType.FOOT) {
                    affectedPositions.add(pos.offset(facing));
                } else {
                    affectedPositions.add(pos.offset(facing.getOpposite()));
                }
            }

            for (BlockPos currentPos : affectedPositions) {
                if (worldDataInstance.getModifiedBlocks().contains(currentPos)) {
                    worldDataInstance.getModifiedBlocks().remove(currentPos);
                } else {
                    IBlockState currentState = level.getBlockState(currentPos);
                    worldDataInstance.getMinedBlocks().put(currentPos, currentState);
                    worldDataInstance.getInstanceBlockDimensionIndices().put(currentPos, dimensionIndex);
                }
            }
        }
    }

    public static float getExplosionRadius(Explosion explosion) {
        try {
            // "size" is the field name, "field_77280_f" is the SRG name for 1.12.2
            return ObfuscationReflectionHelper.getPrivateValue(Explosion.class, explosion, "size", "field_77280_f");
        } catch (Exception e) {
            e.printStackTrace();
            return 0.0f;
        }
    }


    @SubscribeEvent
    public static void onExplosionStart(ExplosionEvent.Start event) {
        World world = event.getWorld();
        if (world.isRemote || !(world instanceof WorldServer)) return;
        WorldServer level = (WorldServer) world;

        CheckpointData data = CheckpointData.get(level);
        UUID anchorUUID = data.getAnchorPlayerUUID();
        if (anchorUUID == null) return;

        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        EntityPlayerMP anchorPlayer = server.getPlayerList().getPlayerByUUID(anchorUUID);

        // isDead check
        if (anchorPlayer == null || anchorPlayer.isDead || anchorPlayer.getHealth() <= 0) return;

        Explosion explosion = event.getExplosion();
        Vec3d explosionPos = explosion.getPosition();
        float explosionRadius = getExplosionRadius(explosion);

        double distance = anchorPlayer.getPositionVector().distanceTo(explosionPos);

        // 1.12.2 logic for "Seen Percent" / Exposure
        float seenPercent = level.getBlockDensity(explosionPos, anchorPlayer.getEntityBoundingBox());

        float scaled = (1.0F - (float)(distance / (explosionRadius * 2.0F))) * seenPercent;

        // Standard Minecraft explosion damage formula
        float estimatedDamage = (scaled * scaled + scaled) * explosionRadius * 7.0F + 1.0F;

        Logger logger = LogManager.getLogger();
        logger.info("Explosion at {} | distance: {} | seen: {} | estimated dmg: {}",
                explosionPos,
                String.format("%.2f", distance),
                String.format("%.2f", seenPercent),
                String.format("%.2f", estimatedDamage));

        if (estimatedDamage >= anchorPlayer.getHealth()) {
            event.setCanceled(true);
            // Apply damage manually to ensure it kills if intended, bypassing normal explosion logic since we canceled it
            anchorPlayer.attackEntityFrom(DamageSource.causeExplosionDamage(explosion), anchorPlayer.getMaxHealth() * 5);
        }
    }
}