package boomcow.minezero;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;

public class ConfigHandler {

    public static final ForgeConfigSpec COMMON_CONFIG;
    public static final CommonConfig COMMON;

    static {
        Pair<CommonConfig, ForgeConfigSpec> commonPair = new ForgeConfigSpec.Builder().configure(CommonConfig::new);
        COMMON_CONFIG = commonPair.getRight();
        COMMON = commonPair.getLeft();
    }

    public static class CommonConfig {
        public final ForgeConfigSpec.ConfigValue<String> deathChime;

        public CommonConfig(ForgeConfigSpec.Builder builder) {
            builder.comment("General settings").push("general");

            deathChime = builder
                    .comment("Death Chime Options: CLASSIC, ALTERNATE")
                    .define("deathChime", "CLASSIC");

            builder.pop();

            builder.comment("Checkpoint settings").push("checkpoints");

            builder.pop();
        }
    }

    public static String getDeathChime() {
        return COMMON.deathChime.get();
    }

    public static void loadConfig(ModConfig config) {
    }
}
