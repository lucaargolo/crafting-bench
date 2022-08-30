package io.github.lucaargolo.craftingbench.mixin;

import io.github.lucaargolo.craftingbench.CraftingBench;
import io.github.lucaargolo.craftingbench.common.screenhandler.CraftingBenchScreenHandler;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.InputSlotFiller;
import net.minecraft.recipe.Recipe;
import net.minecraft.screen.AbstractRecipeScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InputSlotFiller.class)
public class InputSlotFilterMixin<C extends Inventory> {

    @Shadow protected AbstractRecipeScreenHandler<C> handler;

    @Inject(at = @At("HEAD"), method = "fillInputSlots(Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/recipe/Recipe;Z)V")
    public void unlockUnknownRecipe(ServerPlayerEntity entity, Recipe<C> recipe, boolean craftAll, CallbackInfo ci) {
        if(handler instanceof CraftingBenchScreenHandler) {
            CraftingBench.INSTANCE.setUnlockUnknownRecipes(true);
        }
    }

    @Inject(at = @At("HEAD"), method = "fillInputSlot", cancellable = true)
    public void fillInputSlotUsingBenchContents(Slot slot, ItemStack stack, CallbackInfo ci) {
        boolean changedSlot = false;
        if(handler instanceof CraftingBenchScreenHandler) {
            SimpleInventory inventory = ((CraftingBenchScreenHandler) handler).getInventory();

            boolean found = false;
            int i;
            for(i = 0; i < inventory.size(); ++i) {
                ItemStack itemStack = inventory.getStack(i);
                if (!inventory.getStack(i).isEmpty() && ItemStack.canCombine(stack, inventory.getStack(i)) && !inventory.getStack(i).isDamaged() && !itemStack.hasEnchantments() && !itemStack.hasCustomName()) {
                    found = true;
                    break;
                }
            }
            if(found) {
                ItemStack itemStack = inventory.getStack(i).copy();
                if (!itemStack.isEmpty()) {
                    if (itemStack.getCount() > 1) {
                        inventory.removeStack(i, 1);
                    } else {
                        inventory.removeStack(i);
                    }

                    itemStack.setCount(1);
                    if (slot.getStack().isEmpty()) {
                        slot.setStack(itemStack);
                    } else {
                        slot.getStack().increment(1);
                    }
                    changedSlot = true;
                }
            }
        }
        if(changedSlot) {
            ci.cancel();
        }
    }


}
