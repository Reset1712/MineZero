package boomcow.minezero.network;

import boomcow.minezero.MineZero;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PacketHandler {

    private static final Logger LOGGER = LogManager.getLogger(MineZero.MODID);

    // 1.12.2 uses SimpleNetworkWrapper instead of SimpleChannel
    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(MineZero.MODID);

    private static int packetId = 0;
    private static int id() {
        return packetId++;
    }

    public static void register() {
        LOGGER.info("Registering MineZero network packets...");

        // Register the SelfDamagePacket.
        // 1.12.2 Syntax: registerMessage(Handler.class, Packet.class, ID, SideToHandleOn)
        // Since clients send this to the server, we register it for Side.SERVER.
        INSTANCE.registerMessage(SelfDamagePacket.Handler.class, SelfDamagePacket.class, id(), Side.SERVER);

        LOGGER.info("Finished registering {} network packets.", packetId);
    }
}