package boomcow.minezero.network;

import com.google.common.collect.Multimap;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;

public class SelfDamagePacket implements IMessage {

    private static final Logger LOGGER = LogManager.getLogger();

    public SelfDamagePacket() {
        // Empty constructor required for IMessage
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        // No data to read
    }

    @Override
    public void toBytes(ByteBuf buf) {
        // No data to write
    }

    public static class Handler implements IMessageHandler<SelfDamagePacket, IMessage> {

        @Override
        public IMessage onMessage(SelfDamagePacket message, MessageContext ctx) {
            // Get the player from the context
            EntityPlayerMP player = ctx.getServerHandler().player;

            // Schedule the task on the main server thread
            player.getServerWorld().addScheduledTask(() -> {
                if (player == null) return;

                ItemStack mainHandItem = player.getHeldItemMainhand();
                float damageAmount = 1.0f;

                if (!mainHandItem.isEmpty()) {
                    // In 1.12.2, getAttributeModifiers returns a Multimap
                    Multimap<String, AttributeModifier> modifiers = mainHandItem.getAttributeModifiers(EntityEquipmentSlot.MAINHAND);
                    String attackDamageKey = SharedMonsterAttributes.ATTACK_DAMAGE.getName();

                    if (modifiers.containsKey(attackDamageKey)) {
                        Collection<AttributeModifier> attackModifiers = modifiers.get(attackDamageKey);

                        double sum = 0;
                        for (AttributeModifier mod : attackModifiers) {
                            // Operation 0 is ADDITION
                            if (mod.getOperation() == 0) {
                                sum += mod.getAmount();
                            }
                        }

                        // Base damage logic (similar to original code)
                        damageAmount = (float) Math.max(1.0f, sum + 1.0f);
                        LOGGER.debug("Calculated self-damage for {}: {}", player.getName(), damageAmount);
                    } else {
                        LOGGER.debug("No attack damage attribute found for {}. Using default damage.", player.getName());
                        damageAmount = 1.0f;
                    }
                } else {
                    LOGGER.debug("No weapon found for {}. Using default damage.", player.getName());
                    damageAmount = 1.0f;
                }

                if (damageAmount > 0) {
                    // causePlayerDamage creates a source of type "player"
                    player.attackEntityFrom(DamageSource.causePlayerDamage(player), damageAmount);
                }
            });

            return null; // No response packet needed
        }
    }
}