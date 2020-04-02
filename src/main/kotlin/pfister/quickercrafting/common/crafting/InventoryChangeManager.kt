package pfister.quickercrafting.common.crafting

import it.unimi.dsi.fastutil.ints.Int2IntAVLTreeMap
import it.unimi.dsi.fastutil.ints.Int2IntMap
import kotlinx.coroutines.runBlocking
import net.minecraft.client.Minecraft
import net.minecraft.client.util.RecipeItemHelper
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import pfister.quickercrafting.client.RecipeWorker
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
                newInv.merge(packed, stack.count) { one, two -> one + two }
            }
        } else {
            (player.inventory.mainInventory.drop(9) + player.inventory.mainInventory.take(9)).forEach { stack ->
                val packed = RecipeItemHelper.pack(stack)
                newInv.merge(packed, stack.count) { one, two -> one + two }
            }
        }
        if (forceRefresh) {
            RecipeWorker.clearRecipes()
            CachedInventory = Int2IntAVLTreeMap()
        }

        // Compute any changes in them and send it to the recipeworker
        newInv.filter { (key, value) -> CachedInventory.get(key) != value }.forEach { runBlocking { RecipeWorker.sendPackedItemStack(it.key, it.value) } }
        CachedInventory.filter { (key, value) -> !newInv.containsKey(key) }.forEach { runBlocking { RecipeWorker.sendPackedItemStack(it.key, 0) } }
        CachedInventory = newInv

    }

}