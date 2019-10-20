package pfister.quickercrafting.common.util

import net.minecraft.client.util.RecipeItemHelper
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.IRecipe
import net.minecraft.item.crafting.Ingredient
import pfister.quickercrafting.LOG
import pfister.quickercrafting.common.gui.ContainerQuickerCrafting
import pfister.quickercrafting.common.util.collection.IndexedSet
import kotlin.concurrent.thread
import kotlin.system.measureTimeMillis

// Some fun type aliases to improve readability
typealias Amount = Int

typealias Index = Int

typealias RecipeList = ArrayList<IRecipe>

//
class RecipeCalculator(val Container: ContainerQuickerCrafting) {

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
        val ingredientsLeft = recipe.ingredients.filterNot { it == Ingredient.EMPTY }.toMutableList()

        for (invIndex: Int in inventory.indices) {
            if (ingredientsLeft.isEmpty()) break

            val stack = inventory[invIndex]
            if (stack.isEmpty) continue

            for (ingrIndex: Int in ingredientsLeft.indices.reversed()) {
                if (ingredientsLeft.isEmpty()) break

                val ingr = ingredientsLeft[ingrIndex]
                if (ingr.apply(stack)) {
                    val itemCount = usedItemMap.getOrDefault(invIndex, 0)
                    if (stack.count - itemCount > 0) {
                        usedItemMap[invIndex] = itemCount + 1
                        ingredientsLeft.removeAt(ingrIndex)
                    }

                }
            }
        }
        return CraftingInfo(recipe, usedItemMap.toMap(), ingredientsLeft)
    }

    // Determines if the inventory can craft a recipe
    fun canCraft(recipe: IRecipe): Boolean {
        return doCraft(Container.inventory, recipe).canCraft()
    }

    private var running_thread: Thread = Thread()
    fun populateRecipeList(list: IndexedSet<IRecipe>, changedStacks: List<ItemStack> = listOf(), callback: (IRecipe?) -> Unit = {}) {
        if (changedStacks.isEmpty()) return
        // Tell the thread to stop if its running
        running_thread.interrupt()
        // Wait for the thread to die
        running_thread.join()
        val changedRecipes: MutableSet<IRecipe> = mutableSetOf()
        changedStacks.forEach {
            val packed = RecipeItemHelper.pack(it)
            if (ConstantCollections.PackedItemStackRecipeGraph.containsVertex(packed))
                changedRecipes.addAll(ConstantCollections.PackedItemStackRecipeGraph.outgoingEdgesOf(RecipeItemHelper.pack(it)))
        }

        running_thread = thread(isDaemon = true) {
            list.removeIf { recipe -> changedRecipes.any { ItemStack.areItemsEqual(recipe.recipeOutput, it.recipeOutput) } }

            LOG.info("Populating recipes took " +
                    measureTimeMillis {
                        changedRecipes.forEach { recipe ->
                            if (canCraft(recipe)) {
                                list.add(recipe)
                                callback(recipe)
                }

                        }
                    } + "ms.")
            callback(null)
        }
    }
}