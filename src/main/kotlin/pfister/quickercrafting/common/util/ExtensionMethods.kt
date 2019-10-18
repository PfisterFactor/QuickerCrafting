package pfister.quickercrafting.common.util

import net.minecraft.inventory.InventoryBasic
import net.minecraft.item.ItemStack
import kotlin.math.min

// Returns true if two itemstacks can stack
fun ItemStack.canStack(other: ItemStack): Boolean {
    return !this.isEmpty &&
            !other.isEmpty &&
            this.isItemEqual(other) &&
            this.isStackable &&
            (!this.hasSubtypes || (this.itemDamage == other.itemDamage)) &&
            ItemStack.areItemStackTagsEqual(this, other) &&
            other.count + this.count <= other.maxStackSize
}

// Attempts to add an itemstack to an inventory
// The returning itemstack is whats left after we cram as much as possible into the inventory
// Doesn't mutate the argument
fun InventoryBasic.condensedAdd(itemStackToAdd: ItemStack): ItemStack {
    val copied = itemStackToAdd.copy()
    val size = this.sizeInventory
    var emptySlotIndex = -1
    var condensedSlotIndex = -1
    for (i in 0 until size) {
        val stack = this.getStackInSlot(i)
        if (stack.isEmpty && emptySlotIndex == -1)
            emptySlotIndex = i
        else if (ItemStack.areItemsEqual(copied, stack) && copied.count + stack.count <= copied.maxStackSize && condensedSlotIndex == -1)
            condensedSlotIndex = i
    }

    if (condensedSlotIndex != -1) {
        val stack = this.getStackInSlot(condensedSlotIndex)
        val j = min(this.inventoryStackLimit, stack.maxStackSize)
        val k = min(copied.count, j - stack.count)

        if (k > 0) {
            stack.grow(k)
            copied.shrink(k)
            if (copied.isEmpty) {
                this.markDirty()
                return ItemStack.EMPTY
            }
        }
    } else if (emptySlotIndex != -1) {
        this.setInventorySlotContents(emptySlotIndex, copied)
        this.markDirty()
        return ItemStack.EMPTY
    }
    if (copied.count != itemStackToAdd.count)
        this.markDirty()

    return copied
}