package pfister.quickercrafting.common.util

import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.IRecipe
import net.minecraft.item.crafting.Ingredient
import net.minecraftforge.fml.common.registry.ForgeRegistries
import pfister.quickercrafting.common.gui.ContainerQuickerCrafting
import kotlin.concurrent.thread

// Some fun type aliases to improve readability
typealias Amount = Int
typealias Index = Int

typealias RecipeList = ArrayList<IRecipe>

//
class RecipeCalculator(val Container: ContainerQuickerCrafting) {
    companion object {
        val SortedRecipes: Map<Item, RecipeList> = run {
            val items = Item.REGISTRY
            ArrayList(ForgeRegistries.RECIPES.valuesCollection
                    .filter { !it.isDynamic && (it as IRecipe).ingredients.size >= 1 }
                    .sortedWith(Comparator { recipe1, recipe2 ->
                        val r1 = items.indexOfFirst { recipe1.recipeOutput.item == it }
                        val r2 = items.indexOfFirst { recipe2.recipeOutput.item == it }
                        return@Comparator r1 - r2
                    })).fold(mapOf<Item, RecipeList>()) { acc, r -> acc + Pair(r.recipeOutput.item, acc.getOrDefault(r.recipeOutput.item, RecipeList()).plus(r) as RecipeList) }
        }
    }

    data class CraftingInfo(val Recipe: IRecipe, val ItemMap: Map<Index, Amount>, val Missing: List<Ingredient>) {
        fun canCraft(): Boolean = Missing.isEmpty() && ItemMap.isNotEmpty()
    }

    class CraftPath(initialCapacity: Int = 0) : ArrayList<CraftingInfo>(initialCapacity) {
        fun getUsedItems(): Map<Int, Int> {
            val map: MutableMap<Int, Int> = mutableMapOf()
            this.forEach {
                it.ItemMap.forEach { (k, v) -> map[k] = map.getOrDefault(k, 0) + v }
            }
            return map
        }
    }

    // Attempts to craft a recipe using the players inventory
    // If success, returns a list of indexes to the passed in item list corresponding to ingredients used and amount used
    // If failure, returns None
    fun doCraft(inventory: List<ItemStack>, recipe: IRecipe): CraftingInfo {
        // A map of all the items and their amounts used in the recipe
        val usedItemMap: MutableMap<Int, Int> = mutableMapOf()
        val missing: MutableList<Ingredient> = mutableListOf()
        recipe.ingredients
                .filterNot { it == Ingredient.EMPTY }
                .forEach { ingr ->
                    // Find an itemstack index where the count is greater than 0 and the ingredient accepts the itemstack for crafting
                    var index = -1
                    for (i in inventory.size - 1 downTo 0) {
                        val itemstack = inventory[i]
                        if (itemstack.count > 0 && ingr.apply(itemstack) && itemstack.count - usedItemMap.getOrDefault(i,0) > 0) {
                            index = i
                            break
                        }
                    }
                    if (index != -1) {
                        usedItemMap[index] = usedItemMap.getOrDefault(index, 0) + 1
                    } else {
                        missing.add(ingr)
                    }

                }
        return CraftingInfo(recipe, usedItemMap.toMap(), missing)
    }

    // Determines if the inventory can craft a recipe
    fun canCraft(recipe: IRecipe): Boolean {
        val result = doCraft(Container.inventory, recipe).canCraft()
        return result
    }

    private var running_thread: Thread? = null
    fun populateRecipeList(list: MutableList<RecipeList>) {
        running_thread?.interrupt()
        running_thread = thread(isDaemon = true) {
            val intermediate = SortedRecipes.values.map { it.filter { canCraft(it) } as RecipeList }.filter { it.isNotEmpty() }
            list.clear()
            list.addAll(intermediate)
        }
    }
}