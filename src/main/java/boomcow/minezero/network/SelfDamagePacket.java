package boomcow.minezero.network;

import boomcow.minezero.MineZero;
import com.mojang.logging.LogUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;
public record SelfDamagePacket() implements CustomPacketPayload {

    private static final Logger LOGGER = LogUtils.getLogger();
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(MineZero.MODID, "self_damage_trigger_v1");
    public static final Type<SelfDamagePacket> TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, SelfDamagePacket> STREAM_CODEC = StreamCodec.unit(new SelfDamagePacket());
    /*
    public SelfDamagePacket(FriendlyByteBuf buf) {
        this();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
    }
    */
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    public static void handle(final SelfDamagePacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Player genericPlayer = context.player();
            if (!(genericPlayer instanceof ServerPlayer player)) {
                LOGGER.warn("SelfDamagePacket received but sender is not a ServerPlayer or is null.");
                return;
            }
            if (context.flow() != PacketFlow.SERVERBOUND) {
                LOGGER.warn("SelfDamagePacket received with unexpected flow: {}. Expected SERVERBOUND.", context.flow());
                return;
            }

            ItemStack mainHandItem = player.getMainHandItem();
            float damageAmount = 1.0f;

            if (!mainHandItem.isEmpty()) {
                ItemAttributeModifiers attributeModifiersComponent = mainHandItem.get(DataComponents.ATTRIBUTE_MODIFIERS);

                if (attributeModifiersComponent != null && attributeModifiersComponent != ItemAttributeModifiers.EMPTY) {
                    double weaponDamageContribution = 0.0;
                    boolean foundAttackDamage = false;
                    for (ItemAttributeModifiers.Entry entry : attributeModifiersComponent.modifiers()) {
                        if (entry.attribute().is(Attributes.ATTACK_DAMAGE) && entry.slot().test(EquipmentSlot.MAINHAND)) {
                            foundAttackDamage = true;
                            AttributeModifier modifier = entry.modifier();
                            if (modifier.operation() == net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE) {
                                weaponDamageContribution += modifier.amount();
                            }
                        }
                    }

                    if (foundAttackDamage) {
                        damageAmount = Math.max(1.0f, (float) weaponDamageContribution + 1.0f);
                        LOGGER.debug("Calculated self-damage for {} with {}: {}", player.getName().getString(), mainHandItem.getDisplayName().getString(), damageAmount);
                    } else {
                        LOGGER.debug("No ATTACK_DAMAGE attribute for main hand on held item {} for player {}. Using default damage 1.0.", mainHandItem.getDisplayName().getString(), player.getName().getString());
                        damageAmount = 1.0f;
                    }
                } else {
                    LOGGER.debug("No attribute modifiers component or empty for held item {} for player {}. Using default damage 1.0.", mainHandItem.getDisplayName().getString(), player.getName().getString());
                    damageAmount = 1.0f;
                }
            } else {
                LOGGER.debug("No item in main hand for {}. Using default self-damage 1.0.", player.getName().getString());
                damageAmount = 1.0f;
            }

            if (damageAmount > 0) {
                DamageSource source = player.damageSources().playerAttack(player);
                player.hurt(source, damageAmount);
                LOGGER.info("Player {} self-inflicted {} damage.", player.getName().getString(), damageAmount);
            }
        }).exceptionally(e -> {
            String playerName = "UnknownPlayer";
            Player p = context.player();
            if (p != null) {
                playerName = p.getName().getString();
            }
            LOGGER.error("Failed to handle SelfDamagePacket for player {}: {}",
                    playerName,
                    e.getMessage(),
                    e);
            return null;
        });
    }
}