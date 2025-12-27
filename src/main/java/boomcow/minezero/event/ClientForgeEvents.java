package boomcow.minezero.event;

import boomcow.minezero.input.KeyBindings;
import boomcow.minezero.network.SelfDamagePacket;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientForgeEvents {

    private static final Logger LOGGER = LoggerFactory.getLogger("MineZeroClientEvents");

    public static void registerClientEvents() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) {
                return;
            }

            if (KeyBindings.EXAMPLE_ACTION_KEY != null) {
                while (KeyBindings.EXAMPLE_ACTION_KEY.wasPressed()) {
                    LOGGER.info("Example Action Key Pressed!");
                    client.player.sendMessage(Text.literal("Example Keybind Pressed!"), false);
                }
            }

            if (KeyBindings.SELF_DAMAGE_KEY != null) {
                while (KeyBindings.SELF_DAMAGE_KEY.wasPressed()) {
                    LOGGER.debug("Self Damage Key Pressed - Sending Packet!");
                    ClientPlayNetworking.send(new SelfDamagePacket());
                }
            }
        });
    }
}
