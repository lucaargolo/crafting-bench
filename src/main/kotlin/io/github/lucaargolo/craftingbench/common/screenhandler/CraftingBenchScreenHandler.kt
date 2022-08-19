package io.github.lucaargolo.craftingbench.common.screenhandler

import io.github.lucaargolo.craftingbench.client.CraftingBenchClient
import io.github.lucaargolo.craftingbench.client.screen.CraftingBenchScreen
import io.github.lucaargolo.craftingbench.common.block.BlockCompendium
import io.github.lucaargolo.craftingbench.utils.SimpleCraftingInventory
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.client.recipebook.ClientRecipeBook
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.CraftingInventory
import net.minecraft.inventory.CraftingResultInventory
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket
import net.minecraft.recipe.Ingredient
import net.minecraft.recipe.Recipe
import net.minecraft.recipe.RecipeMatcher
import net.minecraft.recipe.RecipeType
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.CraftingResultSlot
import net.minecraft.screen.slot.Slot
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.registry.Registry
import net.minecraft.world.World
import kotlin.concurrent.thread
import kotlin.system.measureTimeMillis

class CraftingBenchScreenHandler(syncId: Int, private val playerInventory: PlayerInventory, simpleCraftingInventory: SimpleInventory, private val inventory: SimpleInventory, private val context: ScreenHandlerContext) : ScreenHandler(ScreenHandlerCompendium.CRAFTING_BENCH, syncId) {

    constructor(syncId: Int, playerInventory: PlayerInventory, context: ScreenHandlerContext): this(syncId, playerInventory, SimpleInventory(9), SimpleInventory(28), context)

    private val craftingInventory = SimpleCraftingInventory(this, 3, 3, simpleCraftingInventory)
    private val result = CraftingResultInventory()

    val craftableRecipes = mutableMapOf<Recipe<*>, List<Recipe<*>>>()
    val combinedInventory = object: Inventory {
        override fun clear() {
            playerInventory.clear()
            inventory.clear()
            craftingInventory.clear()
        }

        override fun size(): Int {
            return playerInventory.size()+inventory.size()+craftingInventory.size()
        }

        override fun isEmpty(): Boolean {
            return playerInventory.isEmpty && inventory.isEmpty && craftingInventory.isEmpty
        }

        override fun getStack(slot: Int): ItemStack {
            return when {
                slot < playerInventory.size() -> playerInventory.getStack(slot)
                slot < playerInventory.size()+inventory.size() -> inventory.getStack(slot)
                else -> craftingInventory.getStack(slot)
            }
        }

        override fun removeStack(slot: Int, amount: Int): ItemStack {
            return when {
                slot < playerInventory.size() -> playerInventory.removeStack(slot, amount)
                slot < playerInventory.size()+inventory.size() -> inventory.removeStack(slot, amount)
                else -> craftingInventory.removeStack(slot, amount)
            }
        }

        override fun removeStack(slot: Int): ItemStack {
            return when {
                slot < playerInventory.size() -> playerInventory.removeStack(slot)
                slot < playerInventory.size()+inventory.size() -> inventory.removeStack(slot)
                else -> craftingInventory.removeStack(slot)
            }
        }

        override fun setStack(slot: Int, stack: ItemStack) {
            when {
                slot < playerInventory.size() -> playerInventory.setStack(slot, stack)
                slot < playerInventory.size()+inventory.size() -> inventory.setStack(slot, stack)
                else -> craftingInventory.setStack(slot, stack)
            }
        }

        override fun markDirty() {
            playerInventory.markDirty()
            inventory.markDirty()
            craftingInventory.markDirty()
        }

        override fun canPlayerUse(player: PlayerEntity): Boolean {
            return playerInventory.canPlayerUse(player) && inventory.canPlayerUse(player) && craftingInventory.canPlayerUse(player)
        }

    }
    val recipeFinder: RecipeMatcher by lazy {
        RecipeMatcher().also {
            repeat(combinedInventory.size()) { slot ->
                it.addUnenchantedInput(combinedInventory.getStack(slot))
            }
        }
    }


    var hasCraft = false

    init {
        addSlot(CraftingResultSlot(playerInventory.player, craftingInventory, result, 0, 283+105, 35))

        repeat(3) { n ->
            repeat(3) { m ->
                addSlot(Slot(craftingInventory, m + n * 3, 189 + 105 + m * 18, 17 + n * 18))
            }
        }

        repeat(3) { n ->
            repeat(9) { m ->
                addSlot(Slot(playerInventory, m + n * 9 + 9, 8 + 105 + m * 18, 84 + n*18))
            }
        }

        repeat(9) { n ->
            addSlot(Slot(playerInventory, n, 8 + 105 + n * 18, 142))
        }

        repeat(4) { n ->
            repeat(7) { m ->
                addSlot(Slot(inventory, m + n * 7, 184 + 105 + m * 18, 84 + n*18))
            }
        }

        onContentChanged(null)
        (playerInventory.player as? ClientPlayerEntity)?.also(::populateRecipes)
    }

    private fun populateRecipes(player: ClientPlayerEntity) {
        val recipeBook = player.recipeBook
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
        val allRecipes = mutableSetOf<Recipe<*>>()
        recipeBook.orderedResults.forEach { resultCollection ->
            resultCollection.allRecipes.forEach { recipe ->
                if (recipe.type == RecipeType.CRAFTING) {
                    allRecipes.add(recipe)
                }
            }
        }
        thread {
            val time = measureTimeMillis {
                val newCraftableRecipes = populateRecipes(allRecipes, recipeBook, mutableMapOf(), listOf(), fakeInventory)
                val client = MinecraftClient.getInstance()
                client.execute {
                    craftableRecipes.clear()
                    craftableRecipes.putAll(newCraftableRecipes)
                    (client.currentScreen as? CraftingBenchScreen)?.init(client, client.window.scaledWidth, client.window.scaledHeight)
                }
            }
            println("Whatever this is took $time ms")
        }
    }

    private fun populateRecipes(testRecipes: Iterable<Recipe<*>>, recipeBook: ClientRecipeBook, craftableRecipes: MutableMap<Recipe<*>, List<Recipe<*>>>, recipeHistory: List<Recipe<*>>, fakeInventory: SimpleInventory): MutableMap<Recipe<*>, List<Recipe<*>>> {
        val recipeFinder = RecipeMatcher()
        repeat(fakeInventory.size()) { slot ->
            recipeFinder.addUnenchantedInput(fakeInventory.getStack(slot))
        }
        val newCraftableRecipes = mutableSetOf<Recipe<*>>()
        testRecipes.forEach { recipe ->
            if(recipe.type == RecipeType.CRAFTING) {
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
                    val outerNewFakeInventory = SimpleInventory(fakeInventory.size())
                    repeat(outerNewFakeInventory.size()) { slot ->
                        outerNewFakeInventory.setStack(slot, fakeInventory.getStack(slot).copy())
                    }
                    val outerNewRecipeHistory = recipeHistory.toMutableList()
                    CraftingBenchClient.newRecipeToRecipeTree.getOrDefault(nextRecipe, mutableMapOf()).forEach inner@{ (requiredRecipe, qnt) ->
                        val matcher = recipeFinder.Matcher(requiredRecipe)
                        if (matcher.match(qnt, null)) {
                            val innerNewFakeInventory = SimpleInventory(fakeInventory.size())
                            repeat(innerNewFakeInventory.size()) { slot ->
                                innerNewFakeInventory.setStack(slot, fakeInventory.getStack(slot).copy())
                            }
                            val outerHoldingStacks = mutableListOf<ItemStack>()
                            var outerRecipeComplete = true
                            var innerRecipeComplete = true
                            repeat(qnt) {
                                val outerMissingIngredients = matchedRecipe.ingredients.toMutableList()
                                val innerMissingIngredients = matchedRecipe.ingredients.toMutableList()
                                matcher.requiredItems.forEach { itemId ->
                                    val innerMissingIngredientsIterator = innerMissingIngredients.iterator()
                                    while (innerMissingIngredientsIterator.hasNext()) {
                                        val missingIngredient = innerMissingIngredientsIterator.next()
                                        if (missingIngredient.matchingItemIds.contains(itemId) && innerNewFakeInventory.removeItem(Registry.ITEM.get(itemId), 1).count == 1) {
                                            innerMissingIngredientsIterator.remove()
                                        }
                                    }
                                    val outerMissingIngredientsIterator = outerMissingIngredients.iterator()
                                    while (outerMissingIngredientsIterator.hasNext()) {
                                        val missingIngredient = outerMissingIngredientsIterator.next()
                                        if (missingIngredient.matchingItemIds.contains(itemId) && outerNewFakeInventory.removeItem(Registry.ITEM.get(itemId), 1).count == 1) {
                                            outerMissingIngredientsIterator.remove()
                                            outerHoldingStacks.add(ItemStack(Registry.ITEM.get(itemId), 1))
                                        }
                                    }
                                }
                                if(innerMissingIngredients.any { it != Ingredient.EMPTY }) {
                                    innerRecipeComplete = false
                                }else {
                                    innerNewFakeInventory.addStack(matchedRecipe.output)
                                }
                                if(outerMissingIngredients.any { it != Ingredient.EMPTY }) {
                                    outerRecipeComplete = false
                                }else {
                                    outerNewFakeInventory.addStack(matchedRecipe.output)
                                }
                            }
                            if(outerRecipeComplete) {
                                repeat(qnt) {
                                    outerNewRecipeHistory.add(matchedRecipe)
                                }
                            }else{
                                outerHoldingStacks.forEach {
                                    outerNewFakeInventory.addStack(it)
                                }
                            }
                            if(innerRecipeComplete) {
                                val innerNewRecipeHistory = recipeHistory.toMutableList()
                                repeat(qnt) {
                                    innerNewRecipeHistory.add(matchedRecipe)
                                }
                                populateRecipes(CraftingBenchClient.recipeToNewRecipeTree.getOrDefault(requiredRecipe, mutableSetOf()), recipeBook, craftableRecipes, innerNewRecipeHistory, innerNewFakeInventory)
                            }
                        }
                    }
                    populateRecipes(listOf(nextRecipe), recipeBook, craftableRecipes, outerNewRecipeHistory, outerNewFakeInventory)
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
        return ItemStack.EMPTY
    }

    override fun canUse(player: PlayerEntity): Boolean {
        return canUse(context, player, BlockCompendium.CRAFTING_BENCH)
    }

}