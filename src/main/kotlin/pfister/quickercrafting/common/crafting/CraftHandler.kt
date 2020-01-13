package pfister.quickercrafting.common.crafting

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.Container
import net.minecraft.inventory.InventoryCrafting
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.IRecipe
import net.minecraft.item.crafting.Ingredient
import pfister.quickercrafting.LOG
import pfister.quickercrafting.common.gui.ContainerQuickerCrafting
import pfister.quickercrafting.common.util.condensedAdd

// Handles the functionality for crafting something
object CraftHandler {
    // Attempts to put the item map into a crafting matrix and get the result that way
    // Respects items that are reused, like buckets or something
    // If it fails it just uses recipeOutput
    fun getRecipeOutput(container: ContainerQuickerCrafting, info: RecipeCalculator.CraftingInfo, recipe: IRecipe): List<ItemStack> {
        // A fake crafting matrix to pass to the recipe
        val fakeCraftingInv = InventoryCrafting(object : Container() {
            override fun canInteractWith(playerIn: EntityPlayer): Boolean = true
        }, 3, 3)

        // Populate the crafting matrix according to their indexes in the ingredients list within IRecipe
        recipe.ingredients.forEachIndexed { index, ingredient ->
            if (ingredient != Ingredient.EMPTY) {
                val containerIndex = info.ItemMap.keys.find { ingredient.apply(container.getSlot(it).stack) }
                        ?: return listOf(recipe.recipeOutput.copy())
                val stack = container.getSlot(containerIndex).stack.copy()
                stack.count = 1
                fakeCraftingInv.setInventorySlotContents(index, stack)
            }
        }
        return if (recipe.matches(fakeCraftingInv, container.PlayerInv.player.world)) {
            listOf(recipe.getCraftingResult(fakeCraftingInv)) + recipe.getRemainingItems(fakeCraftingInv).filterNot { it.isEmpty }
        } else {
            listOf(recipe.recipeOutput.copy())
        }

    }
    // Attempts to craft a recipe given the items within the container
    // The shift argument will recursively try to craft the recipe until we run out of ingredients or the  crafting inventory fills up
    fun tryCraftRecipe(container: ContainerQuickerCrafting, recipe: IRecipe, shift: Boolean = false): Boolean {
        val isServer = !container.PlayerInv.player.world.isRemote
        // Get a map of items used and how much are used
        val itemsToRemove: RecipeCalculator.CraftingInfo = RecipeCalculator.doCraft(container.getCraftInventory(), recipe)
        if (!itemsToRemove.canCraft()) {
            // If we can't craft any more on the server, and we aren't shift crafting this recipe, display a warning
            if (isServer && !shift) {
                LOG.warn("MessageCraftItemHandler: Recipes '${recipe.registryName}' cannot be crafted from ${container.PlayerInv.player.displayNameString}'s inventory on server.")
            }
            return false
        }
        // Get the output from a simulated matrix, if supported, or just the regular old recipeOutput
        val output = getRecipeOutput(container, itemsToRemove, recipe)
        val slotStacks: Array<ItemStack> = Array(container.quickCraftResult.sizeInventory) { container.quickCraftResult.getStackInSlot(it).copy() }
        // Detect to see if after crafting there will be an open slot in the craft result slots, if there is we can put the item there
        itemsToRemove.ItemMap.entries
                .filter { container.isCraftResultIndex(it.key) }
                .forEach { (key, value) ->
                    container.getSlot(key).decrStackSize(value)
                }
        // Make sure we can fit the craft result and any remaining items in the matrix into the crafting window
        if (!container.canFitStacksInCraftResult(output)) {
            // Restore stacks we removed
            slotStacks.forEachIndexed { index, itemStack -> container.quickCraftResult.setInventorySlotContents(index, itemStack) }
            if (isServer && !shift) {
                LOG.warn("MessageCraftItemHandler: Cannot stack '${recipe.registryName.toString()}' into item slot on server.")
            }
            return false
        }
        // Remove all items used during crafting
        itemsToRemove.ItemMap.entries
                .filterNot { container.isCraftResultIndex(it.key) }
                .forEach { (key: Int, value: Int) ->
                    container.getSlot(key).decrStackSize(value)
                }

        // Try to put the recipe output in the inventory and get the itemstacks that couldn't fit
        val leftOvers: List<ItemStack> = output.fold(listOf()) { acc, i ->
            acc + container.quickCraftResult.condensedAdd(i)
        }

        leftOvers.forEach {
            if (!it.isEmpty) {
                container.PlayerInv.player.dropItem(it, false)
            }
        }
        if (shift) {
            tryCraftRecipe(container, recipe, true)
        }
        return true
    }
}