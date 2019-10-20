package pfister.quickercrafting.client.gui

import net.minecraft.client.Minecraft
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.inventory.IInventory
import net.minecraft.inventory.InventoryBasic
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.IRecipe
import net.minecraft.util.NonNullList
import net.minecraft.util.text.TextFormatting
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import pfister.quickercrafting.common.gui.ContainerQuickerCrafting
import pfister.quickercrafting.common.gui.NoDragSlot
import pfister.quickercrafting.common.util.RecipeCache
import pfister.quickercrafting.common.util.RecipeCache.CraftableRecipes
import pfister.quickercrafting.common.util.RecipeList
import pfister.quickercrafting.common.util.SearchTree
import pfister.quickercrafting.common.util.collection.IndexedSet
import java.util.*


enum class SlotState {
    ENABLED,
    DISABLED,
    POPULATING,
    EMPTY
}

@SideOnly(Side.CLIENT)
class ClientSlot(inv: IInventory, index: Int, xPos: Int, yPos: Int) : NoDragSlot(inv, index, xPos, yPos) {
    var State: SlotState = SlotState.EMPTY
    var Recipes: RecipeList? = null
    var RecipeIndex: Int = 0

    override fun isEnabled(): Boolean = State == SlotState.ENABLED || State == SlotState.EMPTY
}

@SideOnly(Side.CLIENT)
class ClientContainerQuickerCrafting(playerInv: InventoryPlayer) : ContainerQuickerCrafting(true, playerInv) {
    val ClientSlotsStart: Int = inventorySlots.size

    // Stores all the recipes
    val recipeInventory = InventoryBasic("", false, 27)
    // Suffix tree used for searching -- courtesy of JEI
    var searchTree: SearchTree = SearchTree()
    var shouldDisplayScrollbar = false
    var currentSearchQuery: String = ""
    var isPopulating: Boolean = false
    // The recipes that match our search query, if the search is empty its the same as craftableRecipes
    private var displayedRecipes: IndexedSet<RecipeList> = IndexedSet(Comparator<RecipeList> { rl1, rl2 ->
        val items = Item.REGISTRY
        val r1 = items.indexOfFirst { rl1.first().recipeOutput.item == it }
        val r2 = items.indexOfFirst { rl2.first().recipeOutput.item == it }
        r1 - r2
    })
    private var slotRowYOffset = 0

    init {
        for (y in 0 until 3) {
            for (x in 0 until 9) {
                addSlotToContainer(ClientSlot(recipeInventory, y * 9 + x, 98 + x * 18, 20 + y * 18))
            }
        }
        // Setup things like the search tree, scroll bar,
        onRecipesCalculated(null)
    }

    // Called after populate recipes is done. So we don't reset the scrollbar after every crafting because craftableRecipes isn't fully populated.
    fun checkScrollbar() {
        shouldDisplayScrollbar = displayedRecipes.count() > inventorySlots.size - ClientSlotsStart
    }

    fun updateDisplay(currentScroll: Double, slotUnderMouse: ClientSlot?, forceRefresh: Boolean = false) {
        val length = displayedRecipes.count()
        val rows = (length + 8) / 9 - 3
        slotRowYOffset = ((currentScroll * rows.toDouble()) + 0.5).toInt()
        fun updateSlot(slot: ClientSlot) {
            val recipes = displayedRecipes.getOrNull(slotRowYOffset * 9 + slot.slotNumber - ClientSlotsStart)
            if (recipes != null) {
                slot.putStack(recipes[slot.RecipeIndex].recipeOutput)
                slot.State = SlotState.ENABLED
                slot.Recipes = recipes
            } else {
                slot.putStack(ItemStack.EMPTY)
                slot.State = if (isPopulating) SlotState.POPULATING else SlotState.DISABLED
                slot.Recipes = null
                slot.RecipeIndex = 0
            }
            if (slot.State != SlotState.DISABLED && !forceRefresh && !isPopulating && recipes == slotUnderMouse?.Recipes) {
                slot.State = SlotState.EMPTY
            }
        }

        inventorySlots
                .drop(ClientSlotsStart)
                .map { it as ClientSlot }
                .forEach { slot ->
                    if (!forceRefresh && slotUnderMouse == slot && slot.hasStack) {
                        return@forEach
                    }
                    updateSlot(slot)
                }
        if (slotUnderMouse != null && slotUnderMouse.State != SlotState.DISABLED) {
            if (slotUnderMouse.Recipes?.any { CraftableRecipes.contains(it) } == false) {
                slotUnderMouse.State = SlotState.EMPTY
            } else {
                slotUnderMouse.State = SlotState.ENABLED
            }
        }


    }

    fun handleSearch(query: String) {
        displayedRecipes.clear()
        if (query.isNotBlank()) {
            val craftableRecipesIndexes = searchTree.search(query).fold(setOf<Int>()) { acc, i -> acc + searchTree.getGroupingIndex(i) }
            displayedRecipes.addAll(craftableRecipesIndexes.map { CraftableRecipes[it] }.groupBy { it.recipeOutput.item }.values as Collection<RecipeList>)
            currentSearchQuery = query
        } else {
            displayedRecipes.addAll(CraftableRecipes.groupBy { it.recipeOutput.item }.values as Collection<RecipeList>)
        }
        checkScrollbar()
    }

    // Only sends changes for the slots shared between server and client
    override fun detectAndSendChanges() {
        // If any changes were made to the inventory at all
        var updateRecipes = false
        for (i in 0 until ClientSlotsStart) {
            val after = this.inventorySlots[i].stack
            val before = this.inventoryItemStacks[i].copy()

            if (!ItemStack.areItemStacksEqual(after, before)) {
                val stackChanged = !ItemStack.areItemStacksEqualUsingNBTShareTag(before, after)

                this.inventoryItemStacks[i] = if (after.isEmpty) ItemStack.EMPTY else after.copy()

                if (stackChanged) {
                    listeners.forEach { it.sendSlotContents(this, i, before) }
                    updateRecipes = true
                }
            }
        }
        // Regenerate the possible craftable recipes if any changes were made to the inventory
        if (updateRecipes) {
            RecipeCache.updateCache { onRecipesCalculated(it) }
        }
    }

    private fun onRecipesCalculated(recipe: IRecipe?) {
        if (recipe == null) {
            isPopulating = false
            // Build search tree
            searchTree = SearchTree()
            CraftableRecipes.forEach {
                searchTree.putGrouping(*(it.recipeOutput.getTooltip(PlayerInv.player, if (Minecraft.getMinecraft().gameSettings.advancedItemTooltips) ITooltipFlag.TooltipFlags.ADVANCED else ITooltipFlag.TooltipFlags.NORMAL).map {
                    TextFormatting.getTextWithoutFormattingCodes(it)!!.toLowerCase(Locale.ROOT)
                }.toTypedArray()))
            }

            // Group recipes with the same output
            displayedRecipes.clear()
            displayedRecipes.addAll(CraftableRecipes.groupBy { it.recipeOutput.item }.values as Collection<RecipeList>)

            // Search any query we have
            handleSearch(currentSearchQuery)

            // Check to see if the scrollbar should be enabled
            checkScrollbar()
        } else {
            isPopulating = true
            checkScrollbar()
        }
    }
    // Get all the slots we craft with
    // Excludes the player inventory's crafting matrix and result, plus the armor slots and offhand slot
    override fun getInventory(): NonNullList<ItemStack> {
        val list = NonNullList.create<ItemStack>()
        list.addAll(inventorySlots.take(ClientSlotsStart).mapIndexed { i, elem ->
            if (i < 9 || i == 45) {
                ItemStack.EMPTY
            } else {
                elem.stack
            }
        })
        return list
    }

}