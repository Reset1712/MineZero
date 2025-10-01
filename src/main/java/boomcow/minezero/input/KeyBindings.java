package boomcow.minezero.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.neoforged.neoforge.common.util.Lazy;

import org.lwjgl.glfw.GLFW;

public class KeyBindings {

    public static final String KEY_CATEGORY_MINEZERO = "key.categories.minezero";
    public static final Lazy<KeyMapping> EXAMPLE_ACTION_KEY = Lazy.of(() -> new KeyMapping(
            "key.minezero.example_action",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            KEY_CATEGORY_MINEZERO
    ));

    public static final Lazy<KeyMapping> SELF_DAMAGE_KEY = Lazy.of(() -> new KeyMapping(
            "key.minezero.self_damage",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            KEY_CATEGORY_MINEZERO
    ));
}