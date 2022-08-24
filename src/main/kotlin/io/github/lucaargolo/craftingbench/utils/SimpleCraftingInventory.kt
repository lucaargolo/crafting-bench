package io.github.lucaargolo.craftingbench.utils

import io.github.lucaargolo.craftingbench.CraftingBench.stacks
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.CraftingInventory
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.recipe.RecipeMatcher
import net.minecraft.screen.ScreenHandler

class SimpleCraftingInventory(private val handler: ScreenHandler, width: Int, height: Int, private val simpleInventory: SimpleInventory): CraftingInventory(handler, width, height) {

    override fun size(): Int {
        return simpleInventory.stacks.size
    }

    override fun isEmpty(): Boolean {
        val var1: Iterator<*> = simpleInventory.stacks.iterator()
        var itemStack: ItemStack
        do {
            if (!var1.hasNext()) {
                return true
            }
            itemStack = var1.next() as ItemStack
        } while (itemStack.isEmpty)
        return false
    }

    override fun getStack(slot: Int): ItemStack {
        return if (slot >= size()) ItemStack.EMPTY else simpleInventory.stacks[slot]
    }

    override fun removeStack(slot: Int): ItemStack {
        return Inventories.removeStack(simpleInventory.stacks, slot)
    }

    override fun removeStack(slot: Int, amount: Int): ItemStack {
        val itemStack = Inventories.splitStack(simpleInventory.stacks, slot, amount)
        if (!itemStack.isEmpty) {
            markDirty()
            handler.onContentChanged(this)
        }
        return itemStack
    }

    override fun setStack(slot: Int, stack: ItemStack) {
        simpleInventory.stacks[slot] = stack
        handler.onContentChanged(this)
        markDirty()
    }

    override fun markDirty() {
        simpleInventory.markDirty()
    }

    override fun canPlayerUse(player: PlayerEntity): Boolean {
        return true
    }

    override fun clear() {
        simpleInventory.stacks.clear()
        markDirty()
    }

    override fun provideRecipeInputs(finder: RecipeMatcher) {
        val var2: Iterator<*> = simpleInventory.stacks.iterator()
        while (var2.hasNext()) {
            val itemStack = var2.next() as ItemStack
            finder.addUnenchantedInput(itemStack)
        }
    }

}