package boomcow.minezero;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

@Config(name = MineZeroMain.MOD_ID)
public class ConfigHandler implements ConfigData {
    private static final Logger LOGGER = LoggerFactory.getLogger(MineZeroMain.MOD_ID + "-config");

    @ConfigEntry.Gui.Tooltip
    public String deathChime = "CLASSIC";
    
    private static ConfigHandler INSTANCE = null;

    public static void register() {
        AutoConfig.register(ConfigHandler.class, GsonConfigSerializer::new);
        INSTANCE = AutoConfig.getConfigHolder(ConfigHandler.class).getConfig();
        LOGGER.info("MineZero config loaded/initialized with Cloth Config.");
        AutoConfig.getConfigHolder(ConfigHandler.class).registerSaveListener((manager, data) -> {
            LOGGER.info("MineZero config saved!");
            INSTANCE = data;
            return net.minecraft.util.ActionResult.SUCCESS;
        });
    }

    public static ConfigHandler get() {
        if (INSTANCE == null) {
            LOGGER.warn("ConfigHandler.INSTANCE was null, attempting to retrieve from AutoConfig. Ensure register() is called.");
            INSTANCE = AutoConfig.getConfigHolder(ConfigHandler.class).getConfig();
        }
        return INSTANCE;
    }

    public static String getDeathChimeOption() {
        return get().deathChime;
    }

    @Override
    public void validatePostLoad() throws ConfigData.ValidationException {
        ConfigData.super.validatePostLoad();
        List<String> validChimes = Arrays.asList("CLASSIC", "ALTERNATE");
        if (!validChimes.contains(deathChime)) {
            LOGGER.warn("Invalid deathChime value '{}' found in config, resetting to default 'CLASSIC'.", deathChime);
            deathChime = "CLASSIC";
        }
    }
}
