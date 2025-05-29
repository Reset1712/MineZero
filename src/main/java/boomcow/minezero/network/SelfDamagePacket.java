package boomcow.minezero.network;

import com.mojang.logging.LogUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot; // Import EquipmentSlot
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
        // No data to send
    }

    public static SelfDamagePacket decode(FriendlyByteBuf buf) {
        return new SelfDamagePacket();
    }

    public static void handle(SelfDamagePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            // A valid ServerPlayer sender will always have a level, so only need to check player null
            if (player == null) {
                return;
            }

            ItemStack mainHandItem = player.getMainHandItem();
            float damageAmount = 1.0f;

            // Use EquipmentSlot.MAINHAND explicitly
            if (!mainHandItem.isEmpty() && mainHandItem.getAttributeModifiers(EquipmentSlot.MAINHAND).containsKey(Attributes.ATTACK_DAMAGE)) {

                damageAmount = (float) mainHandItem.getAttributeModifiers(EquipmentSlot.MAINHAND)
                        .get(Attributes.ATTACK_DAMAGE).stream()
                        .filter(attr -> attr.getOperation() == AttributeModifier.Operation.ADDITION)
                        .mapToDouble(AttributeModifier::getAmount)
                        .sum();

                // Add the base 1 damage players always have, ensure minimum 1 total
                damageAmount = Math.max(1.0f, damageAmount + 1.0f);

                LOGGER.debug("Calculated self-damage for {}: {}", player.getName().getString(), damageAmount);
            } else {
                LOGGER.debug("No weapon or attack damage attribute found for {}. Using default damage.", player.getName().getString());
                damageAmount = 1.0f; // Default for non-weapon items (like punching yourself)
            }

            if (damageAmount > 0) {
                // --- Correction: Use player.level() ---
                // Get the damage source registry from the player's level using the getter
                DamageSource source = player.level().damageSources().playerAttack(player);
                player.hurt(source, damageAmount);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}