package boomcow.minezero.network;

import boomcow.minezero.MineZero;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class PacketHandler {

    private static final String PROTOCOL_VERSION = "1"; // Change if you modify packet structures non-compatibly
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MineZero.MODID, "main"), // Unique name for your channel
            () -> PROTOCOL_VERSION,                     // Supplier for protocol version (client side)
            PROTOCOL_VERSION::equals,                   // Predicate for server version compatibility
            PROTOCOL_VERSION::equals                    // Predicate for client version compatibility
    );

    private static int packetId = 0;
    private static int id() {
        return packetId++;
    }

    public static void register() {
        LOGGER.info("Registering MineZero network packets...");

        // Register your packets here
        INSTANCE.registerMessage(
                id(), // Unique ID for this packet type within the channel
                SelfDamagePacket.class,          // The packet class
                SelfDamagePacket::encode,        // Method reference for encoding
                SelfDamagePacket::decode,        // Method reference for decoding
                SelfDamagePacket::handle         // Method reference for handling
        );

        // Register more packets here if you add them later
        // INSTANCE.registerMessage(id(), OtherPacket.class, ...);

        LOGGER.info("Finished registering {} network packets.", packetId);

    }
    // Add a logger instance if you don't have one accessible easily
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
}