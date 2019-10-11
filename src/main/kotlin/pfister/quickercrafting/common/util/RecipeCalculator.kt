package pfister.quickercrafting.common.util

import net.minecraft.creativetab.CreativeTabs
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemDoor
import net.minecraft.item.crafting.IRecipe
import net.minecraft.item.crafting.Ingredient
import net.minecraftforge.fml.common.registry.ForgeRegistries
import pfister.quickercrafting.common.gui.ContainerQuickerCrafting
import kotlin.concurrent.thread

class RecipeCalculator(val Container: ContainerQuickerCrafting) {
    companion object {
        val SortedRecipes: List<IRecipe> = ForgeRegistries.RECIPES.valuesCollection
                .filterNot { it.isDynamic || it.ingredients.size == 0 }
                .sortedWith(Comparator<IRecipe> { o1, o2 ->
                    val itemX = o1.recipeOutput.item
                    val itemY = o2.recipeOutput.item

                    // Blocks are first
                    val xIsBlock = itemX is ItemBlock || itemX is ItemDoor
                    val yIsBlock = itemY is ItemBlock || itemY is ItemDoor

                    if (xIsBlock && !yIsBlock) return@Comparator -1
                    else if (yIsBlock && !xIsBlock) return@Comparator 1
                    else if (xIsBlock && yIsBlock) {
                        // Special check for doors
                        if (itemX is ItemDoor && itemY !is ItemDoor) return@Comparator 1
                        else if (!(itemX is ItemDoor) && itemY is ItemDoor) return@Comparator -1
                        else if (itemX is ItemDoor && itemY is ItemDoor) return@Comparator 0

                        val blockX = (itemX as ItemBlock).block
                        val blockY = (itemY as ItemBlock).block
                        // Full blocks/building blocks should be put first
                        val isBuildingOrFullBlockX = blockX.isFullBlock(blockX.defaultState) || blockX.creativeTabToDisplayOn == CreativeTabs.BUILDING_BLOCKS
                        val isBuildingOrFullBlockY = blockY.isFullBlock(blockY.defaultState) || blockY.creativeTabToDisplayOn == CreativeTabs.BUILDING_BLOCKS
                        if (isBuildingOrFullBlockX && !isBuildingOrFullBlockY) return@Comparator -1
                        else if (!isBuildingOrFullBlockX && isBuildingOrFullBlockY) return@Comparator 1
                        // Otherwise just do a default comparison
                        var comparison = blockX::class.simpleName!!.compareTo(blockY::class.simpleName!!)
                        if (comparison == 0) {
                            comparison = blockX.unlocalizedName.compareTo(blockY.unlocalizedName)
                        }
                        return@Comparator comparison
                    }
                    // Next is damageable items, typically tools, swords, hoes, armor, etc...
                    val xIsDamageable = itemX.isDamageable
                    val yIsDamageable = itemY.isDamageable

                    if (xIsDamageable && !yIsDamageable) return@Comparator -1
                    else if (yIsDamageable && !xIsDamageable) return@Comparator 1

                    // Lastly a default check between class names and, failing that, their unlocalized names
                    var comparison =
                            itemX::class.simpleName!!.compareTo(itemY::class.simpleName!!)
                    if (comparison == 0) {
                        comparison = itemX.unlocalizedName.compareTo(itemY.unlocalizedName)
                    }
                    comparison
                })
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
                        usedItemMap.put(index,usedItemMap.getOrDefault(index,0) + 1)
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
        running_thread = thread {
            val intermidate = SortedRecipes.filter { recipe ->
                canCraft(recipe)
            }
            list.clear()
            list.addAll(intermidate)
        }
    }
}