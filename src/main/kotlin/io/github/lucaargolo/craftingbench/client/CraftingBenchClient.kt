package io.github.lucaargolo.craftingbench.client

import io.github.lucaargolo.craftingbench.CraftingBench
import io.github.lucaargolo.craftingbench.common.block.BlockCompendium
import io.github.lucaargolo.craftingbench.common.blockentity.BlockEntityCompendium
import io.github.lucaargolo.craftingbench.common.item.ItemCompendium
import io.github.lucaargolo.craftingbench.common.screenhandler.ScreenHandlerCompendium
import io.github.lucaargolo.craftingbench.mixin.RecipeManagerInvoker
import io.github.lucaargolo.craftingbench.utils.RecipeTree
import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.ints.IntList
import net.fabricmc.api.ClientModInitializer
import net.minecraft.recipe.*
import net.minecraft.util.math.MathHelper
import net.minecraft.util.registry.Registry
import kotlin.system.measureTimeMillis

object CraftingBenchClient: ClientModInitializer {

    val recipeTrees: MutableMap<CraftingRecipe, RecipeTree> = mutableMapOf()
    val itemToRecipeTrees: MutableMap<Int, MutableSet<RecipeTree>> = mutableMapOf()

    private val recipesYouCanUseToDoItem: MutableMap<Int, MutableMap<CraftingRecipe, Int>> = mutableMapOf()

    fun onSynchronizeRecipes(recipeManager: RecipeManager) {
        CraftingBench.LOGGER.info("[Crafting Bench] New recipes received, constructing recipe trees...")
        if(CraftingBench.NBTCRAFTING) {
            CraftingBench.LOGGER.warn("[Crafting Bench] NBTCrafting is present! Recipes trees might take longer to build and will not work correctly.")
        }
        val time = measureTimeMillis {
            recipeTrees.clear()
            itemToRecipeTrees.clear()
            recipesYouCanUseToDoItem.clear()
            val recipes = (recipeManager as RecipeManagerInvoker).invokeGetAllOfType(RecipeType.CRAFTING).values
            recipes.forEach { recipe ->
                if (recipe is ShapedRecipe || recipe is ShapelessRecipe) {
                    recipesYouCanUseToDoItem.getOrPut(Registry.ITEM.getRawId(recipe.output.item), ::mutableMapOf)[recipe] = recipe.output.count
                }
            }
            recipes.forEach(::populateRecipeTree)
        }
        CraftingBench.LOGGER.info("[Crafting Bench] Constructed recipe trees in $time ms")
    }

    private fun populateRecipeTree(recipe: CraftingRecipe): RecipeTree {
        return recipeTrees[recipe] ?: let{
            val recipeTree = RecipeTree(recipe)
            val ingredients = recipe.ingredients.map(Ingredient::getMatchingItemIds).let {
                if(CraftingBench.NBTCRAFTING) it.map { IntArrayList().also { realIntList -> it.forEach { itemId -> realIntList.add(Registry.ITEM.getRawId(RecipeMatcher.getStackFromId(itemId).item)) } } } else it
            }
            //First path
            recipeTree.branch(listOf(recipe), ingredients)
            ingredients.forEach { ingredient ->
                ingredient.forEach { item ->
                    itemToRecipeTrees.getOrPut(item) { mutableSetOf() }.add(recipeTree)
                }
            }

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
            populateRecipeTree(recipe, recipeTree, listOf(recipe), ingredients, allRequiredRecipes)
            //The end
            recipeTrees[recipe] = recipeTree
            recipeTree
        }
    }

    private fun populateRecipeTree(originalRecipe: CraftingRecipe, recipeTree: RecipeTree, originalRecipeHistory: List<CraftingRecipe>, originalIngredients: List<IntList>, requiredRecipes: Map<CraftingRecipe, Int>) {

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
                ingredients.addAll(recipe.ingredients.map(Ingredient::getMatchingItemIds).let {
                    if(CraftingBench.NBTCRAFTING) it.map { IntArrayList().also { realIntList -> it.forEach { itemId -> realIntList.add(Registry.ITEM.getRawId(RecipeMatcher.getStackFromId(itemId).item)) } } } else it
                })
            }

            recipeHistory.reverse()
            val branch = recipeTree.branch(recipeHistory, ingredients)

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

            allRequiredRecipes.filter { it.key != originalRecipe }.forEach(branch::nest)
        }
    }

    override fun onInitializeClient() {
        BlockCompendium.initializeClient()
        ItemCompendium.initializeClient()
        BlockEntityCompendium.initializeClient()
        ScreenHandlerCompendium.initializeClient()
    }

}