package boomcow.minezero.network;

import boomcow.minezero.MineZero;
import com.mojang.logging.LogUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;
// If you need to send packets from this class (though usually done elsewhere):
// import net.neoforged.neoforge.network.PacketDistributor;
// import net.minecraft.network.protocol.common.custom.CustomPacketPayload;


public class PacketHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    // The old SimpleChannel INSTANCE is no longer used.
    // Protocol versioning is handled per channel by the PayloadRegistrar.

    // Packet IDs (like your old `id()` method) are not managed globally like this anymore.
    // Each packet type has its own unique ResourceLocation ID.
    // private static int packetId = 0;
    // private static int id() { return packetId++; }

    /**
     * Registers a listener for the RegisterPayloadHandlersEvent on the mod event bus.
     * This is typically called from your main mod class's constructor.
     * @param modEventBus The mod-specific event bus.
     */
    public static void register(final IEventBus modEventBus) {
        // Payloads are registered during this event.
        modEventBus.addListener(PacketHandler::onRegisterPayloadHandlers);
        LOGGER.info("Scheduled MineZero payload handler registration.");
    }

    /**
     * Event handler method to register all custom packet payloads.
     * This method is called by the event bus when NeoForge is ready for payload registration.
     * @param event The registration event.
     */
    private static void onRegisterPayloadHandlers(final RegisterPayloadHandlersEvent event) {
        LOGGER.info("Registering MineZero network payloads...");

        // Get a registrar for your mod's channel.
        // The channel name is your MODID.
        // The version is for your channel's protocol; increment if you make breaking changes to any packets on this channel.
        final PayloadRegistrar registrar = event.registrar(MineZero.MODID).versioned("1");

        // Register your SelfDamagePacket (assuming it's C2S)
        // It needs:
        // 1. SelfDamagePacket.TYPE (the CustomPacketPayload.Type<SelfDamagePacket>)
        // 2. SelfDamagePacket.STREAM_CODEC (the StreamCodec<FriendlyByteBuf, SelfDamagePacket>)
        // 3. A lambda to configure the handler, specifying server-side handling and the method.
        if (SelfDamagePacket.TYPE != null && SelfDamagePacket.STREAM_CODEC != null) {
            // For a Client-to-Server packet during the PLAY phase:
            // The IPayloadHandler<T> is your static ::handle method.
            // The StreamCodec for play packets often expects RegistryFriendlyByteBuf.
            // If your SelfDamagePacket.STREAM_CODEC is StreamCodec<FriendlyByteBuf, SelfDamagePacket>,
            // you might need to adapt it or ensure it's compatible.
            // Often, StreamCodec.unit() is fine with FriendlyByteBuf.
            // The registrar methods often take StreamCodec<? super RegistryFriendlyByteBuf, T> for play packets.
            // Let's assume SelfDamagePacket.STREAM_CODEC is compatible or can be adapted.

            // The IPayloadHandler functional interface is (MESSAGE, CONTEXT) -> void
            // Your SelfDamagePacket.handle(SelfDamagePacket, IPayloadContext) matches this.
            registrar.playToServer(
                    SelfDamagePacket.TYPE,
                    (StreamCodec<? super RegistryFriendlyByteBuf, SelfDamagePacket>) SelfDamagePacket.STREAM_CODEC, // Explicit cast if needed due to B extending FriendlyByteBuf
                    SelfDamagePacket::handle
            );
            LOGGER.debug("Registered C2S payload: {}", SelfDamagePacket.ID);
        } else {
            LOGGER.error("SelfDamagePacket.TYPE or .STREAM_CODEC is null! Packet will not be registered.");
        }


        // Example for registering another hypothetical packet:
        /*
        if (AnotherExamplePacket.TYPE != null && AnotherExamplePacket.STREAM_CODEC != null) {
            registrar.play(
                    AnotherExamplePacket.TYPE,
                    AnotherExamplePacket.STREAM_CODEC,
                    handler -> handler.server(AnotherExamplePacket::handleServer) // If C2S
                                     .client(AnotherExamplePacket::handleClient) // If it can also be S2C or is bidirectional
            );
            LOGGER.debug("Registered payload: {}", AnotherExamplePacket.ID);
        }
        */

        LOGGER.info("Finished registering network payloads for MineZero.");
    }

    // Methods for sending packets are usually static helpers or handled by PacketDistributor directly.
    // You typically don't need to "send" from this PacketHandler class itself, but rather from
    // where the packet needs to be initiated (e.g., client-side key press handler, server-side event handler).

    /*
    // Example helper method to send a packet to the server (call from client side)
    public static void sendToServer(CustomPacketPayload payload) {
        PacketDistributor.sendToServer(payload);
    }

    // Example helper method to send a packet to a specific player (call from server side)
    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }
    */
}