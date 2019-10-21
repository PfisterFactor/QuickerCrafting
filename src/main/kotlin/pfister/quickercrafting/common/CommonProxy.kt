package pfister.quickercrafting.common
import net.minecraft.item.Item
import net.minecraftforge.event.RegistryEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.network.NetworkRegistry
import pfister.quickercrafting.LOG
import pfister.quickercrafting.QuickerCrafting
import pfister.quickercrafting.common.crafting.RecipeCache
import pfister.quickercrafting.common.gui.GuiHandler
import pfister.quickercrafting.common.item.ItemGuiTester
import pfister.quickercrafting.common.network.PacketHandler
import kotlin.system.measureTimeMillis

// Handles initialization functionality common to client and server
open class CommonProxy {
    @Mod.EventHandler
    open fun preInit(event: FMLPreInitializationEvent) {
        // Register our network packets with forge
        PacketHandler.registerMessages()
    }

    @Mod.EventHandler
    open fun init(event: FMLInitializationEvent) {
        // Register our gui handlers with forge
        NetworkRegistry.INSTANCE.registerGuiHandler(QuickerCrafting, GuiHandler)

    }

    @Mod.EventHandler
    open fun postInit(event: FMLPostInitializationEvent) {
        // Evaluate the lazy variables, thus calculating the item set and recipe graph
        val ms1 = measureTimeMillis { RecipeCache.ItemsUsedInRecipes }
        LOG.info("Building ingredient set took ${ms1}ms.")
        val ms2 = measureTimeMillis { RecipeCache.RecipeGraph }
        LOG.info("Building recipe graph took ${ms2}ms.")
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