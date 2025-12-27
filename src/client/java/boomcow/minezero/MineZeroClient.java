package boomcow.minezero;

import boomcow.minezero.event.ClientForgeEvents;
import boomcow.minezero.input.KeyBindings;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MineZeroClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger(MineZeroMain.MOD_ID + "-client");

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing MineZero client specifics (Yarn Mappings)!");
        KeyBindings.registerKeyBindings();
        ClientForgeEvents.registerClientEvents();
    }
}
