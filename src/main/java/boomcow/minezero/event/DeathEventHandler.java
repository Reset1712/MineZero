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
            if (data.getAnchorPlayerUUID() == null || !player.getUUID().equals(data.getAnchorPlayerUUID())) {
                return; // Do nothing if the player is not the anchor player
            }

            // Reset the world when the anchor player dies
            CheckpointManager.restoreCheckpoint(player);

            // Notify all players
            level.getServer().getPlayerList().getPlayers().forEach(p -> {
                //p.displayClientMessage(Component.literal("The anchor player has died! Resetting the world."), true);

                // Restore their individual states
                if (!p.getUUID().equals(data.getAnchorPlayerUUID())) {
                    CheckpointManager.restoreCheckpoint(p);
                }
            });

            // Play the chime sound for all players
            level.playSound(
                    null,
                    player.blockPosition(),
                    ModSoundEvents.DEATH_CHIME.get(),
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F
            );

            // Cancel the death of the anchor player
            event.setCanceled(true);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
