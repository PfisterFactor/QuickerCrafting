package pfister.quickercrafting.common.gui

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.inventory.ContainerPlayer
import net.minecraft.inventory.IInventory
import net.minecraft.inventory.InventoryBasic
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack
import pfister.quickercrafting.common.crafting.RecipeCalculator
import pfister.quickercrafting.common.util.condensedAdd

open class NoDragSlot(inv: IInventory, index: Int, xPos: Int, yPos: Int) : Slot(inv, index, xPos, yPos) {
    override fun isItemValid(stack: ItemStack): Boolean {
        return false
    }
}


open class ContainerQuickerCrafting(localWorld: Boolean = false, val PlayerInv: InventoryPlayer) : ContainerPlayer(PlayerInv, localWorld, PlayerInv.player) {
    val quickCraftResult = InventoryBasic("", false, 3)
    init {
        inventorySlots.take(5).forEach {
            it.xPos = -18
            it.yPos = -18
        }
        inventorySlots.drop(9).take(36).forEach {
            it.yPos += 4
        }

        // The slots on the right of the inventory
        for (i in 0 until 3) {
            addSlotToContainer(NoDragSlot(quickCraftResult, i, 184, 88 + i * 18))
        }
    }

    fun canFitStacksInCraftResult(stacks: List<ItemStack>): Boolean {
        if (stacks.isEmpty()) return true
        val slotStacks: Array<ItemStack> = Array(quickCraftResult.sizeInventory) { quickCraftResult.getStackInSlot(it).copy() }
        val workingStacks: MutableList<ItemStack> = stacks.map { it.copy() }.toMutableList()
        slotStacks.any { slotStack ->
            if (slotStack.isEmpty) {
                workingStacks.removeAt(0)
            } else {
                val otherStack = workingStacks.find { ItemStack.areItemsEqual(slotStack, it) }
                if (otherStack != null) {
                    if (slotStack.count + otherStack.count <= slotStack.maxStackSize) {
                        workingStacks.remove(otherStack)
                    } else {
                        otherStack.count -= slotStack.maxStackSize - slotStack.count
                    }
                }
            }
            workingStacks.isEmpty()
        }
        return workingStacks.isEmpty() || workingStacks.all { it.count == 0 }

    }

    fun isCraftResultIndex(index:Int): Boolean {
        return getSlot(index) is NoDragSlot
    }

    override fun canInteractWith(playerIn: EntityPlayer): Boolean {
        return true
    }

    override fun onContainerClosed(playerIn: EntityPlayer) {
        super.onContainerClosed(playerIn)
        // Can't use clearcontainer here because packet delays screw things up
        (0 until quickCraftResult.sizeInventory).map { quickCraftResult.getStackInSlot(it) }.forEach { item ->
            val leftover = playerIn.inventory.condensedAdd(item)
            if (leftover != ItemStack.EMPTY) {
                playerIn.dropItem(item, false)
            }

        }
        quickCraftResult.clear()
    }

    override fun transferStackInSlot(playerIn: EntityPlayer, index: Int): ItemStack {
        if (this.getSlot(index) !is NoDragSlot) return ItemStack.EMPTY
        val slot = this.inventorySlots[index]
        playerIn.addItemStackToInventory(slot.onTake(playerIn, slot.stack))
        slot.putStack(ItemStack.EMPTY)
        return ItemStack.EMPTY
    }


    // Get all the slots we craft with
    // Excludes the player inventory's crafting matrix and result, plus the armor slots and offhand slot
    fun getCraftInventory(): RecipeCalculator.CraftInventory {
        return object : RecipeCalculator.CraftInventory {
            override fun getNormalInv(): Array<ItemStack> {
                return inventory.dropLast(3).toTypedArray()
            }

            override fun getCraftResults(): Array<ItemStack> {
                return (0 until 3).map { quickCraftResult.getStackInSlot(it) }.toTypedArray()
            }

        }
    }

}