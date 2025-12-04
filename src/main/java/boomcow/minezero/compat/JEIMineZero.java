package boomcow.minezero.compat;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin; // FIX: Change JeiPlugin to JEIPlugin
import mezz.jei.api.recipe.IRecipeCategoryRegistration;

// FIX: Change @JeiPlugin to @JEIPlugin
@JEIPlugin
public class JEIMineZero implements IModPlugin {

    @Override
    public void registerCategories(IRecipeCategoryRegistration registry) {
        // Register categories here
    }

    @Override
    public void register(IModRegistry registry) {
        // Register recipes here
        // Example: registry.handleRecipes(MyRecipe.class, recipe -> new MyRecipeWrapper(recipe), MyRecipeCategory.UID);
    }
}