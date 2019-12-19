package pfister.quickercrafting.common.crafting

import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.IRecipe
import net.minecraft.item.crafting.Ingredient
import pfister.quickercrafting.LOG
import pfister.quickercrafting.common.util.canStack

// Some fun type aliases to improve readability
typealias Amount = Int

typealias Index = Int

typealias RecipeList = ArrayList<IRecipe>

//
object RecipeCalculator {
    // An interface to interact with crafting inventories
    // Note that this is a read-only interface
    interface CraftInventory {
        fun getNormalInv(): Array<ItemStack>
        fun getCraftResults(): Array<ItemStack>

        fun getCombinedInv(): Array<ItemStack> {
            return getNormalInv() + getCraftResults()
        }

        fun size() = getNormalInv().size + getCraftResults().size
    }

    data class CraftingInfo(val Recipe: IRecipe, val ItemMap: Map<Index, Amount>, val Missing: List<Ingredient>) {
        fun canCraft(): Boolean = Missing.isEmpty() && ItemMap.isNotEmpty()
        fun toCraftPath(): CraftingPath = CraftingPath(arrayOf(this))
    }

    data class CraftingPath(val CraftingInfos: Array<CraftingInfo?>) {
        companion object {
            fun shiftCraftRecipe(inventory: CraftInventory, recipe: IRecipe): CraftingPath {
                var maxCraftable = 0
                inventory.getCraftResults().forEach {
                    if (it.isEmpty) {
                        maxCraftable += recipe.recipeOutput.maxStackSize
                    } else if (recipe.recipeOutput.canStack(it)) {
                        maxCraftable += it.maxStackSize - it.count
                    }
                }
                return doCraftPath(inventory, arrayOfNulls<IRecipe>(maxCraftable).map { recipe } as RecipeList).reduceToCraftablePath()
            }
        }

        val totalItemMap: Map<Index, Amount> by lazy {
            val mergedMap: MutableMap<Index, Amount> = mutableMapOf()
            CraftingInfos.forEach { craftInfo ->
                if (craftInfo == null) return@forEach
                craftInfo.ItemMap.forEach { (k, v) ->
                    mergedMap.merge(k, v) { v1, v2 -> v1 + v2 }
                }
            }
            mergedMap
        }

        fun isEmpty(): Boolean = CraftingInfos.isEmpty()
        fun canCraft(): Boolean = !isEmpty() && CraftingInfos.all { it != null && it.canCraft() }
        fun reduceToCraftablePath(): CraftingPath = CraftingPath(CraftingInfos.dropLastWhile { it == null }.toTypedArray())
        fun finalRecipeOutput(): ItemStack? {
            return if (canCraft()) {
                CraftingInfos.last()?.Recipe?.recipeOutput
            } else null
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as CraftingPath

            if (!CraftingInfos.contentEquals(other.CraftingInfos)) return false

            return true
        }

        override fun hashCode(): Int {
            return CraftingInfos.contentHashCode()
        }
    }

    var CanCraft3By3: Boolean = true
    // Attempts to craft a recipe using the players inventory
    // Returns an itemmap of items it would used and a list of missing ingredients (if there are any)
    fun doCraft(inventory: CraftInventory, recipe: IRecipe): CraftingInfo {
        if (!CanCraft3By3 && !recipe.canFit(2, 2)) return CraftingInfo(recipe, mapOf(), listOf())
        // A map of all the items and their amounts used in the recipe
        val usedItemMap: MutableMap<Int, Int> = mutableMapOf()
        val ingredientsLeft = recipe.ingredients.filterNot { it == Ingredient.EMPTY }.toMutableList()

        // Iterate through the craftresult indexes first, then the inventory
        for (invIndex: Int in IntRange(inventory.size() - inventory.getCraftResults().size - 1, inventory.size() - 1).union(IntRange(0, inventory.getNormalInv().size - 1))) {
            if (ingredientsLeft.isEmpty()) break
            val stack = inventory.getCombinedInv()[invIndex]
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

    fun doCraftPath(inventory: CraftInventory, recipeList: RecipeList): CraftingPath {
        val backingInv = inventory.getCombinedInv().map { it.copy() }.toTypedArray()
        val backingCraftInv = object : CraftInventory {
            override fun getNormalInv(): Array<ItemStack> {
                return backingInv.take(inventory.getNormalInv().size).toTypedArray()
            }

            override fun getCraftResults(): Array<ItemStack> {
                return backingInv.drop(inventory.getNormalInv().size).toTypedArray()
            }

        }
        val craftPath = CraftingPath(arrayOfNulls(recipeList.size))
        recipeList.forEachIndexed { i, recipe ->
            val crafted = doCraft(backingCraftInv, recipe)
            if (crafted.canCraft()) {
                craftPath.CraftingInfos[i] = crafted
                if (!removeFromInventory(backingInv, crafted)) {
                    LOG.warn("RecipeCalculator: There was an issue removing items from the working inventory while doing a craft path!")
                    return craftPath
                }
            } else {
                return craftPath
            }
        }
        return craftPath
    }

    private fun removeFromInventory(inventory: Array<ItemStack>, craftingInfo: CraftingInfo): Boolean {
        if (!craftingInfo.canCraft()) return false
        if (craftingInfo.ItemMap.isEmpty()) return true
        if (inventory.size <= craftingInfo.ItemMap.maxBy { it.key }!!.key) return false

        craftingInfo.ItemMap.forEach { (index, amount) ->
            if (amount == inventory[index].count) {
                inventory[index] = ItemStack.EMPTY
            } else {
                inventory[index].count = inventory[index].count - amount
            }
        }
        return true

    }

    // Determines if the inventory can craft a recipe
    fun canCraft(inventory: CraftInventory, recipe: IRecipe): Boolean {
        return doCraft(inventory, recipe).canCraft()
    }


}