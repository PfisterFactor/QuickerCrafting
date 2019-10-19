package pfister.quickercrafting.common.util

import net.minecraft.init.Items
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.IRecipe
import net.minecraft.item.crafting.Ingredient
import net.minecraftforge.fml.common.registry.ForgeRegistries
import pfister.quickercrafting.common.gui.ContainerQuickerCrafting
import kotlin.Comparator
import kotlin.concurrent.thread

// Some fun type aliases to improve readability
typealias Amount = Int

typealias Index = Int

typealias RecipeList = ArrayList<IRecipe>

//
class RecipeCalculator(val Container: ContainerQuickerCrafting) {
    companion object {
        // All the recipes that we can craft sorted according to how their items would appear in the search tab in creative
        val SortedRecipes: Map<Item, RecipeList> by lazy {
            val items = Item.REGISTRY
            ArrayList(ForgeRegistries.RECIPES.valuesCollection
                    .filter { (it as IRecipe).ingredients.isNotEmpty() }
                    .sortedWith(Comparator { recipe1, recipe2 ->
                        val r1 = items.indexOfFirst { recipe1.recipeOutput.item == it }
                        val r2 = items.indexOfFirst { recipe2.recipeOutput.item == it }
                        return@Comparator r1 - r2
                    })).fold(mapOf<Item, RecipeList>()) { acc, r -> acc + Pair(r.recipeOutput.item, acc.getOrDefault(r.recipeOutput.item, RecipeList()).plus(r) as RecipeList) }
        }

        // Finds all the items that aren't used in any recipes
        // Don't even try to tell me the big O notation of this
        val NonIngredientItems: HashSet<Item> by lazy {
            val items = Item.REGISTRY
            val set = HashSet<Item>()
            set.addAll(items.filterNot { item ->
                ForgeRegistries.RECIPES.valuesCollection.any { recipe ->
                    recipe.ingredients.any { ingr ->
                        ingr.matchingStacks.any { stack ->
                            stack.item == item
                        }
                    }
                }
            }.without(Items.AIR))
            set

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
        val ingredientsLeft = recipe.ingredients.filterNot { it == Ingredient.EMPTY }.toMutableList()

        for (invIndex: Int in inventory.indices) {
            if (ingredientsLeft.isEmpty()) break

            val stack = inventory[invIndex]
            if (stack.isEmpty || NonIngredientItems.contains(stack.item)) continue

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
    fun populateRecipeList(list: MutableList<RecipeList>, callback: (RecipeList?) -> Unit = {}) {
        // Tell the thread to stop if its running
        running_thread.interrupt()
        // Wait for the thread to die
        running_thread.join()
        running_thread = thread(isDaemon = true) {
            list.clear()
            SortedRecipes.values.forEach { recipeList ->
                val newRecipelist = recipeList.filter { canCraft(it) } as RecipeList
                if (newRecipelist.isNotEmpty()) {
                    list.add(newRecipelist)
                    callback(newRecipelist)
                }

            }
            callback(null)
        }
    }
}