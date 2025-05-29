package boomcow.minezero.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
// NeoForge specific imports for keybinding context and modifiers
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.neoforged.neoforge.common.util.Lazy; // NeoForge's Lazy utility

import org.lwjgl.glfw.GLFW;

public class KeyBindings {

    public static final String KEY_CATEGORY_MINEZERO = "key.categories.minezero";

    // Use Lazy for initialization as per NeoForge documentation
    public static final Lazy<KeyMapping> EXAMPLE_ACTION_KEY = Lazy.of(() -> new KeyMapping(
            "key.minezero.example_action", // name
            KeyConflictContext.IN_GAME,    // IKeyConflictContext
            KeyModifier.NONE,              // KeyModifier
            InputConstants.Type.KEYSYM,    // Input Type
            GLFW.GLFW_KEY_O,               // Default Key Code
            KEY_CATEGORY_MINEZERO          // Category
    ));

    public static final Lazy<KeyMapping> SELF_DAMAGE_KEY = Lazy.of(() -> new KeyMapping(
            "key.minezero.self_damage",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R, // Assuming R is the intended key
            KEY_CATEGORY_MINEZERO
    ));
}