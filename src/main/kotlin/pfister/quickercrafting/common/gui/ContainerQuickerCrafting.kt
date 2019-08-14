package pfister.quickercrafting.common.gui

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.inventory.Container
import net.minecraft.inventory.IInventory
import net.minecraft.inventory.InventoryBasic
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack

open class NoDragSlot(inv: IInventory, index: Int, xPos: Int, yPos: Int) : Slot(inv, index, xPos, yPos) {
    override fun isItemValid(stack: ItemStack): Boolean {
        return false
    }
}

open class ContainerQuickerCrafting(val PlayerInv: InventoryPlayer) : Container() {
    val craftResult = InventoryBasic("", false, 3)

    init {
        // Handle the player's inventory rendering
        for (y in 0 until 3) {
            for (x in 0 until 9) {
                addSlotToContainer(
                        Slot(PlayerInv, y * 9 + x + 9,
                                8 + x * 18,
                                90 + y * 18)
                )
            }
        }
        for (hotbarIndex in 0 until 9) {
            addSlotToContainer(Slot(PlayerInv,hotbarIndex,8 + hotbarIndex * 18, 148))
        }
        //
        for (i in 0 until 3) {
            addSlotToContainer(NoDragSlot(craftResult,i, 184, 90 + i * 18))
        }
    }

    fun canFitStackInCraftResult(stack:ItemStack): Boolean {
        val workingStack = stack.copy()
        for (i in 0 until craftResult.sizeInventory) {
            val item = craftResult.getStackInSlot(i)
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
        for (i in 0 until craftResult.sizeInventory) {
            val stack = craftResult.removeStackFromSlot(i)
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