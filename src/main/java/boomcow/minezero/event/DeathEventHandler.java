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
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod.EventBusSubscriber(modid = MineZero.MODID)
public class DeathEventHandler {

    private static final Logger LOGGER = LogManager.getLogger();

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // ЛОГ 1: Событие смерти сработало
        LOGGER.info("[MineZero] Death event detected for player: {}", player.getName().getString());

        ServerLevel level = player.serverLevel();
        CheckpointData data = CheckpointData.get(level);

        // Проверка: установлен ли Якорь
        if (data.getAnchorPlayerUUID() == null) {
            LOGGER.warn("[MineZero] Restore failed: No anchor player set in data.");
            return;
        }

        // Проверка: является ли умерший Якорем
        if (!player.getUUID().equals(data.getAnchorPlayerUUID())) {
            LOGGER.info("[MineZero] Player {} died, but anchor is {}. No restore.", 
                player.getUUID(), data.getAnchorPlayerUUID());
            return;
        }

        // ЛОГ 2: Условия выполнены, начинаем возврат
        LOGGER.info("[MineZero] Anchor player died! Triggering Return By Death...");

        // ОТМЕНЯЕМ СМЕРТЬ (чтобы не было экрана возрождения)
        event.setCanceled(true);
        
        // Восстанавливаем здоровье, чтобы игрок не умер повторно в следующем тике
        player.setHealth(player.getMaxHealth());

        // Запускаем восстановление
        level.getServer().execute(() -> {
            try {
                CheckpointManager.restoreCheckpoint(player);
                LOGGER.info("[MineZero] Restore task submitted.");
            } catch (Exception e) {
                LOGGER.error("[MineZero] Critical error during restore!", e);
            }
        });

        // Звуки
        String chime = ConfigHandler.getDeathChime();
        if ("CLASSIC".equalsIgnoreCase(chime)) {
            playClassicChime(player);
        } else if ("ALTERNATE".equalsIgnoreCase(chime)) {
            playAlternateChime(player);
        }
    }

    private static void playClassicChime(ServerPlayer player) {
        ClientboundStopSoundPacket stopSoundPacket = new ClientboundStopSoundPacket(
                new ResourceLocation("minezero", "death_chime"),
                SoundSource.PLAYERS);
        player.connection.send(stopSoundPacket);
        player.playNotifySound(ModSoundEvents.DEATH_CHIME.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
    }

    private static void playAlternateChime(ServerPlayer player) {
        ClientboundStopSoundPacket stopSoundPacket = new ClientboundStopSoundPacket(
                new ResourceLocation("minezero", "alt_death_chime"),
                SoundSource.PLAYERS);
        player.connection.send(stopSoundPacket);
        player.playNotifySound(ModSoundEvents.ALT_DEATH_CHIME.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
    }
}
