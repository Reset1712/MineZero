package boomcow.minezero.event;

import boomcow.minezero.checkpoint.CheckpointData;
import boomcow.minezero.checkpoint.CheckpointManager;
import boomcow.minezero.checkpoint.PlayerData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraft.sounds.SoundSource;
import boomcow.minezero.ModSoundEvents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class DeathEventHandler {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(LivingDeathEvent event) {
        Logger logger = LogManager.getLogger();
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
                    //CheckpointManager.restoreCheckpoint(p);
                }
            });



            // Stop previous death chime
            ClientboundStopSoundPacket stopSoundPacket = new ClientboundStopSoundPacket(
                    new ResourceLocation("minezero", "death_chime"), // The exact sound name
                    SoundSource.PLAYERS
            );
            player.connection.send(stopSoundPacket);

            // Play new death chime
            player.playNotifySound(ModSoundEvents.DEATH_CHIME.get(), SoundSource.PLAYERS, 0.8F, 1.0F);





            // Cancel the death of the anchor player
            event.setCanceled(true);


        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            e.printStackTrace();
        }
    }

}

