package io.github.lucaargolo.craftingbench.mixin;

import io.github.lucaargolo.craftingbench.client.CraftingBenchClient;
import net.minecraft.client.recipebook.ClientRecipeBook;
import net.minecraft.recipe.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientRecipeBook.class)
public abstract class ClientRecipeBookMixin {

    @Inject(at = @At("HEAD"), method = "reload")
    public void handleRecipeBookReload(Iterable<Recipe<?>> recipes, CallbackInfo ci) {
        CraftingBenchClient.INSTANCE.onRecipeBookReload(recipes);
    }

}
