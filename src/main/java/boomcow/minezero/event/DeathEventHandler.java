package boomcow.minezero.event;

import boomcow.minezero.checkpoint.CheckpointData;
import boomcow.minezero.checkpoint.CheckpointManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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

            ServerLevel level = player.serverLevel();
            CheckpointData data = CheckpointData.get(level);

            // Check if the player is the anchor player
            if (player.getUUID().equals(data.getAnchorPlayerUUID())) {
                // Reset the world
                CheckpointManager.restoreCheckpoint(player);

                // Notify all players
                level.getServer().getPlayerList().getPlayers().forEach(p -> {
                    p.displayClientMessage(Component.literal("The anchor player has died! Resetting the world."), true);
                });

                // Play sound to indicate reset
                player.serverLevel().playSound(
                        null,
                        player.blockPosition(),
                        ModSoundEvents.DEATH_CHIME.get(),
                        SoundSource.PLAYERS,
                        1.0F,
                        1.0F
                );

                // Cancel the death
                event.setCanceled(true);
                return;
            }

            // Handle non-anchor players' deaths
            if (data.getCheckpointPos() != null) {
                // Play sound for checkpoint restoration
                player.serverLevel().playSound(
                        null,
                        player.blockPosition(),
                        ModSoundEvents.DEATH_CHIME.get(),
                        SoundSource.PLAYERS,
                        1.0F,
                        1.0F
                );

                // Cancel death and restore checkpoint
                event.setCanceled(true);
                CheckpointManager.restoreCheckpoint(player);

                // Restore health
                player.setHealth(Math.max(data.getCheckpointHealth(), 1.0F));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
