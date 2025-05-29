package boomcow.minezero.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {

    public static final String KEY_CATEGORY_MINEZERO = "key.categories.minezero";

    // Existing Keybind
    public static final String KEY_EXAMPLE_ACTION = "key.minezero.example_action";
    public static final KeyMapping EXAMPLE_ACTION_KEY = new KeyMapping(
            KEY_EXAMPLE_ACTION,
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_O, // Default: O
            KEY_CATEGORY_MINEZERO
    );

    // --- New Keybind ---
    public static final String KEY_SELF_DAMAGE = "key.minezero.self_damage";
    public static final KeyMapping SELF_DAMAGE_KEY = new KeyMapping(
            KEY_SELF_DAMAGE,
            KeyConflictContext.IN_GAME, // Only active in game
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_P, // Default: P
            KEY_CATEGORY_MINEZERO
    );
    // --- End New Keybind ---
}