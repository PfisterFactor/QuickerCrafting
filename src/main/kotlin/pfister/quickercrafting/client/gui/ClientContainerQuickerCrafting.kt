package pfister.quickercrafting.client.gui

import net.minecraft.client.Minecraft
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.inventory.IInventory
import net.minecraft.inventory.InventoryBasic
import net.minecraft.item.ItemStack
import net.minecraft.util.NonNullList
import net.minecraft.util.text.TextFormatting
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import pfister.quickercrafting.common.gui.ContainerQuickerCrafting
import pfister.quickercrafting.common.gui.NoDragSlot
import pfister.quickercrafting.common.util.RecipeCalculator
import pfister.quickercrafting.common.util.RecipeList
import java.util.*


enum class SlotState {
    ENABLED,
    DISABLED,
    EMPTY
}

@SideOnly(Side.CLIENT)
class ClientSlot(inv:IInventory, index:Int, xPos:Int, yPos:Int): NoDragSlot(inv,index,xPos, yPos) {
    var State: SlotState = SlotState.EMPTY
    var Recipes: RecipeList? = null
    var RecipeIndex: Int = 0

    override fun isEnabled(): Boolean = State == SlotState.ENABLED || State == SlotState.EMPTY
}

@SideOnly(Side.CLIENT)
class ClientContainerQuickerCrafting(playerInv: InventoryPlayer) : ContainerQuickerCrafting(true, playerInv) {
    val RecipeCalculator: RecipeCalculator = RecipeCalculator(this)
    val ClientSlotsStart: Int = inventorySlots.size

    // Stores all the recipes
    val recipeInventory = InventoryBasic("",false, 27)
    var shouldDisplayScrollbar = false
    private var slotRowYOffset = 0
    // This list is asynchronously populated, so don't check its size or something synchronously
    var craftableRecipes: MutableList<RecipeList> = mutableListOf()

    // The recipes that match our search query, if the search is empty its the same as craftableRecipes
    private var displayedRecipes: List<RecipeList> = craftableRecipes
    var currentSearchQuery: String = ""
    var ignoreExemption: Boolean = false

    init {
        for (y in 0 until 3) {
            for (x in 0 until 9) {
                addSlotToContainer(ClientSlot(recipeInventory, y * 9 + x, 98 + x * 18, 20 + y * 18))
            }
        }
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
            if (recipes != null && (recipes != slotUnderMouse?.Recipes || forceRefresh)) {
                slot.putStack(recipes[slot.RecipeIndex].recipeOutput)
                slot.State = SlotState.ENABLED
                slot.Recipes = recipes
            } else {
                slot.putStack(ItemStack.EMPTY)
                slot.State = SlotState.DISABLED
                slot.Recipes = null
                slot.RecipeIndex = 0
            }
        }

        inventorySlots
                .drop(ClientSlotsStart)
                .map { it as ClientSlot}
                .forEach { slot ->
                    if (!forceRefresh && slotUnderMouse == slot) {
                        return@forEach
                    }
                    updateSlot(slot)
                }
        if (slotUnderMouse != null && slotUnderMouse.State != SlotState.DISABLED) {
            if (!craftableRecipes.contains(slotUnderMouse.Recipes)) {
                slotUnderMouse.State = SlotState.EMPTY
            } else {
                slotUnderMouse.State = SlotState.ENABLED
            }
        }


    }

    fun handleSearch(query: String) {
        if (query.isNotBlank()) {
            displayedRecipes = craftableRecipes.filter {
                val tooltip = it.first().recipeOutput.getTooltip(PlayerInv.player, if (Minecraft.getMinecraft().gameSettings.advancedItemTooltips) ITooltipFlag.TooltipFlags.ADVANCED else ITooltipFlag.TooltipFlags.NORMAL)
                for (line in tooltip) {
                    if (TextFormatting.getTextWithoutFormattingCodes(line)!!.toLowerCase(Locale.ROOT).contains(query, true)) {
                        return@filter true
                    }
                }
                return@filter false
            }
            ignoreExemption = true
            currentSearchQuery = query
            checkScrollbar()
        }
        else {
            displayedRecipes = craftableRecipes
        }
    }
    // Only sends changes for the slots shared between server and client
    override fun detectAndSendChanges() {
        // If any changes were made to the inventory at all
        var updateRecipes = false
        for (i in 0 until ClientSlotsStart) {
            val itemstack = this.inventorySlots[i].stack
            var itemstack1 = this.inventoryItemStacks[i]

            if (!ItemStack.areItemStacksEqual(itemstack, itemstack1)) {
                val stackChanged = !ItemStack.areItemStacksEqualUsingNBTShareTag(itemstack1, itemstack)
                itemstack1 = if (itemstack.isEmpty) ItemStack.EMPTY else itemstack.copy()

                this.inventoryItemStacks[i] = itemstack1

                if (stackChanged) {
                    listeners.forEach { it.sendSlotContents(this, i, itemstack1) }
                    if (i >= 9 && i != 45) {
                        updateRecipes = true
                    }

                }
            }
            }
        // Regenerate the possible craftable recipes if any changes were made to the inventory
        if (updateRecipes) {
            RecipeCalculator.populateRecipeList(craftableRecipes) { checkScrollbar() }
            displayedRecipes = craftableRecipes
            handleSearch(currentSearchQuery)

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