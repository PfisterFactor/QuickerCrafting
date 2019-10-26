package pfister.quickercrafting.common.crafting

import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.IRecipe
import net.minecraft.item.crafting.Ingredient

// Some fun type aliases to improve readability
typealias Amount = Int

typealias Index = Int

typealias RecipeList = ArrayList<IRecipe>

//
object RecipeCalculator {

    data class CraftingInfo(val Recipe: IRecipe, val ItemMap: Map<Index, Amount>, val Missing: List<Ingredient>) {
        fun canCraft(): Boolean = Missing.isEmpty() && ItemMap.isNotEmpty()
    }
    // Attempts to craft a recipe using the players inventory
    // Returns an itemmap of items it would used and a list of missing ingredients (if there are any)
    fun doCraft(inventory: List<ItemStack>, recipe: IRecipe): CraftingInfo {

        // A map of all the items and their amounts used in the recipe
        val usedItemMap: MutableMap<Int, Int> = mutableMapOf()
        val ingredientsLeft = recipe.ingredients.filterNot { it == Ingredient.EMPTY }.toMutableList()

        // Iterate through the craftresult indexes first, then the inventory
        for (invIndex: Int in IntRange(inventory.size - 4, inventory.size - 1).union(IntRange(0, inventory.size - 4))) {
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
    fun canCraft(inventory: List<ItemStack>, recipe: IRecipe): Boolean {
        return doCraft(inventory, recipe).canCraft()
    }


}