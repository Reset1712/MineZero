package boomcow.minezero.network;

import com.mojang.logging.LogUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

import java.util.function.Supplier;

public class SelfDamagePacket {

    private static final Logger LOGGER = LogUtils.getLogger();

    public SelfDamagePacket() {
    }

    public static void encode(SelfDamagePacket msg, FriendlyByteBuf buf) {
    }

    public static SelfDamagePacket decode(FriendlyByteBuf buf) {
        return new SelfDamagePacket();
    }

    public static void handle(SelfDamagePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }

            ItemStack mainHandItem = player.getMainHandItem();
            float damageAmount = 1.0f;
            if (!mainHandItem.isEmpty() && mainHandItem.getAttributeModifiers(EquipmentSlot.MAINHAND).containsKey(Attributes.ATTACK_DAMAGE)) {

                damageAmount = (float) mainHandItem.getAttributeModifiers(EquipmentSlot.MAINHAND)
                        .get(Attributes.ATTACK_DAMAGE).stream()
                        .filter(attr -> attr.getOperation() == AttributeModifier.Operation.ADDITION)
                        .mapToDouble(AttributeModifier::getAmount)
                        .sum();
                damageAmount = Math.max(1.0f, damageAmount + 1.0f);

                LOGGER.debug("Calculated self-damage for {}: {}", player.getName().getString(), damageAmount);
            } else {
                LOGGER.debug("No weapon or attack damage attribute found for {}. Using default damage.", player.getName().getString());
                damageAmount = 1.0f;
            }

            if (damageAmount > 0) {
                DamageSource source = player.level().damageSources().playerAttack(player);
                player.hurt(source, damageAmount);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}