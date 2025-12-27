package boomcow.minezero.event;

import boomcow.minezero.ConfigHandler;
import boomcow.minezero.MineZeroMain;
import boomcow.minezero.ModSoundEvents;
import boomcow.minezero.checkpoint.CheckpointData;
import boomcow.minezero.checkpoint.CheckpointManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.network.packet.s2c.play.StopSoundS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeathEventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("MineZeroDeathHandler");

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, damageSource, damageAmount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;

            try {
                ServerWorld world = (ServerWorld) player.getWorld();
                CheckpointData data = CheckpointData.get(world);
                MinecraftServer server = player.getServer();
                
                if (server != null) {
                    CheckpointTicker.lastCheckpointTick = server.getTicks();
                }

                if (data.getAnchorPlayerUUID() == null || !player.getUuid().equals(data.getAnchorPlayerUUID())) {
                    return true;
                }

                LOGGER.info("[MineZero] Anchor player died! Triggering Return By Death...");
                
                world.getServer().execute(() -> {
                    CheckpointManager.restoreCheckpoint(player);
                });

                String chime = ConfigHandler.getDeathChimeOption();
                if ("CLASSIC".equalsIgnoreCase(chime)) {
                    playClassicChime(player);
                } else if ("ALTERNATE".equalsIgnoreCase(chime)) {
                    playAlternateChime(player);
                }

                return false; 
            } catch (Exception e) {
                LOGGER.error("Error handling player death", e);
                return true; 
            }
        });
    }

    private static void playClassicChime(ServerPlayerEntity player) {
        StopSoundS2CPacket stopSoundPacket = new StopSoundS2CPacket(
                Identifier.of(MineZeroMain.MOD_ID, "death_chime"),
                SoundCategory.PLAYERS
        );

        if (player.networkHandler != null) {
            player.networkHandler.sendPacket(stopSoundPacket);
        }
        player.playSound(ModSoundEvents.DEATH_CHIME, SoundCategory.PLAYERS, 0.8F, 1.0F);
    }

    private static void playAlternateChime(ServerPlayerEntity player) {
        StopSoundS2CPacket stopSoundPacket = new StopSoundS2CPacket(
                Identifier.of(MineZeroMain.MOD_ID, "alt_death_chime"),
                SoundCategory.PLAYERS
        );

        if (player.networkHandler != null) {
            player.networkHandler.sendPacket(stopSoundPacket);
        }
        player.playSound(ModSoundEvents.ALT_DEATH_CHIME, SoundCategory.PLAYERS, 0.8F, 1.0F);
    }
}
