package pfister.quickercrafting.common.gui

import invtweaks.api.container.IgnoreContainer
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.inventory.ContainerPlayer
import net.minecraft.inventory.IInventory
import net.minecraft.inventory.InventoryBasic
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack

open class NoDragSlot(inv: IInventory, index: Int, xPos: Int, yPos: Int) : Slot(inv, index, xPos, yPos) {
    override fun isItemValid(stack: ItemStack): Boolean {
        return false
    }
}


@IgnoreContainer
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

    fun canFitStackInCraftResult(stack:ItemStack): Boolean {
        val workingStack = stack.copy()
        for (i in 0 until quickCraftResult.sizeInventory) {
            val item = quickCraftResult.getStackInSlot(i)
            if (item.isEmpty)
                return true
            else if (ItemStack.areItemsEqual(workingStack, item)) {
                if (item.count + workingStack.count <= item.maxStackSize)
                    return true
                else
                    workingStack.count = workingStack.count - (item.maxStackSize - item.count)
            }
        }
        return false
    }

    fun isCraftResultIndex(index:Int): Boolean {
        return getSlot(index) is NoDragSlot
    }

    override fun canInteractWith(playerIn: EntityPlayer): Boolean {
        return true
    }

    override fun onContainerClosed(playerIn: EntityPlayer) {
        super.onContainerClosed(playerIn)
        for (i in 0 until quickCraftResult.sizeInventory) {
            val stack = quickCraftResult.removeStackFromSlot(i)
            if (!playerIn.inventory.addItemStackToInventory(stack)) {
                playerIn.dropItem(stack, false)
            }
        }
    }

    override fun transferStackInSlot(playerIn: EntityPlayer, index: Int): ItemStack {
        if (this.getSlot(index) !is NoDragSlot) return ItemStack.EMPTY
        val slot = this.inventorySlots[index]
        playerIn.addItemStackToInventory(slot.onTake(playerIn, slot.stack))
        slot.putStack(ItemStack.EMPTY)
        return ItemStack.EMPTY
    }

}