package boomcow.minezero.event;

import boomcow.minezero.checkpoint.CheckpointData;
import boomcow.minezero.checkpoint.CheckpointManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.AbstractDecorationEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;

public class EntityTracker {

    public static void register() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (world.isClient) return;
            if (CheckpointManager.isRestoring) return;

            CheckpointData data = CheckpointData.get((ServerWorld) world);
            
            if (data.getAnchorPlayerUUID() == null) return;

            boolean shouldTrack = entity instanceof LivingEntity || 
                                  entity instanceof AbstractMinecartEntity || 
                                  entity instanceof BoatEntity || 
                                  entity instanceof ArmorStandEntity || 
                                  entity instanceof AbstractDecorationEntity;

            if (entity instanceof ProjectileEntity || 
                entity instanceof TntEntity || 
                entity instanceof ItemEntity) { 
                shouldTrack = false;
            }

            if (shouldTrack) {
                if (!data.isEntitySaved(entity.getUuid())) {
                    NbtCompound tag = new NbtCompound();
                    if (entity.saveNbt(tag)) {
                        data.trackDynamicEntity(entity.getUuid(), tag, world.getRegistryKey());
                    }
                }
            }
        });
    }
}
