package io.github.lucaargolo.craftingbench.utils

import io.github.lucaargolo.craftingbench.client.CraftingBenchClient
import it.unimi.dsi.fastutil.ints.IntList
import net.minecraft.recipe.CraftingRecipe
import net.minecraft.recipe.RecipeMatcher
import net.minecraft.util.Identifier

class RecipeTree(val recipe: CraftingRecipe) {

    private val branchesList = mutableListOf<Branch>()

    fun getBranches(possibleRecipeTrees: MutableSet<RecipeTree>, depth: Int): Iterable<Branch> {
        return object : Iterable<Branch> {
            override fun iterator() = object : Iterator<Branch> {
                var index = 0

                var nestedBranchIterator: Iterator<Pair<RecipeTree, Int>>? = null
                var nestedIterator: Iterator<Branch>? = null
                var nestedIteratorMultiplier: Int = 1

                override fun hasNext(): Boolean {
                    return depth < 6 && (nestedBranchIterator?.hasNext() == true || index < branchesList.size)
                }

                override fun next(): Branch {
                    val branch = if(index >= branchesList.size) {
                        branchesList[index-1]
                    }else if(nestedIterator?.hasNext() == true || nestedBranchIterator?.hasNext() == true) {
                        branchesList[index]
                    }else{
                        val innerBranch = branchesList[index++]
                        nestedBranchIterator = innerBranch.nested.iterator()
                        innerBranch
                    }

                    if (nestedIterator?.hasNext() == true) {
                        val nestedBranch = nestedIterator!!.next()
                        return branchesList[index-1].combined(nestedBranch, nestedIteratorMultiplier)
                    }
                    nestedIterator = null

                    while (nestedBranchIterator?.hasNext() == true && nestedIterator == null) {
                        val pair = nestedBranchIterator!!.next()
                        val recipeTree = pair.first
                        if(possibleRecipeTrees.contains(recipeTree)) {
                            val iterator = recipeTree.getBranches(possibleRecipeTrees, depth + 1).iterator()
                            if (iterator.hasNext()) {
                                nestedIterator = iterator
                                nestedIteratorMultiplier = pair.second
                            }
                        }else{
                        }
                    }

                    if (nestedIterator != null) {
                        val nestedBranch = nestedIterator!!.next()
                        return branchesList[index-1].combined(nestedBranch, nestedIteratorMultiplier)
                    }

                    return branch
                }

            }
        }
    }

    fun branch(recipeHistory: List<CraftingRecipe>, ingredients: List<IntList>): Branch {
        val branch = Branch(this, recipeHistory, ingredients)
        branchesList.add(branch)
        return branch
    }

    class Branch(val parent: RecipeTree, val recipeHistory: List<CraftingRecipe>, val ingredients: List<IntList>) {

        private val nestedList: MutableList<Pair<CraftingRecipe, Int>> = mutableListOf()

        fun nest(requiredRecipe: CraftingRecipe, requiredQnt: Int) {
            nestedList.add(Pair(requiredRecipe, requiredQnt))
        }

        fun combined(nestedBranch: Branch, requiredQnt: Int): Branch {
            val requiredRecipe = nestedBranch.parent.recipe

            val innerRecipeHistory = nestedBranch.recipeHistory
            val innerIngredients = nestedBranch.ingredients

            val combinedRecipeHistory = mutableListOf<CraftingRecipe>()
            repeat(requiredQnt) {
                combinedRecipeHistory.addAll(innerRecipeHistory)
            }
            combinedRecipeHistory.addAll(recipeHistory)
            val innerOutputQnt = requiredRecipe.output.count * requiredQnt
            var innerOutputExcess = innerOutputQnt
            val innerOutputItem = RecipeMatcher.getItemId(requiredRecipe.output)
            val combinedIngredients = ingredients.toMutableList()
            val combinedIngredientsIterator = combinedIngredients.iterator()
            while (combinedIngredientsIterator.hasNext()) {
                val ingredient = combinedIngredientsIterator.next()
                if (innerOutputExcess > 0 && ingredient.contains(innerOutputItem)) {
                    combinedIngredientsIterator.remove()
                    innerOutputExcess--
                }
            }
            repeat(requiredQnt) {
                combinedIngredients.addAll(innerIngredients)
            }

            return Branch(parent, combinedRecipeHistory, combinedIngredients)
        }

        val nested: Iterable<Pair<RecipeTree, Int>>
            get() = object: Iterable<Pair<RecipeTree, Int>> {
                override fun iterator() = object: Iterator<Pair<RecipeTree, Int>> {
                    var index = 0

                    override fun hasNext() = index < nestedList.size

                    override fun next(): Pair<RecipeTree, Int> {
                        val pair = nestedList[index++]
                        return Pair(CraftingBenchClient.recipeTrees[pair.first]!!, pair.second)
                    }
                }

            }

    }

}
