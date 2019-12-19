package pfister.quickercrafting.common.util

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.inventory.InventoryBasic
import net.minecraft.item.ItemStack
import net.minecraft.util.math.BlockPos
import pfister.quickercrafting.common.ConfigValues
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

private var cachedTablePos: BlockPos? = null
fun EntityPlayer.craftingTableInRange(): Boolean {
    // Todo: Make this more efficient
    if (ConfigValues.CraftingTableRadius == 0) return false
    if (ConfigValues.CraftingTableRadius < 0) return true
    if (cachedTablePos != null && this.getDistance(cachedTablePos!!.x.toDouble(), cachedTablePos!!.y.toDouble(), cachedTablePos!!.z.toDouble()) <= ConfigValues.CraftingTableRadius) {
        if (this.world.getBlockState(cachedTablePos!!).block == Blocks.CRAFTING_TABLE) {
            return true
        } else {
            cachedTablePos = null
        }
    }
    val mutBlockPos: BlockPos.MutableBlockPos = BlockPos.MutableBlockPos()
    for (y: Int in 0..ConfigValues.CraftingTableRadius) {
        for (x: Int in 0..ConfigValues.CraftingTableRadius) {
            for (z: Int in 0..ConfigValues.CraftingTableRadius) {
                mutBlockPos.setPos(this.posX.toInt() + x, this.posY.toInt() + y, this.posZ.toInt() + z)
                if (this.world.isBlockLoaded(mutBlockPos) && this.world.getBlockState(mutBlockPos).block == Blocks.CRAFTING_TABLE) {
                    cachedTablePos = mutBlockPos
                    return true
                }
                mutBlockPos.setPos(this.posX.toInt() - x, this.posY.toInt() - y, this.posZ.toInt() - z)
                if (this.world.isBlockLoaded(mutBlockPos) && this.world.getBlockState(mutBlockPos).block == Blocks.CRAFTING_TABLE) {
                    cachedTablePos = mutBlockPos
                    return true
                }
            }
        }
    }
    cachedTablePos = null
    return false

}

// Thanks kotlin standard library somehow you have an indexed foreach function but not a remove on immutable lists
fun <E> List<E>.without(vararg elems: E): List<E> {
    return fold(listOf()) { acc, i ->
        if (!elems.contains(i)) {
            acc + i
        } else
            acc
    }
}

fun <E> List<E>.without(vararg indexes: Int): List<E> {
    return foldIndexed(this.javaClass.newInstance()) { i, acc, elem ->
        if (!indexes.contains(i)) {
            acc + elem
        } else {
            acc
        }
    }
}
