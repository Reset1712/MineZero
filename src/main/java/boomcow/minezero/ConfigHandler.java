package boomcow.minezero;

import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class ConfigHandler {
    public static final ModConfigSpec COMMON_CONFIG_SPEC;
    public static final CommonConfig COMMON;

    static {
        Pair<CommonConfig, ModConfigSpec> commonPair = new ModConfigSpec.Builder().configure(CommonConfig::new);
        COMMON_CONFIG_SPEC = commonPair.getRight();
        COMMON = commonPair.getLeft();
    }

    public static class CommonConfig {
        public final ModConfigSpec.ConfigValue<String> deathChime;

        public CommonConfig(ModConfigSpec.Builder builder) {
            builder.comment("General settings").push("general");

            deathChime = builder
                    .comment("Death Chime Options: CLASSIC, ALTERNATE. Default: CLASSIC")
                    .define("deathChime", "CLASSIC");

            builder.pop();
        }
    }
    public static String getDeathChime() {
        if (COMMON != null && COMMON.deathChime != null) {
            return COMMON.deathChime.get();
        }
        return "CLASSIC";
    }
    public static void onLoad(final ModConfigEvent.Loading event) {
        System.out.println("MineZero Common Config Loaded: " + event.getConfig().getFileName());
    }

    public static void onReload(final ModConfigEvent.Reloading event) {
        System.out.println("MineZero Common Config Reloaded: " + event.getConfig().getFileName());
    }

}