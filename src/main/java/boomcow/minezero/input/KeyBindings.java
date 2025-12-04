package boomcow.minezero.input;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.input.Keyboard;

public class KeyBindings {

    public static final String KEY_CATEGORY_MINEZERO = "key.categories.minezero";
    public static final String KEY_EXAMPLE_ACTION = "key.minezero.example_action";

    // 1.12.2 uses KeyBinding, and Keyboard.KEY_* for key codes
    public static final KeyBinding EXAMPLE_ACTION_KEY = new KeyBinding(
            KEY_EXAMPLE_ACTION,
            KeyConflictContext.IN_GAME,
            Keyboard.KEY_O,
            KEY_CATEGORY_MINEZERO
    );

    public static final String KEY_SELF_DAMAGE = "key.minezero.self_damage";

    public static final KeyBinding SELF_DAMAGE_KEY = new KeyBinding(
            KEY_SELF_DAMAGE,
            KeyConflictContext.IN_GAME,
            Keyboard.KEY_P,
            KEY_CATEGORY_MINEZERO
    );
}