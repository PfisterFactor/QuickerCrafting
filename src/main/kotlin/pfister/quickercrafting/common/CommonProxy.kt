package pfister.quickercrafting.common
import net.minecraft.item.Item
import net.minecraftforge.event.RegistryEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.network.NetworkRegistry
import pfister.quickercrafting.LOG
import pfister.quickercrafting.QuickerCrafting
import pfister.quickercrafting.common.gui.GuiHandler
import pfister.quickercrafting.common.item.ItemGuiTester
import pfister.quickercrafting.common.network.PacketHandler
import pfister.quickercrafting.common.util.RecipeCalculator
import kotlin.concurrent.thread
import kotlin.system.measureTimeMillis

// Handles initialization functionality common to client and server
open class CommonProxy {
    // Our thread that loads the RecipeCalc object, and by extension evaluates the SortedRecipes variable
    // That computation is expensive when the recipe registry is very big (i.e. modpacks), so we offload it to another thread while loading
    // If it still is sorting by load completion, we just wait for it to be done
    var sorting_thread: Thread? = null

    @Mod.EventHandler
    open fun preInit(event: FMLPreInitializationEvent) {
        // Register our network packets with forge
        PacketHandler.registerMessages()
    }

    @Mod.EventHandler
    open fun init(event: FMLInitializationEvent) {
        // Register our gui handlers with forge
        NetworkRegistry.INSTANCE.registerGuiHandler(QuickerCrafting, GuiHandler)
        // Start the sorting thread
        sorting_thread = thread {
            val ms = measureTimeMillis { RecipeCalculator.SortedRecipes }
            val ms2 = measureTimeMillis { RecipeCalculator.NonIngredientItems }
            LOG.info("Sorting Recipes took ${ms}ms.")
            LOG.info("Finding non-ingredient items took ${ms2}ms.")
        }
    }

    @Mod.EventHandler
    open fun postInit(event: FMLPostInitializationEvent) {

    }

    @Mod.EventHandler
    open fun loadComplete(event: FMLLoadCompleteEvent) {
        // If we're still sorting recipes, just wait until its done
        sorting_thread?.join()
    }

}

@Suppress("unused")
@Mod.EventBusSubscriber
object CommonEventListener {
    @JvmStatic
    @SubscribeEvent
    fun registerItems(event: RegistryEvent.Register<Item>) {
        // Register our one item with forge
        event.registry.register(ItemGuiTester())
    }


}