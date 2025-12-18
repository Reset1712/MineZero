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
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MineZero.MODID)
public class EntityTracker {

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide) return;
        if (CheckpointManager.isRestoring) return;

        Entity entity = event.getEntity();
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;

        CheckpointData data = CheckpointData.get(serverLevel);
        
        // If checkpoint hasn't been set yet, ignore
        if (data.getAnchorPlayerUUID() == null) return;

        // Determine if we should track this entity type
        boolean shouldTrack = entity instanceof Mob || 
                              entity instanceof AbstractMinecart || 
                              entity instanceof Boat ||
                              entity instanceof ArmorStand || 
                              entity instanceof HangingEntity;

        // Exclude transient things
        if (entity instanceof Projectile || 
            entity instanceof PrimedTnt || 
            entity instanceof ItemEntity || 
            entity instanceof FallingBlockEntity) {
            shouldTrack = false;
        }

        if (shouldTrack) {
            // "loadedFromDisk()" indicates it's an existing entity in the world (e.g. Villager in a new chunk),
            // not something just spawned by a mob spawner or breeding in this timeline.
            // However, even if it IS spawned, for "Absolute Return" we might want to ensure 
            // the world is wiped clean. But here we want to SAVE state.
            
            // Logic: If this entity is NOT in our database yet, save its INITIAL state.
            // So if I meet a Villager, save him. If he dies later, I have his data.
            
            // Note: event.loadedFromDisk() is often the best check for "Was this here before I arrived?"
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