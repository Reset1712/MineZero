package boomcow.minezero.network;

import boomcow.minezero.MineZeroMain;
import com.mojang.logging.LogUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;


public class PacketHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Registers a listener for the RegisterPayloadHandlersEvent on the mod event bus.
     * This is typically called from your main mod class's constructor.
     * @param modEventBus The mod-specific event bus.
     */
    public static void register(final IEventBus modEventBus) {
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
        final PayloadRegistrar registrar = event.registrar(MineZeroMain.MODID).versioned("1");
        if (SelfDamagePacket.TYPE != null && SelfDamagePacket.STREAM_CODEC != null) {
            registrar.playToServer(
                    SelfDamagePacket.TYPE,
                    (StreamCodec<? super RegistryFriendlyByteBuf, SelfDamagePacket>) SelfDamagePacket.STREAM_CODEC,
                    SelfDamagePacket::handle
            );
            LOGGER.debug("Registered C2S payload: {}", SelfDamagePacket.ID);
        } else {
            LOGGER.error("SelfDamagePacket.TYPE or .STREAM_CODEC is null! Packet will not be registered.");
        }
        /*
        if (AnotherExamplePacket.TYPE != null && AnotherExamplePacket.STREAM_CODEC != null) {
            registrar.play(
                    AnotherExamplePacket.TYPE,
                    AnotherExamplePacket.STREAM_CODEC,
                    handler -> handler.server(AnotherExamplePacket::handleServer)
                                     .client(AnotherExamplePacket::handleClient)
            );
            LOGGER.debug("Registered payload: {}", AnotherExamplePacket.ID);
        }
        */

        LOGGER.info("Finished registering network payloads for MineZero.");
    }

    /*
    public static void sendToServer(CustomPacketPayload payload) {
        PacketDistributor.sendToServer(payload);
    }
    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }
    */
}