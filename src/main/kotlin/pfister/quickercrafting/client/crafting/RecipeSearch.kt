package pfister.quickercrafting.client.crafting

import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import net.minecraft.client.Minecraft
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.item.crafting.IRecipe
import net.minecraft.util.text.TextFormatting
import net.minecraftforge.fml.common.registry.ForgeRegistries
import suffixtree.GeneralizedSuffixTree
import java.util.*

// Works around generalsuffixtree's limitation of having the indexes in increasing order
// Basically wraps it to allow us to search for recipes easier
class RecipeSearch {
    private val generalSuffixTree: GeneralizedSuffixTree = GeneralizedSuffixTree()
    private val intToRecipeMap: Int2ObjectMap<IRecipe> = Int2ObjectAVLTreeMap()

    init {
        var counter = 0
        ForgeRegistries.RECIPES.valuesCollection.forEach { recipe ->
            val search_strings = recipe.recipeOutput.getTooltip(Minecraft.getMinecraft().player, ITooltipFlag.TooltipFlags.NORMAL).map {
                TextFormatting.getTextWithoutFormattingCodes(it)!!.toLowerCase(Locale.ROOT)
            }
            search_strings.forEach { generalSuffixTree.put(it, counter) }
            intToRecipeMap[counter] = recipe
            counter += 1
        }
    }

    fun search(query: String): Set<IRecipe> {
        val intSet = generalSuffixTree.search(query)
        return intSet.map { intToRecipeMap.get(it) }.toHashSet()
    }


}