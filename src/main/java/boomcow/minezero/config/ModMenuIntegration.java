package boomcow.minezero.config; // Or your preferred package

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import boomcow.minezero.ConfigHandler; // Your config handler class

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        // Return a factory that creates your config screen
        // The 'parent' screen is provided by Mod Menu
        return parent -> ConfigHandler.getClothConfigScreen(parent);
    }
}
