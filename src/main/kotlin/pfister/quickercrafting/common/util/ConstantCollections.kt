package pfister.quickercrafting.common.util

import net.minecraft.client.util.RecipeItemHelper
import net.minecraft.item.crafting.IRecipe
import net.minecraftforge.fml.common.registry.ForgeRegistries
import org.jgrapht.graph.builder.GraphTypeBuilder
import org.jgrapht.opt.graph.fastutil.FastutilMapIntVertexGraph
import pfister.quickercrafting.LOG

object ConstantCollections {
    val ItemsUsedInRecipes: Set<Int> by lazy {
        val set = mutableSetOf<Int>()
        ForgeRegistries.RECIPES.forEach {
            it.ingredients.forEach {
                set.addAll(it.validItemStacksPacked)
            }
            set.add(RecipeItemHelper.pack(it.recipeOutput))
        }
        set
    }
    // Finds all the items that aren't ingredients in any recipes
    val PackedItemStackRecipeGraph: FastutilMapIntVertexGraph<IRecipe> by lazy {

        val graph = FastutilMapIntVertexGraph<IRecipe>(null, null, GraphTypeBuilder.directed<Int, IRecipe>().allowingSelfLoops(true).allowingMultipleEdges(true).buildType().asWeighted())
        ItemsUsedInRecipes.forEach {
            graph.addVertex(it)
        }
        val recipes = ForgeRegistries.RECIPES.valuesCollection.filter { (it as IRecipe).ingredients.isNotEmpty() && !it.recipeOutput.isEmpty }
        recipes.forEach { recipe ->
            val recipeOutput = RecipeItemHelper.pack(recipe.recipeOutput)
            if (recipeOutput == 0) return@forEach
            recipe.ingredients.mapNotNull { it.validItemStacksPacked }.forEach { stackList ->
                stackList.forEach { stack ->
                    if (graph.getAllEdges(stack, recipeOutput)?.contains(recipe) != true) {
                        if (stack != 0) {
                            if (graph.containsVertex(stack) && graph.containsVertex(recipeOutput)) {
                                graph.addEdge(stack, recipeOutput, recipe)
                            } else {
                                LOG.warn("PackedItemStackRecipeGraph: Error Trying to add edge from ${RecipeItemHelper.unpack(stack)} to ${RecipeItemHelper.unpack(recipeOutput)} with ${recipe.registryName} of type ${recipe.javaClass.simpleName}.")
                            }
                        }
                    }
                }
            }
        }

        graph
    }
}