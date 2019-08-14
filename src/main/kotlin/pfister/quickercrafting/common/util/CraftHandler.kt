package pfister.quickercrafting.common.util

import net.minecraft.item.crafting.IRecipe
import pfister.quickercrafting.LOG
import pfister.quickercrafting.common.gui.ContainerQuickerCrafting

// Handles the functionality for crafting something
object CraftHandler {
    // Attempts to craft a recipe given the items within the container
    fun tryCraftRecipe(container: ContainerQuickerCrafting, recipe: IRecipe): Boolean {
        val recipeCalculator = RecipeCalculator(container)
        val isServer = !container.PlayerInv.player.world.isRemote
        // Get a map of items used and how much are used
        val itemsToRemove: Map<Int,Int>? = recipeCalculator.doCraft(recipe)
        if (itemsToRemove == null) {
            if (isServer)
                LOG.warn("MessageCraftItemHandler: Recipe '${recipe.registryName.toString()}' cannot be crafted from ${container.PlayerInv.player.displayNameString}'s inventory on server.")
            return false
        }
        if (!container.canFitStackInCraftResult(recipe.recipeOutput)) {
            // Detect to see if after crafting there will be an open slot in the craft result slots, if there is we can put the item there
            val willCraftResultItemBeConsumed = itemsToRemove?.entries?.any { (key: Int, value: Int) ->
                container.isCraftResultIndex(key) && container.getSlot(value).stack.count <= value
            }
            if (!willCraftResultItemBeConsumed) {
                if (isServer)
                    LOG.warn("MessageCraftItemHandler: Cannot stack '${recipe.registryName.toString()}' into item slot on server.")
                return false
            }
        }
        // Remove all items used during crafting
        itemsToRemove.entries.forEach { (key: Int, value: Int) ->
            val slot = container.getSlot(key)
            slot.decrStackSize(value)
        }

        // Get the recipe output itemstack
        // Todo: This will not work on special recipes (Repairing, Cloning Books, Fireworks, etc...)
        val recipeOutput = recipe.recipeOutput.copy()
        val leftOver = container.craftResult.condensedAdd(recipeOutput)
        if (!leftOver.isEmpty) {
            container.PlayerInv.player.dropItem(recipeOutput, false)
        }
        return true
    }
}