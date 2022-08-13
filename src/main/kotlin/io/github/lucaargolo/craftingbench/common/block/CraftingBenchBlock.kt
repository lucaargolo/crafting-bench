package io.github.lucaargolo.craftingbench.common.block

import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.client.recipebook.ClientRecipeBook
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.SimpleInventory
import net.minecraft.recipe.Recipe
import net.minecraft.recipe.RecipeMatcher
import net.minecraft.recipe.RecipeType
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.Identifier
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.MathHelper
import net.minecraft.util.registry.Registry
import net.minecraft.world.World
import kotlin.math.min

class CraftingBenchBlock(settings: Settings) : Block(settings) {

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java", ReplaceWith("super.onUse(state, world, pos, player, hand, hit)", "net.minecraft.block.Block"))
    override fun onUse(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity, hand: Hand, hit: BlockHitResult): ActionResult {
        if(world.isClient) {
            val clientPlayer = player as? ClientPlayerEntity ?: return ActionResult.FAIL

            val recipeBook = clientPlayer.recipeBook
            val fakeInventory = SimpleInventory(player.inventory.size())
            repeat(player.inventory.size()) { slot ->
                fakeInventory.setStack(slot, player.inventory.getStack(slot).copy())
            }
            val craftableRecipes: MutableMap<Recipe<*>, List<Recipe<*>>> = mutableMapOf()
            populateRecipes(recipeBook, craftableRecipes, listOf(), fakeInventory, 0)
            craftableRecipes.forEach { (recipe, recipeHistory) ->
                println(recipe.id)
            }
        }
        return ActionResult.SUCCESS
    }

    private fun populateRecipes(recipeBook: ClientRecipeBook, craftableRecipes: MutableMap<Recipe<*>, List<Recipe<*>>>, recipeHistory: List<Recipe<*>>, fakeInventory: SimpleInventory, depth: Int) {
        if(depth > 5) {
            return
        }
        val recipeFinder = RecipeMatcher()
        repeat(fakeInventory.size()) { slot ->
            recipeFinder.addUnenchantedInput(fakeInventory.getStack(slot))
        }
        val list = recipeBook.orderedResults
        list.forEach { resultCollection ->
            resultCollection.allRecipes.forEach { recipe ->
                val matcher = recipeFinder.Matcher(recipe)
                //TODO: Substituir o recipe se o depth for menor
                if(recipe.type == RecipeType.CRAFTING && matcher.match(1, null)) {
                    if(!craftableRecipes.containsKey(recipe) || craftableRecipes.get(recipe)!!.size > recipeHistory.size) {
                        craftableRecipes.put(recipe, recipeHistory)
                        //Uma nova recipe
                        val outputCount = recipe.output.count
                        //How many should we craft?
                        val howMany = min(matcher.maximumCrafts, MathHelper.ceil(outputCount / 9f))
                        repeat(howMany) { times ->
                            val newFakeInventory = SimpleInventory(fakeInventory.size())
                            //TODO: this loops can be optimized
                            repeat(newFakeInventory.size()) { slot ->
                                newFakeInventory.setStack(slot, fakeInventory.getStack(slot).copy())
                            }
                            val missingIngredients = recipe.ingredients.toMutableList()
                            repeat(times+1) {
                                matcher.requiredItems.forEach { itemId ->
                                    val missingIngredientsIterator = missingIngredients.iterator()
                                    while (missingIngredientsIterator.hasNext()) {
                                        val missingIngredient = missingIngredientsIterator.next()
                                        if (missingIngredient.matchingItemIds.contains(itemId) && newFakeInventory.removeItem(Registry.ITEM.get(itemId), 1).count == 1) {
                                            missingIngredientsIterator.remove()
                                        }
                                    }
                                }
                            }
                            newFakeInventory.addStack(recipe.output)
                            val newRecipeHistory = recipeHistory.toMutableList()
                            repeat(times + 1) {
                                newRecipeHistory.add(recipe)
                            }
                            populateRecipes(recipeBook, craftableRecipes, newRecipeHistory, newFakeInventory, depth + 1)
                        }
                    }
                }
            }
        }

    }


}