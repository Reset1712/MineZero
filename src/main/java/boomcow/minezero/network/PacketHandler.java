package boomcow.minezero.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PacketHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("MineZeroPacketHandler");

    public static void register() {
        PayloadTypeRegistry.playC2S().register(SelfDamagePacket.ID, SelfDamagePacket.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(SelfDamagePacket.ID, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayerEntity player = context.player();
                float damageAmount = 1.0f;

                EntityAttributeInstance attackAttr = player.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
                if (attackAttr != null) {
                    damageAmount = (float) attackAttr.getValue();
                }

                if (damageAmount > 0) {
                    DamageSource source = player.getDamageSources().playerAttack(player);
                    player.damage(source, damageAmount);
                    LOGGER.debug("Player {} self-inflicted {} damage.", player.getName().getString(), damageAmount);
                }
            });
        });
        LOGGER.info("MineZero networking registered.");
    }
}
