package boomcow.minezero.event;

import boomcow.minezero.ConfigHandler;
import boomcow.minezero.MineZero;
import boomcow.minezero.ModSoundEvents;
import boomcow.minezero.checkpoint.CheckpointData;
import boomcow.minezero.checkpoint.CheckpointManager;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@EventBusSubscriber(modid = MineZero.MODID)
public class DeathEventHandler {

    private static final Logger LOGGER = LogManager.getLogger();

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ServerLevel level = player.serverLevel();
        CheckpointData data = CheckpointData.get(level);

        if (data.getAnchorPlayerUUID() == null) {
            return;
        }

        if (!player.getUUID().equals(data.getAnchorPlayerUUID())) {
            return;
        }

        LOGGER.info("[MineZero] Anchor player died! Triggering Return By Death...");

        event.setCanceled(true);
        player.setHealth(player.getMaxHealth());
        player.removeAllEffects();

        try {
            CheckpointManager.restoreCheckpoint(player);
            LOGGER.info("[MineZero] Checkpoint restored successfully.");
        } catch (Exception e) {
            LOGGER.error("[MineZero] Critical error during restore!", e);
        }

        String chime = ConfigHandler.getDeathChime();
        if ("CLASSIC".equalsIgnoreCase(chime)) {
            playClassicChime(player);
        } else if ("ALTERNATE".equalsIgnoreCase(chime)) {
            playAlternateChime(player);
        }
    }

    private static void playClassicChime(ServerPlayer player) {
        ClientboundStopSoundPacket stopSoundPacket = new ClientboundStopSoundPacket(
                ResourceLocation.fromNamespaceAndPath(MineZero.MODID, "death_chime"),
                SoundSource.PLAYERS);
        player.connection.send(stopSoundPacket);
        player.playNotifySound(ModSoundEvents.DEATH_CHIME.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
    }

    private static void playAlternateChime(ServerPlayer player) {
        ClientboundStopSoundPacket stopSoundPacket = new ClientboundStopSoundPacket(
                ResourceLocation.fromNamespaceAndPath(MineZero.MODID, "alt_death_chime"),
                SoundSource.PLAYERS);
        player.connection.send(stopSoundPacket);
        player.playNotifySound(ModSoundEvents.ALT_DEATH_CHIME.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
    }
}