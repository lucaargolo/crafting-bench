package io.github.lucaargolo.craftingbench.client

import io.github.lucaargolo.craftingbench.common.block.BlockCompendium
import io.github.lucaargolo.craftingbench.common.blockentity.BlockEntityCompendium
import io.github.lucaargolo.craftingbench.common.item.ItemCompendium
import io.github.lucaargolo.craftingbench.common.screenhandler.ScreenHandlerCompendium
import io.github.lucaargolo.craftingbench.mixin.RecipeManagerInvoker
import it.unimi.dsi.fastutil.ints.IntList
import net.fabricmc.api.ClientModInitializer
import net.minecraft.recipe.*
import net.minecraft.util.math.MathHelper
import net.minecraft.util.registry.Registry
import kotlin.system.measureTimeMillis

object CraftingBenchClient: ClientModInitializer {

    val recipeTrees: MutableMap<CraftingRecipe, MutableList<Pair<List<CraftingRecipe>, List<IntList>>>> = mutableMapOf()

    private val recipesYouCanUseToDoItem: MutableMap<Int, MutableMap<CraftingRecipe, Int>> = mutableMapOf()
    private val recipesYouCanDoWithItem: MutableMap<Int, MutableSet<CraftingRecipe>> = mutableMapOf()

    fun onSynchronizeRecipes(recipeManager: RecipeManager) {
        val time = measureTimeMillis {
            recipeTrees.clear()
            recipesYouCanDoWithItem.clear()
            recipesYouCanUseToDoItem.clear()
            val recipes = (recipeManager as RecipeManagerInvoker).invokeGetAllOfType(RecipeType.CRAFTING).values
            recipes.forEach { recipe ->
                if (recipe is ShapedRecipe || recipe is ShapelessRecipe) {
                    recipe.ingredients.forEach { ingredient ->
                        if (!ingredient.isEmpty) {
                            ingredient.matchingItemIds.forEach { itemId ->
                                recipesYouCanDoWithItem.getOrPut(itemId, ::mutableSetOf).add(recipe)
                            }
                        }
                    }
                    recipesYouCanUseToDoItem.getOrPut(Registry.ITEM.getRawId(recipe.output.item), ::mutableMapOf)[recipe] = recipe.output.count
                }
            }
            recipes.forEach(::populateRecipeTree)
        }
        println("Constructed recipe trees in $time ms")
    }

    private fun populateRecipeTree(recipe: CraftingRecipe, depth: Int = 0): MutableList<Pair<List<CraftingRecipe>, List<IntList>>> {
        return recipeTrees[recipe] ?: let{
            val recipeTree = mutableListOf<Pair<List<CraftingRecipe>, List<IntList>>>()
            val ingredients = recipe.ingredients.map(Ingredient::getMatchingItemIds)
            //First path
            recipeTree.add(Pair(listOf(recipe), ingredients))
            //Recursive paths
            val itemQntMap = mutableMapOf<Int, Int>()
            ingredients.forEach { ingredient ->
                ingredient.forEach { itemId ->
                    itemQntMap[itemId] = (itemQntMap[itemId] ?: 0) + 1
                }
            }
            val allRequiredRecipes = mutableMapOf<CraftingRecipe, Int>()
            itemQntMap.forEach { (item, qnt) ->
                recipesYouCanUseToDoItem[item]?.let { requiredRecipes ->
                    requiredRecipes.mapValues { entry -> MathHelper.ceil(qnt / entry.value.toFloat()) }.forEach { (a, b) ->
                        allRequiredRecipes[a] = (allRequiredRecipes[a] ?: 0) + b
                    }
                }
            }
            recipeTree.addAll(populateRecipeTree(listOf(recipe), ingredients, allRequiredRecipes, depth + 1))
            //The end
            recipeTrees[recipe] = recipeTree
            recipeTree
        }
    }

    private fun populateRecipeTree(originalRecipeHistory: List<CraftingRecipe>, originalIngredients: List<IntList>, requiredRecipes: Map<CraftingRecipe, Int>, depth: Int = 0): List<Pair<List<CraftingRecipe>, List<IntList>>> {
        val recipeTree = mutableListOf<Pair<List<CraftingRecipe>, List<IntList>>>()
        if(depth > 4) {
            return recipeTree
        }

        requiredRecipes.forEach { (recipe, qnt) ->
            val recipeHistory = originalRecipeHistory.toMutableList()
            val ingredients = originalIngredients.toMutableList()

            val outputQnt = recipe.output.count * qnt
            var outputExcess = outputQnt
            val outputItem = Registry.ITEM.getRawId(recipe.output.item)

            repeat(qnt) {
                recipeHistory.add(recipe)
            }

            val ingredientsIterator = ingredients.iterator()
            while (ingredientsIterator.hasNext()) {
                val ingredient = ingredientsIterator.next()
                if(outputExcess > 0 && ingredient.contains(outputItem)) {
                    ingredientsIterator.remove()
                    outputExcess--
                }
            }
            repeat(qnt) {
                ingredients.addAll(recipe.ingredients.map(Ingredient::getMatchingItemIds))
            }

            recipeHistory.reverse()
            recipeTree.add(Pair(recipeHistory, ingredients))

            val itemQntMap = mutableMapOf<Int, Int>()
            ingredients.forEach { ingredient ->
                ingredient.forEach { itemId ->
                    itemQntMap[itemId] = (itemQntMap[itemId] ?: 0) + 1
                }
            }
            val allRequiredRecipes = mutableMapOf<CraftingRecipe, Int>()
            itemQntMap.forEach { (item, qnt) ->
                recipesYouCanUseToDoItem[item]?.let { requiredRecipes ->
                    requiredRecipes.mapValues { entry -> MathHelper.ceil(qnt/entry.value.toFloat()) }.forEach { (a, b) ->
                        allRequiredRecipes[a] = (allRequiredRecipes[a] ?: 0) + b
                    }
                }
            }

            allRequiredRecipes.forEach { (requiredRecipe, requiredQnt) ->
                populateRecipeTree(requiredRecipe, depth).forEach { (innerRecipeHistory, innerIngredients) ->
                    val combinedRecipeHistory = mutableListOf<CraftingRecipe>()
                    repeat(requiredQnt) {
                        combinedRecipeHistory.addAll(innerRecipeHistory)
                    }
                    combinedRecipeHistory.addAll(recipeHistory)
                    val innerOutputQnt = requiredRecipe.output.count * requiredQnt
                    var innerOutputExcess = innerOutputQnt
                    val innerOutputItem = Registry.ITEM.getRawId(requiredRecipe.output.item)
                    val combinedIngredients = ingredients.toMutableList()
                    val combinedIngredientsIterator = combinedIngredients.iterator()
                    while (combinedIngredientsIterator.hasNext()) {
                        val ingredient = combinedIngredientsIterator.next()
                        if(innerOutputExcess > 0 && ingredient.contains(innerOutputItem)) {
                            combinedIngredientsIterator.remove()
                            innerOutputExcess--
                        }
                    }
                    repeat(requiredQnt) {
                        combinedIngredients.addAll(innerIngredients)
                    }
                    recipeTree.add(Pair(combinedRecipeHistory, combinedIngredients))
                }
            }
        }

        return recipeTree
    }

    override fun onInitializeClient() {
        BlockCompendium.initializeClient()
        ItemCompendium.initializeClient()
        BlockEntityCompendium.initializeClient()
        ScreenHandlerCompendium.initializeClient()
    }

}