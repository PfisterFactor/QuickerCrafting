package pfister.quickercrafting.common.crafting

import it.unimi.dsi.fastutil.ints.IntAVLTreeSet
import it.unimi.dsi.fastutil.ints.IntSet
import net.minecraft.client.Minecraft
import net.minecraft.client.util.RecipeItemHelper
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.IRecipe
import net.minecraftforge.fml.common.registry.ForgeRegistries
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import org.jgrapht.graph.builder.GraphTypeBuilder
import org.jgrapht.opt.graph.fastutil.FastutilMapIntVertexGraph
import pfister.quickercrafting.LOG
import pfister.quickercrafting.client.gui.ClientContainerQuickerCrafting
import pfister.quickercrafting.common.util.collection.IndexedSet
import kotlin.concurrent.thread

object RecipeCache {
    // Finds all items used in recipes plus their outputs
    // Todo: If JEI is present use their ingredient list
    val ItemsUsedInRecipes: IntSet by lazy {
        val set = IntAVLTreeSet()
        ForgeRegistries.RECIPES.forEach {
            it.ingredients.forEach {
                set.addAll(it.validItemStacksPacked)
            }
            set.add(RecipeItemHelper.pack(it.recipeOutput))
        }
        set
    }

    // Finds all the items that aren't ingredients in any recipes
    val RecipeGraph: FastutilMapIntVertexGraph<IRecipe> by lazy {
        val graph = FastutilMapIntVertexGraph<IRecipe>(null, null, GraphTypeBuilder.directed<Int, IRecipe>().allowingSelfLoops(true).allowingMultipleEdges(true).buildType().asWeighted())
        ItemsUsedInRecipes.forEach {
            graph.addVertex(it)
        }
        val recipes = ForgeRegistries.RECIPES.valuesCollection.filter { (it as IRecipe).ingredients.isNotEmpty() && !it.recipeOutput.isEmpty }
        recipes.forEach { recipe ->
            val recipeOutput = RecipeItemHelper.pack(recipe.recipeOutput)
            if (recipeOutput == 0) return@forEach
            recipe.ingredients.mapNotNull { it.validItemStacksPacked }.forEach { stackList ->
                stackList.forEach { stack ->
                    if (graph.getAllEdges(stack, recipeOutput)?.contains(recipe) != true) {
                        if (stack != 0) {
                            if (graph.containsVertex(stack) && graph.containsVertex(recipeOutput)) {
                                graph.addEdge(stack, recipeOutput, recipe)
                            } else {
                                LOG.warn("RecipeGraph: Error Trying to add edge from ${RecipeItemHelper.unpack(stack)} to ${RecipeItemHelper.unpack(recipeOutput)} with ${recipe.registryName} of type ${recipe.javaClass.simpleName}.")
                            }
                        }
                    }
                }
            }
        }
        graph
    }
    @SideOnly(Side.CLIENT)
    val CraftableRecipes: IndexedSet<IRecipe> = IndexedSet(Comparator { recipe1, recipe2 ->
        val items = Item.REGISTRY
        val r1 = items.indexOfFirst { recipe1.recipeOutput.item == it }
        val r2 = items.indexOfFirst { recipe2.recipeOutput.item == it }
        r1 - r2
    })
    // 36 for the players inventory + 3 for the crafting results on the container
    @SideOnly(Side.CLIENT)
    private val oldInventory: Array<ItemStack> = Array(39) { ItemStack.EMPTY }

    @SideOnly(Side.CLIENT)
    fun updateCache(callback: (Boolean, Int) -> Unit = { _, _ -> }) {
        val player = Minecraft.getMinecraft().player
        val inv: Array<ItemStack> = if (player.openContainer is ClientContainerQuickerCrafting) {
            val cont = player.openContainer as ClientContainerQuickerCrafting
            val modifiedInv = cont.inventoryItemStacks.take(cont.ClientSlotsStart).toMutableList()
            modifiedInv.removeAt(45)
            modifiedInv.drop(9).toTypedArray()
        } else {
            oldInventory[36] = ItemStack.EMPTY
            oldInventory[37] = ItemStack.EMPTY
            oldInventory[38] = ItemStack.EMPTY
            (player.inventory.mainInventory.drop(9) + player.inventory.mainInventory.take(9)).toTypedArray()
        }

        val changedStacks: MutableList<ItemStack> = mutableListOf()
        inv.forEachIndexed { i, after ->
            val before = oldInventory[i]
            if (!ItemStack.areItemStacksEqual(before, after)) {
                val changed = !ItemStack.areItemStacksEqualUsingNBTShareTag(before, after)
                if (changed) {
                    if (!before.isEmpty) changedStacks.add(before)
                    if (!after.isEmpty) changedStacks.add(after)
                    oldInventory[i] = after
                }
            }
        }
        val craftInv = object : RecipeCalculator.CraftInventory {
            override fun getNormalInv(): Array<ItemStack> {
                return inv.dropLast(3).toTypedArray()
            }

            override fun getCraftResults(): Array<ItemStack> {
                return inv.takeLast(3).toTypedArray()
            }

        }
        populateRecipeCache(craftInv, changedStacks, callback)
    }

    @SideOnly(Side.CLIENT)
    private var running_thread: Thread = Thread()

    @SideOnly(Side.CLIENT)
    fun populateRecipeCache(inventory: RecipeCalculator.CraftInventory, changedStacks: List<ItemStack> = listOf(), callback: (Boolean, Int) -> Unit = { _, _ -> }) {
        if (changedStacks.isEmpty()) return
        // Tell the thread to stop if its running
        running_thread.interrupt()
        // Wait for the thread to die
        running_thread.join()

        // Find all the recipes that the changed item stacks are used in
        val changedRecipes: MutableSet<IRecipe> = mutableSetOf()
        changedStacks.forEach {
            val packed = RecipeItemHelper.pack(it)
            if (RecipeGraph.containsVertex(packed))
                changedRecipes.addAll(RecipeGraph.outgoingEdgesOf(RecipeItemHelper.pack(it)))
        }
        if (changedRecipes.isEmpty()) return
        // Remove only the recipes affected by the changed items
        running_thread = thread(isDaemon = true) {
            var counter = 0
            CraftableRecipes.removeIf { recipe ->
                val result = changedRecipes.any { ItemStack.areItemsEqual(recipe.recipeOutput, it.recipeOutput) }
                if (result) counter += 1
                result
            }
            changedRecipes.forEach { recipe ->
                if (RecipeCalculator.canCraft(inventory, recipe)) {
                    CraftableRecipes.add(recipe)
                    counter += 1
                    callback(false, counter)
                }
            }
            callback(true, counter)
        }
    }
}