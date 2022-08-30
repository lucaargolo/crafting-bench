package io.github.lucaargolo.craftingbench.mixin;

import io.github.lucaargolo.craftingbench.CraftingBench;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.book.RecipeBook;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RecipeBook.class)
public class RecipeBookMixin {

    @Inject(at = @At("HEAD"), method = "contains(Lnet/minecraft/recipe/Recipe;)Z", cancellable = true)
    public void findUnknownRecipes(@Nullable Recipe<?> recipe, CallbackInfoReturnable<Boolean> cir) {
        if(recipe != null && CraftingBench.INSTANCE.getUnlockUnknownRecipes()) {
            CraftingBench.INSTANCE.setUnlockUnknownRecipes(false);
            cir.setReturnValue(true);
        }
    }

    @Inject(at = @At("HEAD"), method = "contains(Lnet/minecraft/util/Identifier;)Z", cancellable = true)
    public void findUnknownRecipes(Identifier id, CallbackInfoReturnable<Boolean> cir) {
        if(CraftingBench.INSTANCE.getUnlockUnknownRecipes()) {
            CraftingBench.INSTANCE.setUnlockUnknownRecipes(false);
            cir.setReturnValue(true);
        }
    }

}
