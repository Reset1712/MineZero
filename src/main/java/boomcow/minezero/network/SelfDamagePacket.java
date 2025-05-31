package boomcow.minezero.network;

import boomcow.minezero.MineZeroMain;
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

// This packet has no data, so a record is concise.
public record SelfDamagePacket() implements CustomPacketPayload {

    private static final Logger LOGGER = LogUtils.getLogger();

    // 1. Unique ID for this payload type.
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(MineZeroMain.MODID, "self_damage_trigger_v1");

    // 2. The Type object for registration.
    public static final Type<SelfDamagePacket> TYPE = new Type<>(ID);

    // 3. StreamCodec for serialization/deserialization.
    // For a packet with no data, StreamCodec.unit creates a codec that reads/writes nothing
    // and always produces the same instance (new SelfDamagePacket() in this case).
    public static final StreamCodec<FriendlyByteBuf, SelfDamagePacket> STREAM_CODEC = StreamCodec.unit(new SelfDamagePacket());

    // The FriendlyByteBuf constructor and write() method are effectively handled by the StreamCodec for registration.
    // So, they are not strictly needed here if you use the StreamCodec with the registrar.
    // If you were *not* using StreamCodec in registration and instead providing constructor/write method refs, you'd need them:
    /*
    public SelfDamagePacket(FriendlyByteBuf buf) {
        this(); // Calls the record's canonical constructor
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        // No data to write for this packet.
    }
    */

    // 4. Implementation of CustomPacketPayload.type()
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // 5. Static handler method.
    public static void handle(final SelfDamagePacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> {
            // Get the player who sent the packet.
            Player genericPlayer = context.player(); // This often returns Player, could be null.
            if (!(genericPlayer instanceof ServerPlayer player)) {
                LOGGER.warn("SelfDamagePacket received but sender is not a ServerPlayer or is null.");
                return;
            }

            // Ensure this is being handled on the server side for a C2S packet.
            if (context.flow() != PacketFlow.SERVERBOUND) {
                LOGGER.warn("SelfDamagePacket received with unexpected flow: {}. Expected SERVERBOUND.", context.flow());
                return;
            }

            ItemStack mainHandItem = player.getMainHandItem();
            float damageAmount = 1.0f; // Default damage

            if (!mainHandItem.isEmpty()) {
                // ItemStack.getAttributeModifiers(EquipmentSlot) now likely returns ItemAttributeModifiers
                // or it's obtained differently, e.g. via components: mainHandItem.get(DataComponents.ATTRIBUTE_MODIFIERS)
                // Let's assume for now it still returns something compatible with ItemAttributeModifiers structure
                // OR, more likely, attributes are now a DATA COMPONENT.

                // --- HOW TO GET ItemAttributeModifiers from ItemStack in 1.21.1 ---
                // Attributes are now typically stored as a data component on the ItemStack.
                // import net.minecraft.world.item.component.DataComponents; (at top of file)
                ItemAttributeModifiers attributeModifiersComponent = mainHandItem.get(DataComponents.ATTRIBUTE_MODIFIERS);

                if (attributeModifiersComponent != null && attributeModifiersComponent != ItemAttributeModifiers.EMPTY) {
                    double weaponDamageContribution = 0.0;
                    boolean foundAttackDamage = false;

                    // Iterate through the list of modifier entries
                    for (ItemAttributeModifiers.Entry entry : attributeModifiersComponent.modifiers()) {
                        // Check if the attribute is ATTACK_DAMAGE and applies to the MAINHAND
                        if (entry.attribute().is(Attributes.ATTACK_DAMAGE) && entry.slot().test(EquipmentSlot.MAINHAND)) {
                            foundAttackDamage = true;
                            AttributeModifier modifier = entry.modifier();
                            // Summing ADD_VALUE (formerly ADDITION) operations.
                            if (modifier.operation() == net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE) {
                                weaponDamageContribution += modifier.amount();
                            }
                            // You might need to handle other operations (ADD_MULTIPLIED_BASE, ADD_MULTIPLIED_TOTAL)
                            // if weapons commonly use them for their primary damage stat, though ADD_VALUE is typical for base damage.
                        }
                    }

                    if (foundAttackDamage) {
                        // Original logic: Math.max(1.0f, weaponContribution + 1.0f)
                        damageAmount = Math.max(1.0f, (float) weaponDamageContribution + 1.0f);
                        LOGGER.debug("Calculated self-damage for {} with {}: {}", player.getName().getString(), mainHandItem.getDisplayName().getString(), damageAmount);
                    } else {
                        // Item in hand, but no ATTACK_DAMAGE attribute for MAINHAND
                        LOGGER.debug("No ATTACK_DAMAGE attribute for main hand on held item {} for player {}. Using default damage 1.0.", mainHandItem.getDisplayName().getString(), player.getName().getString());
                        damageAmount = 1.0f;
                    }
                } else {
                    // Item in hand, but no attribute modifiers component or it's empty
                    LOGGER.debug("No attribute modifiers component or empty for held item {} for player {}. Using default damage 1.0.", mainHandItem.getDisplayName().getString(), player.getName().getString());
                    damageAmount = 1.0f;
                }
            } else {
                // Empty hand
                LOGGER.debug("No item in main hand for {}. Using default self-damage 1.0.", player.getName().getString());
                damageAmount = 1.0f;
            }

            if (damageAmount > 0) {
                // Use the player's own context to get damage sources.
                // playerAttack(player) implies the player is the source and target.
                DamageSource source = player.damageSources().playerAttack(player);
                player.hurt(source, damageAmount);
                LOGGER.info("Player {} self-inflicted {} damage.", player.getName().getString(), damageAmount);
            }
        }).exceptionally(e -> {
            String playerName = "UnknownPlayer";
            Player p = context.player(); // Get player for logging context
            if (p != null) {
                playerName = p.getName().getString();
            }
            LOGGER.error("Failed to handle SelfDamagePacket for player {}: {}",
                    playerName,
                    e.getMessage(),
                    e);
            return null; // Required for exceptionally
        });
    }
}