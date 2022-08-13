package io.github.lucaargolo.craftingbench.common.block

import io.github.lucaargolo.craftingbench.client.CraftingBenchClient
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
            val allRecipes = mutableSetOf<Recipe<*>>()
            recipeBook.orderedResults.forEach { resultCollection ->
                resultCollection.allRecipes.forEach { recipe ->
                    if (recipe.type == RecipeType.CRAFTING) {
                        allRecipes.add(recipe)
                    }
                }
            }
            val craftableRecipes = populateRecipes(allRecipes, recipeBook, mutableMapOf(), listOf(), fakeInventory, 0)
            craftableRecipes.forEach { (recipe, recipeHistory) ->
                println(recipe.id)
            }
        }
        return ActionResult.SUCCESS
    }

    private fun populateRecipes(testRecipes: Iterable<Recipe<*>>, recipeBook: ClientRecipeBook, craftableRecipes: MutableMap<Recipe<*>, List<Recipe<*>>>, recipeHistory: List<Recipe<*>>, fakeInventory: SimpleInventory, depth: Int): MutableMap<Recipe<*>, List<Recipe<*>>> {
        if(depth > 10) {
            return craftableRecipes
        }
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
            CraftingBenchClient.recipeToNewRecipeTree.getOrDefault(matchedRecipe, mutableSetOf()).forEach { nextRecipe ->
                if(!craftableRecipes.containsKey(nextRecipe) || craftableRecipes[nextRecipe]!!.size < recipeHistory.size-2) {
                    if(nextRecipe.id == Identifier("birch_boat")) {
                        1+1
                    }
                    CraftingBenchClient.newRecipeToRecipeTree.getOrDefault(nextRecipe, mutableMapOf()).forEach { (qnt, requiredRecipe) ->
                        val matcher = recipeFinder.Matcher(requiredRecipe)
                        if (matcher.match(qnt, null)) {
                            val newFakeInventory = SimpleInventory(fakeInventory.size())
                            repeat(newFakeInventory.size()) { slot ->
                                newFakeInventory.setStack(slot, fakeInventory.getStack(slot).copy())
                            }
                            repeat(qnt+1) {
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
                                newFakeInventory.addStack(matchedRecipe.output)
                            }
                            val newRecipeHistory = recipeHistory.toMutableList()
                            repeat(qnt+1) {
                                newRecipeHistory.add(matchedRecipe)
                            }
                            populateRecipes(CraftingBenchClient.recipeToNewRecipeTree.getOrDefault(requiredRecipe, mutableSetOf()), recipeBook, craftableRecipes, newRecipeHistory, newFakeInventory, depth + qnt)
                        }
                    }
                }
            }
        }
        return craftableRecipes
    }


}