package io.github.lucaargolo.craftingbench.client

import io.github.lucaargolo.craftingbench.common.block.BlockCompendium
import io.github.lucaargolo.craftingbench.common.blockentity.BlockEntityCompendium
import io.github.lucaargolo.craftingbench.common.item.ItemCompendium
import io.github.lucaargolo.craftingbench.common.screenhandler.ScreenHandlerCompendium
import it.unimi.dsi.fastutil.ints.IntList
import net.fabricmc.api.ClientModInitializer
import net.minecraft.recipe.Recipe
import net.minecraft.recipe.RecipeType
import net.minecraft.util.Identifier
import net.minecraft.util.math.MathHelper
import net.minecraft.util.registry.Registry
import kotlin.system.measureTimeMillis

object CraftingBenchClient: ClientModInitializer {

    val recipeToNewRecipeTree: MutableMap<Recipe<*>, MutableSet<Recipe<*>>> = mutableMapOf()
    val newRecipeToRecipeTree: MutableMap<Recipe<*>, MutableMap<Int, Recipe<*>>> = mutableMapOf()
    val recipeIngredientMap: MutableMap<IntList, MutableSet<Recipe<*>>> = mutableMapOf()

    fun onRecipeBookReload(recipes: Iterable<Recipe<*>>) {
        val time = measureTimeMillis {
            recipeToNewRecipeTree.clear()
            recipeIngredientMap.clear()
            recipes.forEach { recipe ->
                if (recipe.type == RecipeType.CRAFTING) {
                    recipe.ingredients.forEach { ingredient ->
                        recipeIngredientMap.getOrPut(ingredient.matchingItemIds, ::mutableSetOf).add(recipe)
                    }
                }
            }
            recipes.forEach { recipe ->
                if(recipe.type == RecipeType.CRAFTING) {
                    recipeIngredientMap.forEach { (intList, recipeSet) ->
                        if (intList.contains(Registry.ITEM.getRawId(recipe.output.item))) {
                            recipeSet.forEach { setRecipe ->
                                val ingredientsNeeded = setRecipe.ingredients.filter { it.matchingItemIds == intList }.size
                                val craftsNeeded = MathHelper.ceil(ingredientsNeeded/recipe.output.count.toFloat())
                                newRecipeToRecipeTree.getOrPut(setRecipe, ::mutableMapOf)[craftsNeeded] = recipe
                                recipeToNewRecipeTree.getOrPut(recipe, ::mutableSetOf).add(setRecipe)
                            }
                        }
                    }
                }
            }
        }
        println("whatever this is took $time")
    }

    override fun onInitializeClient() {
        BlockCompendium.initializeClient()
        ItemCompendium.initializeClient()
        BlockEntityCompendium.initializeClient()
        ScreenHandlerCompendium.initializeClient()
    }

}