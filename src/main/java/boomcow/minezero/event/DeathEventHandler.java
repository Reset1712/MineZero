package boomcow.minezero.event;

import boomcow.minezero.ConfigHandler;
import boomcow.minezero.ModSoundEvents;
import boomcow.minezero.checkpoint.CheckpointData;
import boomcow.minezero.checkpoint.CheckpointManager;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DeathEventHandler {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(LivingDeathEvent event) {
        Logger logger = LogManager.getLogger();
        try {
            if (!(event.getEntity() instanceof ServerPlayer player)) return;


            ServerLevel level = player.serverLevel();
            CheckpointData data = CheckpointData.get(level);
            MinecraftServer server = player.getServer();
            if (server != null) {
                CheckpointTicker.lastCheckpointTick = server.getTickCount();
            }



            // Check if the player is the anchor player
            if (data.getAnchorPlayerUUID() == null || !player.getUUID().equals(data.getAnchorPlayerUUID())) {

                return; // Do nothing if the player is not the anchor player
            }


            CheckpointManager.restoreCheckpoint(player);

            // Notify all players
            level.getServer().getPlayerList().getPlayers().forEach(p -> {

                //p.displayClientMessage(Component.literal("The anchor player has died! Resetting the world."), true);

                // Restore their individual states
                if (!p.getUUID().equals(data.getAnchorPlayerUUID())) {
                    //CheckpointManager.restoreCheckpoint(p);
                }
            });

            // Cancel the death of the anchor player
            event.setCanceled(true);

            // Play a chime sound
            String chime = ConfigHandler.getDeathChime();
            if ("CLASSIC".equalsIgnoreCase(chime)) {
                playClassicChime(player);
            } else if ("ALTERNATE".equalsIgnoreCase(chime)) {
                playAlternateChime(player);
            }




        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            e.printStackTrace();
        }
    }

    private void playClassicChime(ServerPlayer player) {
        // Stop previous death chime
        Logger logger = LogManager.getLogger();
        logger.debug("Playing classic chime");
        ClientboundStopSoundPacket stopSoundPacket = new ClientboundStopSoundPacket(
                new ResourceLocation("minezero", "death_chime"), // The exact sound name
                SoundSource.PLAYERS
        );
        player.connection.send(stopSoundPacket);

        // Play new death chime
        player.playNotifySound(ModSoundEvents.DEATH_CHIME.get(), SoundSource.PLAYERS, 0.8F, 1.0F);



    }

    private void playAlternateChime(ServerPlayer player) {
        // Stop previous death chime
        Logger logger = LogManager.getLogger();
        logger.debug("Playing alternate chime");
        ClientboundStopSoundPacket stopSoundPacket = new ClientboundStopSoundPacket(
                new ResourceLocation("minezero", "alt_death_chime"), // The exact sound name
                SoundSource.PLAYERS
        );
        player.connection.send(stopSoundPacket);

        // Play new death chime
        player.playNotifySound(ModSoundEvents.ALT_DEATH_CHIME.get(), SoundSource.PLAYERS, 0.8F, 1.0F);



    }

}

