package pfister.quickercrafting.common.crafting

import it.unimi.dsi.fastutil.ints.Int2IntAVLTreeMap
import it.unimi.dsi.fastutil.ints.Int2IntMap
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.minecraft.client.Minecraft
import net.minecraft.client.util.RecipeItemHelper
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import pfister.quickercrafting.client.crafting.RecipeWorker
import pfister.quickercrafting.client.gui.ClientContainerQuickerCrafting

// A class that manages updating the RecipeWorker with any changes to the player inventory
@SideOnly(Side.CLIENT)
object InventoryChangeManager {
    // Stores the player's inventory, is compared against every player tick to see if any changes were made
    private var CachedInventory: Int2IntMap = Int2IntAVLTreeMap()

    // Due for refactor
    fun computeChanges(forceRefresh: Boolean = false) {
        val player = Minecraft.getMinecraft().player
        val newInv = Int2IntAVLTreeMap()
        if (player.openContainer is ClientContainerQuickerCrafting) {
            val cont = player.openContainer as ClientContainerQuickerCrafting
            val modifiedInv = cont.inventoryItemStacks.take(cont.ClientSlotsStart).toMutableList()
            modifiedInv.removeAt(45)
            modifiedInv.drop(9)
            modifiedInv.forEach { stack ->
                val packed = RecipeItemHelper.pack(stack)
                if (newInv.containsKey(packed)) {
                    newInv[packed] = newInv.get(packed) + stack.count
                } else {
                    newInv[packed] = stack.count
                }
            }
        } else {
            (player.inventory.mainInventory.drop(9) + player.inventory.mainInventory.take(9)).forEach { stack ->
                val packed = RecipeItemHelper.pack(stack)
                // Don't use merge, it crashes with missing method error because forge ships with
                // fastutil 7 instead of 8
                //newInv.merge(packed, stack.count) { one, two -> one + two }
                if (newInv.containsKey(packed)) {
                    newInv[packed] = newInv.get(packed) + stack.count
                } else {
                    newInv[packed] = stack.count
                }
            }
        }
        if (forceRefresh) {
            RecipeWorker.clearRecipes()
            CachedInventory = Int2IntAVLTreeMap()
        }

        // Compute any changes in them and send it to the recipeworker
        GlobalScope.launch {

            newInv.filter { (key, value) -> CachedInventory.get(key) != value }.forEach { RecipeWorker.sendPackedItemStack(it.key, it.value) }
            CachedInventory.filter { (key, value) -> !newInv.containsKey(key) }.forEach { RecipeWorker.sendPackedItemStack(it.key, 0) }
            CachedInventory = newInv
        }

    }

}