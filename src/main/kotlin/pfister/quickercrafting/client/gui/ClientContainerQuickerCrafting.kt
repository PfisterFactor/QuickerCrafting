package pfister.quickercrafting.client.gui

import net.minecraft.client.Minecraft
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.inventory.IInventory
import net.minecraft.inventory.InventoryBasic
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.IRecipe
import net.minecraft.util.NonNullList
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import pfister.quickercrafting.client.crafting.RecipeWorker
import pfister.quickercrafting.common.crafting.InventoryChangeManager
import pfister.quickercrafting.common.gui.ContainerQuickerCrafting
import pfister.quickercrafting.common.gui.NoDragSlot
import pfister.quickercrafting.common.util.canQuickCraft3x3


enum class SlotState {
    ENABLED,
    DISABLED,
    POPULATING,
    EMPTY
}

@SideOnly(Side.CLIENT)

class ClientSlot(inv: IInventory, index: Int, xPos: Int, yPos: Int) : NoDragSlot(inv, index, xPos, yPos) {
    var State: SlotState = SlotState.EMPTY
    var Recipe: IRecipe? = null

    override fun isEnabled(): Boolean = State == SlotState.ENABLED || State == SlotState.EMPTY
}

@SideOnly(Side.CLIENT)
class ClientContainerQuickerCrafting(playerInv: InventoryPlayer) : ContainerQuickerCrafting(true, playerInv) {
    companion object {
        const val ROW_LENGTH = 9

    }

    val ClientSlotsStart: Int = inventorySlots.size

    // Whether or not 3x3 recipes can be crafted
    var CanCraft3x3 = false

    // Stores all the recipes
    private val recipeInventory = InventoryBasic("", false, 27)
    var shouldDisplayScrollbar = false

    private var slotRowYOffset = 0

    init {
        for (y in 0 until 3) {
            for (x in 0 until ROW_LENGTH) {
                addSlotToContainer(ClientSlot(recipeInventory, y * 9 + x, 98 + x * 18, 20 + y * 18))
            }
        }

    }

    fun onGuiOpened() {
        CanCraft3x3 = Minecraft.getMinecraft().player.canQuickCraft3x3()
        checkScrollbar(0)
    }

    // Checks if we should have a scrollbar enabled if the based on how many recipes we have
    private fun checkScrollbar(recipeCount: Int) {
        shouldDisplayScrollbar = recipeCount > inventorySlots.size - ClientSlotsStart
    }

    fun updateDisplay(currentScroll: Double, slotUnderMouse: ClientSlot?, forceRefresh: Boolean = false) {
        DisplayedRecipeFilter.set3x3(CanCraft3x3)
        val displayedRecipes: List<IRecipe> = DisplayedRecipeFilter.getRecipesToDisplay()
        checkScrollbar(displayedRecipes.size)
        val length = displayedRecipes.count()
        val rows = (length + (ROW_LENGTH - 1)) / ROW_LENGTH - 3
        slotRowYOffset = ((currentScroll * rows.toDouble()) + 0.5).toInt()
        fun updateSlot(slot: ClientSlot) {
            val recipe = displayedRecipes.getOrNull(slotRowYOffset * ROW_LENGTH + slot.slotNumber - ClientSlotsStart)
            if (recipe != null) {
                slot.putStack(recipe.recipeOutput)
                slot.State = SlotState.ENABLED
                slot.Recipe = recipe
            } else {
                slot.putStack(ItemStack.EMPTY)
                slot.State = //if (RecipeCache.isPopulating()) SlotState.POPULATING else
                        SlotState.DISABLED
                slot.Recipe = null
            }
            if (slot.State != SlotState.DISABLED && !forceRefresh /*!RecipeCache.isPopulating()*/ && recipe == slotUnderMouse?.Recipe) {
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
            if (!RecipeWorker.CraftableRecipes.contains(slotUnderMouse.Recipe)) {
                slotUnderMouse.State = SlotState.EMPTY
            } else {
                slotUnderMouse.State = SlotState.ENABLED
            }
        }
    }

    fun handleSearch(query: String) {
        DisplayedRecipeFilter.setSearchTerm(query)
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
            InventoryChangeManager.computeChanges()
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