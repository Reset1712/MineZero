package boomcow.minezero.checkpoint;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

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
    }

    public static void restoreCheckpoint(ServerPlayer player) {
        try {
            ServerLevel level = player.serverLevel();
            CheckpointData data = CheckpointData.get(level);

            // Validate checkpoint data
            if (data.getCheckpointPos() == null) {
                System.out.println("No checkpoint position found.");
                return;
            }

            // Teleport player
            BlockPos pos = data.getCheckpointPos();
            player.teleportTo(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.getYRot(), player.getXRot());
            System.out.println("Teleported player to checkpoint position.");

            // Restore health
            player.setHealth(data.getCheckpointHealth());
            System.out.println("Restored player health to: " + player.getHealth());

            // Restore inventory
            List<ItemStack> inv = data.getCheckpointInventory();
            if (inv != null) {
                System.out.println("Restoring inventory with size: " + inv.size());
                player.getInventory().clearContent();
                for (int i = 0; i < inv.size(); i++) {
                    player.getInventory().setItem(i, inv.get(i).copy());
                }
            }

            // Restore entities
            List<CompoundTag> entities = data.getEntityData();
            if (entities != null) {
                System.out.println("Restoring entities with size: " + entities.size());
                for (CompoundTag eNBT : entities) {
                    EntityType.loadEntityRecursive(eNBT, level, (entity) -> {
                        level.addFreshEntity(entity);
                        return entity;
                    });
                }
            }

            // Restore day time
            level.setDayTime(data.getCheckpointDayTime());
            System.out.println("Restored day time to: " + data.getCheckpointDayTime());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}