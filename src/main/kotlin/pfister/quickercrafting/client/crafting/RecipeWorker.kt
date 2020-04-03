package pfister.quickercrafting.client.crafting

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import net.minecraft.client.util.RecipeItemHelper
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.IRecipe
import pfister.quickercrafting.client.gui.DisplayedRecipeFilter
import pfister.quickercrafting.common.crafting.RecipeGraph
import pfister.quickercrafting.common.util.accountSet
import pfister.quickercrafting.common.util.setPacked

// Creates and handles a background daemon coroutine that keeps track of the recipes
// the player can craft given their inventory
object RecipeWorker {
    // Contains all the recipes the player can currently craft
    val CraftableRecipes: MutableSet<IRecipe> = HashSet()

    private lateinit var recipeGraph: RecipeGraph

    private val recipeItemHelper = RecipeItemHelper()

    // The channel that data is sent to RECIPE_DAEMON through
    private val RECIPE_CHANNEL: Channel<Pair<Int, Int>> = Channel(Channel.Factory.UNLIMITED)

    // The coroutine that does the work of keeping track of the recipes, does not start until startRecipeDaemon is called
    @ExperimentalCoroutinesApi
    private val RECIPE_DAEMON = GlobalScope.launch(start = CoroutineStart.LAZY) {
        RECIPE_CHANNEL.consumeEach { (packedStack, count) ->
            // Do stuff with packed stack
            if (recipeGraph.containsVertex(packedStack)) {
                val recipes = recipeGraph.outgoingEdgesOf(packedStack)
                recipes.forEach { recipe ->
                    if (recipeItemHelper.canCraft(recipe, null)) {
                        CraftableRecipes.add(recipe)
                        DisplayedRecipeFilter.CHANGED_RECIPE_CHANNEL.send(recipe to true)
                    } else {
                        CraftableRecipes.remove(recipe)
                        DisplayedRecipeFilter.CHANGED_RECIPE_CHANNEL.send(recipe to false)
                    }
                }
            }
        }
    }

    // Starts the background recipe daemon
    @ExperimentalCoroutinesApi
    fun startRecipeDaemon() {
        RECIPE_DAEMON.start()
    }

    fun buildRecipeGraph() {
        if (!RecipeWorker::recipeGraph.isInitialized) recipeGraph = RecipeGraph()
    }

    suspend fun sendItemStack(stack: ItemStack) {
        if (stack == ItemStack.EMPTY) return
        recipeItemHelper.accountSet(stack)
        RECIPE_CHANNEL.send(RecipeItemHelper.pack(stack) to stack.count)
    }

    suspend fun sendPackedItemStack(stack: Int, count: Int) {
        if (stack == RecipeItemHelper.pack(ItemStack.EMPTY)) return
        recipeItemHelper.setPacked(stack, count)
        RECIPE_CHANNEL.send(stack to count)
    }

    fun clearRecipes() {
        recipeItemHelper.clear()
        CraftableRecipes.clear()
    }


}