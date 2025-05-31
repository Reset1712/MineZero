package boomcow.minezero.input; // Your package

import boomcow.minezero.MineZeroClient;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {

    public static final String KEY_CATEGORY_MINEZERO = "key.categories.minezero";

    // In Fabric, we directly define and initialize the KeyBinding objects.
    // They are then registered.

    public static final KeyBinding EXAMPLE_ACTION_KEY;
    public static final KeyBinding SELF_DAMAGE_KEY;

    static {
        // Initialize and register the key bindings
        // KeyBinding constructor: (translationKey, type, code, category)

        EXAMPLE_ACTION_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.minezero.example_action", // Translation key for the key's name
                InputUtil.Type.KEYSYM,         // Input Type (e.g., KEYSYM for keyboard, MOUSE for mouse buttons)
                GLFW.GLFW_KEY_O,               // Default key code
                KEY_CATEGORY_MINEZERO          // Category translation key
        ));

        SELF_DAMAGE_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.minezero.self_damage",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R, // Default key code
                KEY_CATEGORY_MINEZERO
        ));
    }

    /**
     * This method is called from your ClientModInitializer to ensure the static block runs
     * and registers the keybindings. Can also be named init() or similar.
     *
     * Note: Simply accessing one of the KeyBinding fields (e.g., in an event handler)
     * would also trigger the static initializer. However, having an explicit registration
     * call in your client initializer is good practice for clarity.
     */
    public static void registerKeyBindings() {
        // The static initializer block above handles the registration when this class is loaded.
        // This method effectively ensures the class is loaded.
        MineZeroClient.LOGGER.info("MineZero keybindings registered."); // Assuming MineZeroClient has a public static LOGGER
    }
}