package pfister.quickercrafting.common.util

import net.minecraft.item.Item
import net.minecraft.item.crafting.IRecipe
import net.minecraft.item.crafting.Ingredient
import net.minecraftforge.fml.common.registry.ForgeRegistries
import pfister.quickercrafting.common.gui.ContainerQuickerCrafting
import kotlin.concurrent.thread

class RecipeCalculator(val Container: ContainerQuickerCrafting) {
    companion object {
        val SortedRecipes: ArrayList<IRecipe> = run {
            val items = Item.REGISTRY
            ArrayList(ForgeRegistries.RECIPES.valuesCollection
                    .filterNot { it.isDynamic || it.ingredients.size == 0 }
                    .sortedWith(Comparator { recipe1, recipe2 ->
                        val r1 = items.indexOfFirst { recipe1.recipeOutput.item == it }
                        val r2 = items.indexOfFirst { recipe2.recipeOutput.item == it }
                        return@Comparator r1 - r2
                    }))
        }
    }

    // Attempts to craft a recipe using the players inventory
    // If success, returns a list of indexes to the passed in item list corresponding to ingredients used and amount used
    // If failure, returns None
    fun doCraft(recipe: IRecipe): Map<Int, Int>? {
        // A map of all the items and their amounts used in the recipe
        val usedItemMap: MutableMap<Int, Int> = mutableMapOf()
        val itemStacks = Container.inventory
        val returnVal = recipe.ingredients
                .filterNot { it == Ingredient.EMPTY }
                .all { ingr ->
                    // Find an itemstack index where the count is greater than 0 and the ingredient accepts the itemstack for crafting
                    var index = -1
                    for (i in itemStacks.size - 1 downTo 0) {
                        val itemstack = itemStacks[i]
                        if (itemstack.count > 0 && ingr.apply(itemstack) && itemstack.count - usedItemMap.getOrDefault(i,0) > 0) {
                            index = i
                            break
                        }
                    }
                    if (index != -1) {
                        usedItemMap[index] = usedItemMap.getOrDefault(index, 0) + 1
                        true
                    }
                    else false

                }
        return if (returnVal)
            usedItemMap
        else
            null
    }

    // Determines if the inventory can craft a recipe
    fun canCraft(recipe: IRecipe): Boolean = doCraft(recipe) != null

    private var running_thread: Thread? = null
    fun populateRecipeList(list: MutableList<IRecipe>) {
        running_thread?.interrupt()
        running_thread = thread(isDaemon = true) {
            val intermediate = SortedRecipes.filter {
                canCraft(it)
            }
            list.clear()
            list.addAll(intermediate)
        }
    }
}