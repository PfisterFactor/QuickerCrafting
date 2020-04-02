package pfister.quickercrafting.common.crafting

import it.unimi.dsi.fastutil.ints.IntAVLTreeSet
import net.minecraft.client.util.RecipeItemHelper
import net.minecraft.item.crafting.IRecipe
import net.minecraftforge.fml.common.registry.ForgeRegistries
import org.jgrapht.graph.builder.GraphTypeBuilder
import org.jgrapht.opt.graph.fastutil.FastutilMapIntVertexGraph
import pfister.quickercrafting.LOG

class RecipeGraph : FastutilMapIntVertexGraph<IRecipe>(null, null, GraphTypeBuilder.directed<Int, IRecipe>().allowingSelfLoops(true).allowingMultipleEdges(true).weighted(true).buildType()) {
    init {
        // Find all items that are ingredients in a recipe
        val ItemsUsedInRecipes = IntAVLTreeSet()
        ForgeRegistries.RECIPES.forEach {
            it.ingredients.forEach {
                ItemsUsedInRecipes.addAll(it.validItemStacksPacked)
            }
            ItemsUsedInRecipes.add(RecipeItemHelper.pack(it.recipeOutput))
        }
        ItemsUsedInRecipes.forEach {
            this.addVertex(it)
        }
        // Grab all recipes and creates recipe edges between different vertexes in the graphs
        val recipes = ForgeRegistries.RECIPES.valuesCollection.filter { (it as IRecipe).ingredients.isNotEmpty() && !it.recipeOutput.isEmpty }
        recipes.forEach { recipe ->
            val recipeOutput = RecipeItemHelper.pack(recipe.recipeOutput)
            if (recipeOutput == 0) return@forEach
            recipe.ingredients.mapNotNull { it.validItemStacksPacked }.forEach { stackList ->
                stackList.forEach { stack ->
                    if (!this.getAllEdges(stack, recipeOutput).contains(recipe)) {
                        if (stack != 0) {
                            if (this.containsVertex(stack) && this.containsVertex(recipeOutput)) {
                                this.addEdge(stack, recipeOutput, recipe)
                                this.setEdgeWeight(recipe, recipe.recipeOutput.count.toDouble())
                            } else {
                                LOG.warn("RecipeGraph: Error Trying to add edge from ${RecipeItemHelper.unpack(stack)} to ${RecipeItemHelper.unpack(recipeOutput)} with ${recipe.registryName} of type ${recipe.javaClass.simpleName}.")
                            }
                        }
                    }
                }
            }
        }
    }
}