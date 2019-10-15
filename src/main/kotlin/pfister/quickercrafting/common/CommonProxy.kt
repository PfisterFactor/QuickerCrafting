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

open class CommonProxy {
    var running_thread: Thread? = null

    @Mod.EventHandler
    open fun preInit(event: FMLPreInitializationEvent) {
        PacketHandler.registerMessages()
    }
    @Mod.EventHandler
    open fun init(event: FMLInitializationEvent) {
        NetworkRegistry.INSTANCE.registerGuiHandler(QuickerCrafting, GuiHandler)
        running_thread = thread {
            val ms = measureTimeMillis { RecipeCalculator.SortedRecipes }
            LOG.info("Sorting Recipes took ${ms}ms.")
        }
    }
    @Mod.EventHandler
    open fun postInit(event: FMLPostInitializationEvent) {

    }

    @Mod.EventHandler
    open fun finishSorting(event: FMLLoadCompleteEvent) {
        // If we're still sorting recipes, just wait until its done
        running_thread?.join()
    }

}

@Suppress("unused")
@Mod.EventBusSubscriber
object CommonEventListener {
    @JvmStatic
    @SubscribeEvent
    fun registerItems(event: RegistryEvent.Register<Item>) {
        event.registry.register(ItemGuiTester())
    }


}