package boomcow.minezero; // Or boomcow.minezero.ConfigHandler

import boomcow.minezero.MineZeroMain;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer; // Or Toml4jConfigSerializer
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen; // For the config screen parent
import net.minecraft.text.Text; // For translatable text in UI

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

// Define a class that holds your config values (this replaces your static COMMON object)
// The @Config annotation links this class to AutoConfig
@Config(name = MineZeroMain.MOD_ID) // This will result in a file like "config/minezero.json"
public class ConfigHandler implements ConfigData {
    private static final Logger LOGGER = LoggerFactory.getLogger(MineZeroMain.MOD_ID + "-config");

    // AutoConfig will automatically populate these fields from the config file.
    // @ConfigEntry.Gui.Excluded // Use this if you don't want a field to appear in the GUI but still be in the file
    // @ConfigEntry.Category("general") // (Optional) Define categories for GUI, though often done in screen builder

    @ConfigEntry.Gui.Tooltip // Adds a tooltip in the config screen
    public String deathChime = "CLASSIC"; // Default value

    // Example of another option
    // @ConfigEntry.Gui.Tooltip
    // public boolean exampleBoolean = true;

    // --- Transient fields are not saved to config, used for logic if needed ---
    // transient boolean someLogicFlag = false;


    // --- Static methods to access config values ---
    // This instance will be managed by AutoConfig
    private static ConfigHandler INSTANCE = null;

    public static void register() {
        // Register the config class with AutoConfig.
        // This will load the config from file or create it with defaults,
        // and make it available via AutoConfig.getConfigHolder(ConfigHandler.class).getConfig()
        AutoConfig.register(ConfigHandler.class, GsonConfigSerializer::new); // Or Toml4jConfigSerializer::new for TOML

        // Get the initial instance
        INSTANCE = AutoConfig.getConfigHolder(ConfigHandler.class).getConfig();
        LOGGER.info("MineZero config loaded/initialized with Cloth Config.");

        // You can listen to save events if needed
        AutoConfig.getConfigHolder(ConfigHandler.class).registerSaveListener((manager, data) -> {
            LOGGER.info("MineZero config saved!");
            INSTANCE = data; // Update our cached instance
            // Perform actions on save if needed
            return me.shedaniel.clothconfig2.api. திரும்பு.SUCCESS; // Typo in original Cloth, should be SaveAction.SUCCESS or similar
        });
    }

    // Public static getter for the config instance
    public static ConfigHandler get() {
        if (INSTANCE == null) {
            // This should ideally not happen if register() is called correctly during init
            // but as a fallback, try to get it from AutoConfig
            LOGGER.warn("ConfigHandler.INSTANCE was null, attempting to retrieve from AutoConfig. Ensure register() is called.");
            INSTANCE = AutoConfig.getConfigHolder(ConfigHandler.class).getConfig();
        }
        return INSTANCE;
    }

    // Static getter for specific values (convenience)
    public static String getDeathChimeOption() {
        return get().deathChime;
    }

    // --- Cloth Config Screen Builder (for ModMenuIntegration) ---
    public static Screen getClothConfigScreen(Screen parentScreen) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parentScreen)
                .setTitle(Text.translatable("config." + MineZeroMain.MOD_ID + ".title")); // e.g., "MineZero Configuration"

        // Save callback: when "Save" is clicked in the GUI
        builder.setSavingRunnable(() -> {
            // AutoConfig handles the actual saving when its holder is saved.
            // We just need to trigger AutoConfig's save.
            AutoConfig.getConfigHolder(ConfigHandler.class).save();
        });

        ConfigCategory general = builder.getOrCreateCategory(Text.translatable("config." + MineZeroMain.MOD_ID + ".category.general"));

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        // Death Chime Option (Dropdown/String List)
        List<String> deathChimeOptions = Arrays.asList("CLASSIC", "ALTERNATE");
        general.addEntry(entryBuilder.startSelector(
                                Text.translatable("config." + MineZeroMain.MOD_ID + ".option.deathChime"),
                                deathChimeOptions.toArray(new String[0]), // Options
                                get().deathChime // Current value
                        )
                        .setDefaultValue("CLASSIC") // Default value for the GUI
                        .setTooltip(Text.translatable("config." + MineZeroMain.MOD_ID + ".option.deathChime.tooltip"))
                        .setSaveConsumer(newValue -> get().deathChime = newValue) // When value changes in GUI
                        .build()
        );

        // Example Boolean:
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

    // --- Optional: Validation (called by AutoConfig after loading data) ---
    @Override
    public void validatePostLoad() throws ConfigData.ValidationException {
        ConfigData.super.validatePostLoad(); // Default validation
        // Add custom validation if needed
        List<String> validChimes = Arrays.asList("CLASSIC", "ALTERNATE");
        if (!validChimes.contains(deathChime)) {
            LOGGER.warn("Invalid deathChime value '{}' found in config, resetting to default 'CLASSIC'.", deathChime);
            deathChime = "CLASSIC";
            // Note: To make this change persist, you'd ideally trigger a save of the config.
            // AutoConfig.getConfigHolder(ConfigHandler.class).save(); // Be careful with recursive saves
        }
    }
}