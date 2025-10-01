package boomcow.minezero.input;

import boomcow.minezero.MineZeroClient;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {

    public static final String KEY_CATEGORY_MINEZERO = "key.categories.minezero";

    public static final KeyBinding EXAMPLE_ACTION_KEY;
    public static final KeyBinding SELF_DAMAGE_KEY;

    static {

        EXAMPLE_ACTION_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.minezero.example_action",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_O,
                KEY_CATEGORY_MINEZERO
        ));

        SELF_DAMAGE_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.minezero.self_damage",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
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
        MineZeroClient.LOGGER.info("MineZero keybindings registered.");
    }
}