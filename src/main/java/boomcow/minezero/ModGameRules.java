package boomcow.minezero;

import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameRules.BooleanRule;
import net.minecraft.world.GameRules.Category;
import net.minecraft.world.GameRules.IntRule;
import net.minecraft.world.GameRules.Key;

public class ModGameRules {
    public static final Key<BooleanRule> AUTO_CHECKPOINT_ENABLED =
            GameRuleRegistry.register(
                    Identifier.of(MineZeroMain.MOD_ID, "autoCheckpointEnabled").toString(),
                    Category.PLAYER,
                    GameRuleFactory.createBooleanRule(true)
            );

    public static final Key<IntRule> CHECKPOINT_FIXED_INTERVAL =
            GameRuleRegistry.register(
                    Identifier.of(MineZeroMain.MOD_ID, "checkpointFixedInterval").toString(),
                    Category.PLAYER,
                    GameRuleFactory.createIntRule(600, 0)
            );

    public static final Key<BooleanRule> USE_RANDOM_INTERVAL =
            GameRuleRegistry.register(
                    Identifier.of(MineZeroMain.MOD_ID, "useRandomCheckpointInterval").toString(),
                    Category.PLAYER,
                    GameRuleFactory.createBooleanRule(false)
            );

    public static final Key<IntRule> RANDOM_CHECKPOINT_LOWER_BOUND =
            GameRuleRegistry.register(
                    Identifier.of(MineZeroMain.MOD_ID, "randomCheckpointLowerBound").toString(),
                    Category.PLAYER,
                    GameRuleFactory.createIntRule(600, 0)
            );

    public static final Key<IntRule> RANDOM_CHECKPOINT_UPPER_BOUND =
            GameRuleRegistry.register(
                    Identifier.of(MineZeroMain.MOD_ID, "randomCheckpointUpperBound").toString(),
                    Category.PLAYER,
                    GameRuleFactory.createIntRule(1200, 1)
            );
    public static final Key<BooleanRule> FLUTE_COOLDOWN_ENABLED =
            GameRuleRegistry.register(
                    Identifier.of(MineZeroMain.MOD_ID, "fluteCooldownEnabled").toString(),
                    Category.PLAYER,
                    GameRuleFactory.createBooleanRule(true)
            );

    public static final Key<IntRule> FLUTE_COOLDOWN_DURATION =
            GameRuleRegistry.register(
                    Identifier.of(MineZeroMain.MOD_ID, "fluteCooldownDuration").toString(),
                    Category.PLAYER,
                    GameRuleFactory.createIntRule(60, 0)
            );

    public static final Key<BooleanRule> ARTIFACT_FLUTE_ENABLED =
            GameRuleRegistry.register(
                    Identifier.of(MineZeroMain.MOD_ID, "artifactFluteEnabled").toString(),
                    Category.PLAYER,
                    GameRuleFactory.createBooleanRule(true)
            );

    /**
     * This method should be called once during your mod's initialization (e.g., in onInitialize)
     * to ensure the game rules are registered.
     * <p>
     * Calling this method explicitly isn't strictly necessary if the static final fields
     * are accessed, as that will trigger the static initializers. However, for clarity
     * and to ensure registration happens, it's good practice to have an explicit init method.
     * <p>
     * Update: With Fabric API's GameRuleRegistry, the static final initializers are the
     * registration itself. So, simply loading this class (e.g., by referencing one of its
     * fields from your main initializer) is enough. No explicit `init()` call is needed.
     */
    public static void initialize() {
        MineZeroMain.LOGGER.info("MineZero game rules registered.");
    }
}