package boomcow.minezero;

// Old Forge imports:
// import net.minecraftforge.common.ForgeConfigSpec;
// import net.minecraftforge.fml.config.ModConfig;

// NeoForge imports:

import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;
// No need for MineZero import here usually, unless you were calling a static logger from MineZero
// import boomcow.minezero.MineZero; // Not strictly needed here, but MineZero.LOGGER might be used in events

public class ConfigHandler {

    // It's good practice to name the Spec object clearly as the "Spec"
    public static final ModConfigSpec COMMON_CONFIG_SPEC;
    public static final CommonConfig COMMON;

    static {
        // The Builder pattern should be identical
        Pair<CommonConfig, ModConfigSpec> commonPair = new ModConfigSpec.Builder().configure(CommonConfig::new);
        COMMON_CONFIG_SPEC = commonPair.getRight();
        COMMON = commonPair.getLeft();
    }

    public static class CommonConfig {
        // ConfigValue type should come from the NeoForge ModConfigSpec
        public final ModConfigSpec.ConfigValue<String> deathChime;
        // public final ModConfigSpec.BooleanValue myExampleBool; // Example

        public CommonConfig(ModConfigSpec.Builder builder) {
            builder.comment("General settings").push("general");

            deathChime = builder
                    .comment("Death Chime Options: CLASSIC, ALTERNATE. Default: CLASSIC")
                    .define("deathChime", "CLASSIC"); // .defineInRange, .defineList etc. for other types

            // Example:
            // myExampleBool = builder
            //        .comment("An example boolean config option.")
            //        .define("exampleBoolean", true);

            builder.pop(); // Pop "general"

            // If you have checkpoint settings, define them here:
            // builder.comment("Checkpoint settings").push("checkpoints");
            // exampleCheckpointSetting = builder.define("exampleCheckpoint", 100);
            // builder.pop(); // Pop "checkpoints"
        }
    }

    // Method to get the death chime setting
    public static String getDeathChime() {
        // It's good practice to check for null in case the config isn't loaded yet,
        // though FML usually ensures COMMON and its values are available after config load.
        if (COMMON != null && COMMON.deathChime != null) {
            return COMMON.deathChime.get();
        }
        return "CLASSIC"; // Fallback default if something goes wrong
    }

    // These event handlers are registered in MineZero.java to the modEventBus
    // They need to be static to be registered with Class::method syntax.
    public static void onLoad(final ModConfigEvent.Loading event) {
        // This method is called when your config file is loaded from disk.
        // The values in your COMMON object are automatically populated by FML.
        // You generally don't need to do anything here unless you want to:
        // 1. Log that the config loaded.
        // 2. Cache config values in other static fields (though direct access via COMMON.value.get() is often fine).
        // 3. Perform actions based on the initial config values.
        // For example, using the static logger from MineZero if it were public, or a local one:
        // MineZero.LOGGER.info("MineZero Common Config Loaded: " + event.getConfig().getFileName());
        System.out.println("MineZero Common Config Loaded: " + event.getConfig().getFileName()); // Simple stdout for now
    }

    public static void onReload(final ModConfigEvent.Reloading event) {
        // This method is called when your config file is reloaded (e.g., via /reload command if supported for your config type).
        // Similar to onLoad, values in COMMON are updated.
        // React here if game behavior needs to change immediately based on new config values.
        // MineZero.LOGGER.info("MineZero Common Config Reloaded: " + event.getConfig().getFileName());
        System.out.println("MineZero Common Config Reloaded: " + event.getConfig().getFileName());
    }


    // The old `loadConfig(ModConfig config)` method is generally not needed.
    // FML handles the process of loading the values from the file into your ModConfigSpec.ConfigValue objects.
    // The ModConfigEvent.Loading and ModConfigEvent.Reloading events are your hooks to react to these actions.
    /*
    public static void loadConfig(ModConfig config) {
        // This method (as you had it) isn't typically used for applying the config values.
        // That's done automatically by FML when it loads the ModConfig.
        // You'd use this if you were manually parsing a different config format, which isn't the case here.
    }
    */
}