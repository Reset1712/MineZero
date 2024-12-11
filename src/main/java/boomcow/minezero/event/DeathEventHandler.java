package boomcow.minezero.event;

import boomcow.minezero.checkpoint.CheckpointData;
import boomcow.minezero.checkpoint.CheckpointManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraft.sounds.SoundSource;
import boomcow.minezero.ModSoundEvents;

public class DeathEventHandler {
    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event) {
        try {
            if (!(event.getEntity() instanceof ServerPlayer player)) return;

            //System.out.println("Starting player death handling for: " + player.getName().getString());

            CheckpointData data = CheckpointData.get(player.serverLevel());
            if (data.getCheckpointPos() == null) {
                //System.out.println("No checkpoint set, exiting...");
                return;
            }

            //System.out.println("Restoring checkpoint at: " + data.getCheckpointPos());

            // Play sound
            player.serverLevel().playSound(
                    null,
                    player.blockPosition(),
                    ModSoundEvents.DEATH_CHIME.get(),
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F
            );
            //System.out.println("Played death chime sound.");

            // Cancel death
            event.setCanceled(true);
            //System.out.println("Death event canceled.");

            // Restore checkpoint
            CheckpointManager.restoreCheckpoint(player);
            //System.out.println("Checkpoint restored.");

            // Restore health
            player.setHealth(Math.max(data.getCheckpointHealth(), 1.0F));
            //System.out.println("Health restored to: " + player.getHealth());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
