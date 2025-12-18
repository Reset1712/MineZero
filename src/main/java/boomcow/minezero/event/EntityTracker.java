package boomcow.minezero.event;

import boomcow.minezero.MineZero;
import boomcow.minezero.checkpoint.CheckpointData;
import boomcow.minezero.checkpoint.CheckpointManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

@EventBusSubscriber(modid = MineZero.MODID)
public class EntityTracker {

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide) return;
        if (CheckpointManager.isRestoring) return;

        Entity entity = event.getEntity();
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;

        CheckpointData data = CheckpointData.get(serverLevel);
        
        if (data.getAnchorPlayerUUID() == null) return;

        boolean shouldTrack = entity instanceof Mob || 
                              entity instanceof AbstractMinecart || 
                              entity instanceof Boat || 
                              entity instanceof ArmorStand || 
                              entity instanceof HangingEntity;

        if (entity instanceof Projectile || 
            entity instanceof PrimedTnt || 
            entity instanceof ItemEntity || 
            entity instanceof FallingBlockEntity) {
            shouldTrack = false;
        }

        if (shouldTrack) {
            if (event.loadedFromDisk()) {
                if (!data.isEntitySaved(entity.getUUID())) {
                    CompoundTag tag = new CompoundTag();
                    if (entity.save(tag)) {
                        data.trackDynamicEntity(entity.getUUID(), tag, serverLevel.dimension());
                    }
                }
            }
        }
    }
}