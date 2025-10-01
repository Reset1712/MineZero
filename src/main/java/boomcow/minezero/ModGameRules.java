package boomcow.minezero;

import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameRules.BooleanValue;
import net.minecraft.world.level.GameRules.IntegerValue;

public class ModGameRules {

    public static final GameRules.Key<BooleanValue> AUTO_CHECKPOINT_ENABLED =
            GameRules.register("autoCheckpointEnabled", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));

    public static final GameRules.Key<IntegerValue> CHECKPOINT_FIXED_INTERVAL =
            GameRules.register("checkpointFixedInterval", GameRules.Category.PLAYER, GameRules.IntegerValue.create(600));

    public static final GameRules.Key<BooleanValue> USE_RANDOM_INTERVAL =
            GameRules.register("useRandomCheckpointInterval", GameRules.Category.PLAYER, GameRules.BooleanValue.create(false));

    public static final GameRules.Key<IntegerValue> RANDOM_CHECKPOINT_LOWER_BOUND =
            GameRules.register("randomCheckpointLowerBound", GameRules.Category.PLAYER, GameRules.IntegerValue.create(600));

    public static final GameRules.Key<IntegerValue> RANDOM_CHECKPOINT_UPPER_BOUND =
            GameRules.register("randomCheckpointUpperBound", GameRules.Category.PLAYER, GameRules.IntegerValue.create(1200));
    public static final GameRules.Key<BooleanValue> FLUTE_COOLDOWN_ENABLED =
            GameRules.register("fluteCooldownEnabled", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));
    public static final GameRules.Key<IntegerValue> FLUTE_COOLDOWN_DURATION =
            GameRules.register("fluteCooldownDuration", GameRules.Category.PLAYER, GameRules.IntegerValue.create(60));
    public static final GameRules.Key<BooleanValue> ARTIFACT_FLUTE_ENABLED =
            GameRules.register("artifactFluteEnabled", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));
    public static final GameRules.Key<BooleanValue> SET_CHECKPOINT_ON_WORLD_CREATION =
            GameRules.register("setCheckpointOnWorldCreation", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));
}
