package pfister.quickercrafting.client.gui

import it.unimi.dsi.fastutil.objects.ObjectAVLTreeSet
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import net.minecraft.item.Item
import net.minecraft.item.crafting.IRecipe
import pfister.quickercrafting.LOG
import pfister.quickercrafting.client.crafting.RecipeSearch
import pfister.quickercrafting.client.crafting.RecipeWorker
import java.util.*
import kotlin.system.measureTimeMillis

// Handles filtering Craftable recipes according to the search query and or whether 3x3 is craftable
object DisplayedRecipeFilter {

    // The Recipe daemon sends over a stream of recipes and whether or not they need to be removed or added
    val CHANGED_RECIPE_CHANNEL: Channel<Pair<IRecipe, Boolean>> = Channel(Channel.Factory.UNLIMITED)
    @ExperimentalCoroutinesApi
    val DISPLAYED_RECIPE_DAEMON = GlobalScope.launch {
        CHANGED_RECIPE_CHANNEL.consumeEach { (recipe, added) ->
            if (added) {
                if (filter(recipe)) displayedRecipes.add(recipe)
            } else {
                displayedRecipes.remove(recipe)
            }
        }
    }


    private var displayedRecipes: ObjectAVLTreeSet<IRecipe> = ObjectAVLTreeSet(Comparator { recipe1, recipe2 ->
        val r1 = Item.getIdFromItem(recipe1.recipeOutput.item)
        val r2 = Item.getIdFromItem(recipe2.recipeOutput.item)
        r1 - r2
    })

    private var isSearching = false
    private var currentSearchSet: Set<IRecipe> = setOf()
    private var canCraft3x3 = false

    // Suffix tree used for searching -- courtesy of JEI
    // Goes through all the recipes in the game and then associates them with their recipe output's tooltip text so we can search easily
    private val recipeSearch: RecipeSearch = RecipeSearch()

    fun set3x3(canCraft3x3: Boolean) {
        if (this.canCraft3x3 != canCraft3x3) {
            this.canCraft3x3 = canCraft3x3
            filterRecipes()
        } else {
            this.canCraft3x3 = canCraft3x3
        }
    }

    fun setSearchTerm(query: String) {
        if (query.isBlank()) {
            isSearching = false
            currentSearchSet = setOf()
        } else {
            isSearching = true
            currentSearchSet = recipeSearch.search(query)
        }
        filterRecipes()
    }

    fun getRecipesToDisplay(): List<IRecipe> {
        return displayedRecipes.toList()
    }

    private fun filter(recipe: IRecipe): Boolean {
        val canCraft = canCraft3x3 || recipe.canFit(2, 2)
        if (!canCraft) return false

        return if (isSearching) currentSearchSet.contains(recipe) else true
    }

    private fun filterRecipes() {
        LOG.info("Filtering recipes took: " + measureTimeMillis {
            displayedRecipes.clear()
            for (r in RecipeWorker.CraftableRecipes) {
                if (filter(r)) displayedRecipes.add(r)
            }
        } + "ms.")
        displayedRecipes
    }

}