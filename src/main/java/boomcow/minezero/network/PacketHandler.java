package boomcow.minezero.network;

import boomcow.minezero.MineZero;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class PacketHandler {

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MineZero.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private static int packetId = 0;

    private static int id() {
        return packetId++;
    }

    public static void register() {
        LOGGER.info("Registering MineZero network packets...");

        INSTANCE.registerMessage(
                id(),
                SelfDamagePacket.class,
                SelfDamagePacket::encode,
                SelfDamagePacket::decode,
                SelfDamagePacket::handle);

        LOGGER.info("Finished registering {} network packets.", packetId);

    }

    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
}