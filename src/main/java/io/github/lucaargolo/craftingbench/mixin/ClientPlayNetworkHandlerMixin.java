package io.github.lucaargolo.craftingbench.mixin;

import io.github.lucaargolo.craftingbench.client.CraftingBenchClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.SynchronizeRecipesS2CPacket;
import net.minecraft.recipe.RecipeManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {

    @Shadow @Final private RecipeManager recipeManager;

    @Inject(at = @At("TAIL"), method = "onSynchronizeRecipes")
    public void onSynchronizeRecipes(SynchronizeRecipesS2CPacket packet, CallbackInfo ci) {
        CraftingBenchClient.INSTANCE.onSynchronizeRecipes(recipeManager);
    }

}
