package boomcow.minezero;

import net.minecraft.world.GameRules;

public class ModGameRules {

    // Rule Names (Keys)
    public static final String AUTO_CHECKPOINT_ENABLED = "autoCheckpointEnabled";
    public static final String CHECKPOINT_FIXED_INTERVAL = "checkpointFixedInterval";
    public static final String USE_RANDOM_INTERVAL = "useRandomCheckpointInterval";
    public static final String RANDOM_CHECKPOINT_LOWER_BOUND = "randomCheckpointLowerBound";
    public static final String RANDOM_CHECKPOINT_UPPER_BOUND = "randomCheckpointUpperBound";
    public static final String FLUTE_COOLDOWN_ENABLED = "fluteCooldownEnabled";
    public static final String FLUTE_COOLDOWN_DURATION = "fluteCooldownDuration";
    public static final String ARTIFACT_FLUTE_ENABLED = "artifactFluteEnabled";
    public static final String SET_CHECKPOINT_ON_WORLD_CREATION = "setCheckpointOnWorldCreation";

    /**
     * Registers the custom GameRules with the server.
     * This MUST be called during the FMLServerStartingEvent in your main class.
     *
     * @param rules The GameRules instance from the Overworld (server.getWorld(0).getGameRules())
     */
    public static void register(GameRules rules) {
        addRule(rules, AUTO_CHECKPOINT_ENABLED, "true", GameRules.ValueType.BOOLEAN_VALUE);
        addRule(rules, CHECKPOINT_FIXED_INTERVAL, "600", GameRules.ValueType.NUMERICAL_VALUE);
        addRule(rules, USE_RANDOM_INTERVAL, "false", GameRules.ValueType.BOOLEAN_VALUE);
        addRule(rules, RANDOM_CHECKPOINT_LOWER_BOUND, "600", GameRules.ValueType.NUMERICAL_VALUE);
        addRule(rules, RANDOM_CHECKPOINT_UPPER_BOUND, "1200", GameRules.ValueType.NUMERICAL_VALUE);
        addRule(rules, FLUTE_COOLDOWN_ENABLED, "true", GameRules.ValueType.BOOLEAN_VALUE);
        addRule(rules, FLUTE_COOLDOWN_DURATION, "60", GameRules.ValueType.NUMERICAL_VALUE);
        addRule(rules, ARTIFACT_FLUTE_ENABLED, "true", GameRules.ValueType.BOOLEAN_VALUE);
        addRule(rules, SET_CHECKPOINT_ON_WORLD_CREATION, "true", GameRules.ValueType.BOOLEAN_VALUE);
    }

    /**
     * Helper to add a rule only if it doesn't exist (prevents overwriting saved values).
     */
    private static void addRule(GameRules rules, String key, String defaultValue, GameRules.ValueType type) {
        if (!rules.hasRule(key)) {
            rules.addGameRule(key, defaultValue, type);
        }
    }
}