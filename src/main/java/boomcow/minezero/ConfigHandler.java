package boomcow.minezero;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Config(modid = MineZero.MODID)
@Mod.EventBusSubscriber(modid = MineZero.MODID)
public class ConfigHandler {

    @Config.Comment("General settings")
    public static General general = new General();

    @Config.Comment("Checkpoint settings")
    public static Checkpoints checkpoints = new Checkpoints();

    public static class General {
        @Config.Comment("Death Chime Options: CLASSIC, ALTERNATE")
        @Config.Name("deathChime")
        public String deathChime = "CLASSIC";
    }

    public static class Checkpoints {
        // Add checkpoint specific config fields here in the future
    }

    /**
     * Helper method to maintain API compatibility with the rest of the port.
     */
    public static String getDeathChime() {
        return general.deathChime;
    }

    /**
     * Syncs the config when changed from the in-game GUI.
     */
    @SubscribeEvent
    public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.getModID().equals(MineZero.MODID)) {
            ConfigManager.sync(MineZero.MODID, Config.Type.INSTANCE);
        }
    }
}