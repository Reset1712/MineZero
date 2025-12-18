package boomcow.minezero.event;

import boomcow.minezero.checkpoint.CheckpointData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.UUID;

public class NonPlayerChangeHandler {

    public static float getExplosionRadius(Explosion explosion) {
        try {
            Field radiusField = Explosion.class.getDeclaredField("radius");
            radiusField.setAccessible(true);
            return radiusField.getFloat(explosion);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            return 0.0f;
        }
    }

    @SubscribeEvent
    public static void onExplosionStart(ExplosionEvent.Start event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        CheckpointData data = CheckpointData.get(level);
        UUID anchorUUID = data.getAnchorPlayerUUID();
        if (anchorUUID == null) return;

        ServerPlayer anchorPlayer = level.getServer().getPlayerList().getPlayer(anchorUUID);
        if (anchorPlayer == null || anchorPlayer.isDeadOrDying()) return;

        Explosion explosion = event.getExplosion();
        Vec3 explosionPos = explosion.center();
        float explosionRadius = getExplosionRadius(explosion);

        double distance = anchorPlayer.position().distanceTo(explosionPos);
        float seenPercent = Explosion.getSeenPercent(explosionPos, anchorPlayer);

        float scaled = (1.0F - (float)(distance / (explosionRadius * 2.0F))) * seenPercent;
        float estimatedDamage = (scaled * scaled + scaled) * explosionRadius * 7.0F + 1.0F;

        if (estimatedDamage >= anchorPlayer.getHealth()) {
            event.setCanceled(true); 
            anchorPlayer.hurt(level.damageSources().explosion(anchorPlayer, null), anchorPlayer.getMaxHealth() * 5);
        }
    }
}