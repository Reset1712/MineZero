package boomcow.minezero.event;

import boomcow.minezero.ConfigHandler;
import boomcow.minezero.MineZeroMain;
import boomcow.minezero.ModSoundEvents;
import boomcow.minezero.checkpoint.CheckpointData;
import boomcow.minezero.checkpoint.CheckpointManager;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
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

            event.setCanceled(true);
            level.getServer().execute(() -> {
                CheckpointManager.restoreCheckpoint(player);
            });

            // Notify all players
            level.getServer().getPlayerList().getPlayers().forEach(p -> {

                //p.displayClientMessage(Component.literal("The anchor player has died! Resetting the world."), true);

                // Restore their individual states
                if (!p.getUUID().equals(data.getAnchorPlayerUUID())) {
                    //CheckpointManager.restoreCheckpoint(p);
                }
            });

            // Cancel the death of the anchor player



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

        // --- CORRECTED ResourceLocation ---
        ClientboundStopSoundPacket stopSoundPacket = new ClientboundStopSoundPacket(
                ResourceLocation.fromNamespaceAndPath(MineZeroMain.MODID, "death_chime"), // Use static factory method
                SoundSource.PLAYERS
        );
        // --- End Correction ---

        if (player.connection != null) { // Good to check connection before sending packets
            player.connection.send(stopSoundPacket);
        }

        // Ensure ModSoundEvents.DEATH_CHIME.get() returns a valid SoundEvent
        player.playNotifySound(ModSoundEvents.DEATH_CHIME.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
    }

    private void playAlternateChime(ServerPlayer player) {

        // --- CORRECTED ResourceLocation ---
        ClientboundStopSoundPacket stopSoundPacket = new ClientboundStopSoundPacket(
                ResourceLocation.fromNamespaceAndPath(MineZeroMain.MODID, "alt_death_chime"), // Use static factory method
                SoundSource.PLAYERS
        );
        // --- End Correction ---

        if (player.connection != null) {
            player.connection.send(stopSoundPacket);
        }

        player.playNotifySound(ModSoundEvents.ALT_DEATH_CHIME.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
    }

}

