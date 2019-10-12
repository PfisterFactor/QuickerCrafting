package pfister.quickercrafting.client.gui

import net.minecraft.client.Minecraft
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.inventory.IInventory
import net.minecraft.inventory.InventoryBasic
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.IRecipe
import net.minecraft.util.NonNullList
import net.minecraft.util.text.TextFormatting
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import pfister.quickercrafting.common.gui.ContainerQuickerCrafting
import pfister.quickercrafting.common.gui.NoDragSlot
import pfister.quickercrafting.common.util.RecipeCalculator
import java.util.*


enum class SlotState {
    ENABLED,
    DISABLED,
    EMPTY
}

@SideOnly(Side.CLIENT)
class ClientSlot(inv:IInventory, index:Int, xPos:Int, yPos:Int): NoDragSlot(inv,index,xPos, yPos) {
    var State: SlotState = SlotState.EMPTY
    var Recipe: IRecipe? = null

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
    var craftableRecipes: MutableList<IRecipe> = mutableListOf()

    private var displayedRecipes: List<IRecipe> = craftableRecipes
    var currentSearchQuery: String = ""
    var ignoreExemption: Boolean = false

    init {
        for (y in 0 until 3) {
            for (x in 0 until 9) {
                addSlotToContainer(ClientSlot(recipeInventory, y * 9 + x, 98 + x * 18, 20 + y * 18))
            }
        }
    }

    fun updateDisplay(currentScroll:Double, exemptSlotIndex:Int) {
        val length = displayedRecipes.count()
        val rows = (length + 8) / 9 - 3
        slotRowYOffset = ((currentScroll * rows.toDouble()) + 0.5).toInt()
        shouldDisplayScrollbar = length > inventorySlots.size - ClientSlotsStart
        inventorySlots
                .drop(ClientSlotsStart)
                .filterNot { it.slotNumber == exemptSlotIndex && !ignoreExemption }
                .map { it as ClientSlot}
                .forEach { slot ->
                    val recipe = displayedRecipes.getOrNull(slotRowYOffset * 9 + slot.slotNumber - ClientSlotsStart)
                    if (recipe != null) {
                        slot.putStack(recipe.recipeOutput)
                        slot.State = SlotState.ENABLED
                        slot.Recipe = recipe
                    } else {
                        slot.putStack(ItemStack.EMPTY)
                        slot.State = SlotState.DISABLED
                        slot.Recipe = null
                    }
                }
        val exemptSlot: ClientSlot? =
                if (!ignoreExemption && exemptSlotIndex != -1 && exemptSlotIndex >= ClientSlotsStart)
            getSlot(exemptSlotIndex) as ClientSlot
        else
            null

        if (!ignoreExemption && exemptSlot != null && exemptSlot.State != SlotState.DISABLED && exemptSlot.Recipe != null && !craftableRecipes.contains(exemptSlot.Recipe!!))
            exemptSlot.State = SlotState.EMPTY
        ignoreExemption = false

    }

    fun handleSearch(query: String) {
        if (query.isNotBlank()) {
            displayedRecipes = craftableRecipes.filter {
                val tooltip = it.recipeOutput.getTooltip(PlayerInv.player, if (Minecraft.getMinecraft().gameSettings.advancedItemTooltips) ITooltipFlag.TooltipFlags.ADVANCED else ITooltipFlag.TooltipFlags.NORMAL)
                for (line in tooltip) {
                    if (TextFormatting.getTextWithoutFormattingCodes(line)!!.toLowerCase(Locale.ROOT).contains(query, true)) {
                        return@filter true
                    }
                }
                return@filter false
            }
            ignoreExemption = true
            currentSearchQuery = query
        }
        else {
            displayedRecipes = craftableRecipes
        }
    }
    // Only sends changes for the slots shared between server and client
    override fun detectAndSendChanges() {
        // If any changes were made to the inventory at all
        var stackChangedFlag = false
        for (i in 0 until ClientSlotsStart) {
            val itemstack = this.inventorySlots[i].stack
            var itemstack1 = this.inventoryItemStacks[i]

            if (!ItemStack.areItemStacksEqual(itemstack, itemstack1)) {
                val stackChanged = !ItemStack.areItemStacksEqualUsingNBTShareTag(itemstack1, itemstack)
                itemstack1 = if (itemstack.isEmpty) ItemStack.EMPTY else itemstack.copy()

                this.inventoryItemStacks[i] = itemstack1

                if (stackChanged) {
                    listeners.forEach { it.sendSlotContents(this, i, itemstack1) }
                    stackChangedFlag = true
                }
            }
            }
        // Regenerate the possible craftable recipes if any changes were made to the inventory
        if (stackChangedFlag) {
            RecipeCalculator.populateRecipeList(craftableRecipes)
            displayedRecipes = craftableRecipes
            handleSearch(currentSearchQuery)


        }

    }
    override fun getInventory(): NonNullList<ItemStack> {
        val list = NonNullList.create<ItemStack>()
        list.addAll(super.getInventory().take(ClientSlotsStart))
        return list
    }

}