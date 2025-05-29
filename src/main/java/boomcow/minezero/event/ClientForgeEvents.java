package boomcow.minezero.event;

import boomcow.minezero.input.KeyBindings;
import boomcow.minezero.network.SelfDamagePacket;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;

public class ClientForgeEvents { // Renaming to ClientGameEvents or similar might be clearer

    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onClientTickPostEvent(ClientTickEvent.Post event) { // Renamed method for clarity
        // No phase check needed because we are subscribing to the specific Post event

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        // Use .get() to get the KeyMapping instance from Lazy
        if (KeyBindings.EXAMPLE_ACTION_KEY != null) { // Lazy itself won't be null, but good to be safe if it could be
            while (KeyBindings.EXAMPLE_ACTION_KEY.get().consumeClick()) {
                LOGGER.info("Example Action Key Pressed!");
                mc.player.sendSystemMessage(Component.literal("Example Keybind Pressed!"));
            }
        }

        if (KeyBindings.SELF_DAMAGE_KEY != null) {
            while (KeyBindings.SELF_DAMAGE_KEY.get().consumeClick()) {
                LOGGER.debug("Self Damage Key Pressed - Sending Packet!");
                PacketDistributor.sendToServer(new SelfDamagePacket());
            }
        }
    }
}