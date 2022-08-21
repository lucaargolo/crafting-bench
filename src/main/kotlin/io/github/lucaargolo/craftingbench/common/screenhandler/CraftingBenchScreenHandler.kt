package io.github.lucaargolo.craftingbench.common.screenhandler

import io.github.lucaargolo.craftingbench.client.CraftingBenchClient
import io.github.lucaargolo.craftingbench.client.screen.CraftingBenchScreen
import io.github.lucaargolo.craftingbench.common.block.BlockCompendium
import io.github.lucaargolo.craftingbench.mixin.RecipeManagerInvoker
import io.github.lucaargolo.craftingbench.utils.SimpleCraftingInventory
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.CraftingInventory
import net.minecraft.inventory.CraftingResultInventory
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket
import net.minecraft.recipe.*
import net.minecraft.recipe.book.RecipeBookCategory
import net.minecraft.screen.AbstractRecipeScreenHandler
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.ScreenHandlerListener
import net.minecraft.screen.slot.CraftingResultSlot
import net.minecraft.screen.slot.Slot
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.registry.Registry
import net.minecraft.world.World
import kotlin.concurrent.thread

class CraftingBenchScreenHandler(syncId: Int, private val playerInventory: PlayerInventory, simpleCraftingInventory: SimpleInventory, val inventory: SimpleInventory, private val context: ScreenHandlerContext) : AbstractRecipeScreenHandler<CraftingInventory>(ScreenHandlerCompendium.CRAFTING_BENCH, syncId) {

    constructor(syncId: Int, playerInventory: PlayerInventory, context: ScreenHandlerContext): this(syncId, playerInventory, SimpleInventory(9), SimpleInventory(28), context)

    private val craftingInventory = SimpleCraftingInventory(this, 3, 3, simpleCraftingInventory)
    private val result = CraftingResultInventory()

    val craftableRecipes = mutableMapOf<Recipe<*>, List<Recipe<*>>>()
    val combinedInventory = object: Inventory {
        override fun clear() {
            playerInventory.clear()
            inventory.clear()
        }

        override fun size(): Int {
            return playerInventory.size()+inventory.size()
        }

        override fun isEmpty(): Boolean {
            return playerInventory.isEmpty && inventory.isEmpty
        }

        override fun getStack(slot: Int): ItemStack {
            return when {
                slot < playerInventory.size() -> playerInventory.getStack(slot)
                else -> inventory.getStack(slot-playerInventory.size())
            }
        }

        override fun removeStack(slot: Int, amount: Int): ItemStack {
            return when {
                slot < playerInventory.size() -> playerInventory.removeStack(slot, amount)
                else -> inventory.removeStack(slot-playerInventory.size(), amount)
            }
        }

        override fun removeStack(slot: Int): ItemStack {
            return when {
                slot < playerInventory.size() -> playerInventory.removeStack(slot)
                else -> inventory.removeStack(slot-playerInventory.size())
            }
        }

        override fun setStack(slot: Int, stack: ItemStack) {
            when {
                slot < playerInventory.size() -> playerInventory.setStack(slot, stack)
                else -> inventory.setStack(slot-playerInventory.size(), stack)
            }
        }

        override fun markDirty() {
            playerInventory.markDirty()
            inventory.markDirty()
        }

        override fun canPlayerUse(player: PlayerEntity): Boolean {
            return playerInventory.canPlayerUse(player) && inventory.canPlayerUse(player)
        }

    }

    init {
        addListener(object: ScreenHandlerListener {
            override fun onSlotUpdate(handler: ScreenHandler, slotId: Int, stack: ItemStack) {
                if(slotId in (1..9)) onContentChanged(null)
            }
            override fun onPropertyUpdate(handler: ScreenHandler?, property: Int, value: Int) = Unit
        })

        addSlot(CraftingResultSlot(playerInventory.player, craftingInventory, result, 0, 283 + 105, 35))

        repeat(3) { n ->
            repeat(3) { m ->
                addSlot(CraftingBenchSlot(craftingInventory, m + n * 3, 189 + 105 + m * 18, 17 + n * 18))
            }
        }

        repeat(4) { n ->
            repeat(7) { m ->
                addSlot(CraftingBenchSlot(inventory, m + n * 7, 184 + 105 + m * 18, 84 + n * 18))
            }
        }

        repeat(3) { n ->
            repeat(9) { m ->
                addSlot(CraftingBenchSlot(playerInventory, m + n * 9 + 9, 8 + 105 + m * 18, 84 + n * 18))
            }
        }

        repeat(9) { n ->
            addSlot(CraftingBenchSlot(playerInventory, n, 8 + 105 + n * 18, 142))
        }

        onContentChanged(null)
    }

    override fun updateSlotStacks(revision: Int, stacks: MutableList<ItemStack>?, cursorStack: ItemStack?) {
        super.updateSlotStacks(revision, stacks, cursorStack)
        val player = playerInventory.player
        if(player.world.isClient) {
            (playerInventory.player as? ClientPlayerEntity)?.also(::populateRecipes)
        }
    }

    fun populateRecipes(player: ClientPlayerEntity) {
        val fakeInventory = SimpleInventory(player.inventory.size()+inventory.size()+craftingInventory.size())
        repeat(player.inventory.size()) { slot ->
            fakeInventory.setStack(slot, player.inventory.getStack(slot).copy())
        }
        repeat(inventory.size()) { slot ->
            fakeInventory.setStack(player.inventory.size()+slot, inventory.getStack(slot).copy())
        }
        repeat(craftingInventory.size()) { slot ->
            fakeInventory.setStack(player.inventory.size()+inventory.size()+slot, craftingInventory.getStack(slot).copy())
        }
        val recipes = (player.networkHandler.recipeManager as RecipeManagerInvoker).invokeGetAllOfType(RecipeType.CRAFTING).values
        thread(name = "Populate-Recipes") {
            val newCraftableRecipes = populateRecipes(recipes, mutableMapOf(), listOf(), fakeInventory)
            val client = MinecraftClient.getInstance()
            client.execute {
                craftableRecipes.clear()
                newCraftableRecipes.forEach { (recipe, recipeHistory) ->
                    if(player.recipeBook.contains(recipe)) {
                        craftableRecipes[recipe] = recipeHistory
                    }
                }
                (client.currentScreen as? CraftingBenchScreen)?.init(client, client.window.scaledWidth, client.window.scaledHeight)
            }
        }
    }

    private fun populateRecipes(testRecipes: Iterable<Recipe<*>>, craftableRecipes: MutableMap<Recipe<*>, List<Recipe<*>>>, recipeHistory: List<Recipe<*>>, fakeInventory: SimpleInventory): MutableMap<Recipe<*>, List<Recipe<*>>> {
        val recipeFinder = RecipeMatcher()
        repeat(fakeInventory.size()) { slot ->
            recipeFinder.addUnenchantedInput(fakeInventory.getStack(slot))
        }
        val newCraftableRecipes = mutableSetOf<Recipe<*>>()
        testRecipes.forEach { recipe ->
            if(recipe is ShapedRecipe || recipe is ShapelessRecipe) {
                val matcher = recipeFinder.Matcher(recipe)
                if(matcher.match(1, null) && (!craftableRecipes.containsKey(recipe) || craftableRecipes[recipe]!!.size > recipeHistory.size)) {
                    craftableRecipes[recipe] = recipeHistory
                    newCraftableRecipes.add(recipe)
                }
            }
        }
        newCraftableRecipes.forEach { matchedRecipe ->
            CraftingBenchClient.recipeToNewRecipeTree.getOrDefault(matchedRecipe, mutableSetOf()).forEach outer@{ nextRecipe ->
                if(!craftableRecipes.containsKey(nextRecipe) || craftableRecipes[nextRecipe]!!.size < recipeHistory.size-2) {
                    CraftingBenchClient.newRecipeToRecipeTree.getOrDefault(nextRecipe, mutableMapOf()).forEach inner@{ (requiredRecipe, qnt) ->
                        val matcher = recipeFinder.Matcher(requiredRecipe)
                        if (matcher.match(qnt, null)) {
                            val newFakeInventory = SimpleInventory(fakeInventory.size())
                            repeat(newFakeInventory.size()) { slot ->
                                newFakeInventory.setStack(slot, fakeInventory.getStack(slot).copy())
                            }
                            var recipeComplete = true
                            repeat(qnt) {
                                val missingIngredients = matchedRecipe.ingredients.toMutableList()
                                matcher.requiredItems.forEach { itemId ->
                                    val missingIngredientsIterator = missingIngredients.iterator()
                                    while (missingIngredientsIterator.hasNext()) {
                                        val missingIngredient = missingIngredientsIterator.next()
                                        if (missingIngredient.matchingItemIds.contains(itemId) && newFakeInventory.removeItem(Registry.ITEM.get(itemId), 1).count == 1) {
                                            missingIngredientsIterator.remove()
                                        }
                                    }
                                }
                                if(missingIngredients.any { it != Ingredient.EMPTY }) {
                                    recipeComplete = false
                                }else {
                                    newFakeInventory.addStack(matchedRecipe.output)
                                }
                            }
                            if(recipeComplete) {
                                val innerNewRecipeHistory = recipeHistory.toMutableList()
                                repeat(qnt) {
                                    innerNewRecipeHistory.add(matchedRecipe)
                                }
                                populateRecipes(CraftingBenchClient.recipeToNewRecipeTree.getOrDefault(requiredRecipe, mutableSetOf()), craftableRecipes, innerNewRecipeHistory, newFakeInventory)
                            }
                        }
                    }
                }
            }
        }
        return craftableRecipes
    }

    private fun updateResult(handler: ScreenHandler, world: World, player: PlayerEntity, craftingInventory: CraftingInventory, resultInventory: CraftingResultInventory) {
        if (!world.isClient) {
            val serverPlayerEntity = player as? ServerPlayerEntity
            var itemStack = ItemStack.EMPTY
            val optional = serverPlayerEntity?.server?.recipeManager?.getFirstMatch(RecipeType.CRAFTING, craftingInventory, world)
            if (optional?.isPresent == true) {
                val craftingRecipe = optional.get()
                if (resultInventory.shouldCraftRecipe(world, serverPlayerEntity, craftingRecipe)) {
                    itemStack = craftingRecipe.craft(craftingInventory)
                }
            }
            resultInventory.setStack(0, itemStack)
            handler.setPreviousTrackedSlot(0, itemStack)
            serverPlayerEntity?.networkHandler?.sendPacket(ScreenHandlerSlotUpdateS2CPacket(handler.syncId, handler.nextRevision(), 0, itemStack))
        }
    }

    override fun onContentChanged(inventory: Inventory?) {
        context.run { world, _ ->
            updateResult(this, world, playerInventory.player, craftingInventory, result)
        }
    }

    override fun transferSlot(player: PlayerEntity?, index: Int): ItemStack {
        var itemStack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot.hasStack()) {
            val itemStack2 = slot.stack
            itemStack = itemStack2.copy()
            if (index == 0) {
                context.run { world, _ -> itemStack2.item.onCraft(itemStack2, world, player) }
                if (!insertItem(itemStack2, 10, slots.size, true)) {
                    return ItemStack.EMPTY
                }
                slot.onQuickTransfer(itemStack2, itemStack)
            }else if(index < 10) {
                if (!insertItem(itemStack2, 10, slots.size, true)) {
                    return ItemStack.EMPTY
                }
            }else if (index < 38) {
                if (!insertItem(itemStack2, 38, slots.size, true)) {
                    return ItemStack.EMPTY
                }
            } else if (!insertItem(itemStack2, 10, 38, false)) {
                return ItemStack.EMPTY
            }
            if (itemStack2.isEmpty) {
                slot.stack = ItemStack.EMPTY
            } else {
                slot.markDirty()
            }

            if (itemStack2.count == itemStack.count) {
                return ItemStack.EMPTY
            }

            slot.onTakeItem(player, itemStack2)
            if (index == 0) {
                player?.dropItem(itemStack2, false)
            }
        }
        return itemStack
    }

    override fun canUse(player: PlayerEntity): Boolean {
        return canUse(context, player, BlockCompendium.CRAFTING_BENCH)
    }

    override fun populateRecipeFinder(finder: RecipeMatcher) {
        repeat(inventory.size()) { slot ->
            finder.addUnenchantedInput(inventory.getStack(slot))
        }
        craftingInventory.provideRecipeInputs(finder)
    }

    override fun clearCraftingSlots() {
        craftingInventory.clear()
        result.clear()
    }

    override fun matches(recipe: Recipe<in CraftingInventory?>): Boolean {
        return recipe.matches(craftingInventory, playerInventory.player.world)
    }
    
    override fun getCraftingResultSlotIndex(): Int {
        return 0
    }

    override fun getCraftingWidth(): Int {
        return craftingInventory.width
    }

    override fun getCraftingHeight(): Int {
        return craftingInventory.height
    }

    override fun getCraftingSlotCount(): Int {
        return 10
    }

    override fun getCategory(): RecipeBookCategory? {
        return RecipeBookCategory.CRAFTING
    }

    override fun canInsertIntoSlot(index: Int): Boolean {
        return index != craftingResultSlotIndex
    }

    inner class CraftingBenchSlot(inventory: Inventory?, index: Int, x: Int, y: Int) : Slot(inventory, index, x, y) {
        override fun markDirty() {
            if(playerInventory.player.world.isClient) {
                val client = MinecraftClient.getInstance()
                (client.currentScreen as? CraftingBenchScreen)?.updateRequiredItems()
            }
            super.markDirty()
        }
    }

}