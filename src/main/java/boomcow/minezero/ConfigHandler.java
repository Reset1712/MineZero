package boomcow.minezero;

import boomcow.minezero.MineZeroMain;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

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
            return me.shedaniel.clothconfig2.api. திரும்பு.SUCCESS;
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
    public static Screen getClothConfigScreen(Screen parentScreen) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parentScreen)
                .setTitle(Text.translatable("config." + MineZeroMain.MOD_ID + ".title"));
        builder.setSavingRunnable(() -> {
            AutoConfig.getConfigHolder(ConfigHandler.class).save();
        });

        ConfigCategory general = builder.getOrCreateCategory(Text.translatable("config." + MineZeroMain.MOD_ID + ".category.general"));

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        List<String> deathChimeOptions = Arrays.asList("CLASSIC", "ALTERNATE");
        general.addEntry(entryBuilder.startSelector(
                                Text.translatable("config." + MineZeroMain.MOD_ID + ".option.deathChime"),
                                deathChimeOptions.toArray(new String[0]),
                                get().deathChime
                        )
                        .setDefaultValue("CLASSIC")
                        .setTooltip(Text.translatable("config." + MineZeroMain.MOD_ID + ".option.deathChime.tooltip"))
                        .setSaveConsumer(newValue -> get().deathChime = newValue)
                        .build()
        );
        /*
        general.addEntry(entryBuilder.startBooleanToggle(
                                Text.translatable("config." + MineZeroMain.MOD_ID + ".option.exampleBoolean"),
                                get().exampleBoolean
                        )
                        .setDefaultValue(true)
                        .setTooltip(Text.translatable("config." + MineZeroMain.MOD_ID + ".option.exampleBoolean.tooltip"))
                        .setSaveConsumer(newValue -> get().exampleBoolean = newValue)
                        .build()
        );
        */

        return builder.build();
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