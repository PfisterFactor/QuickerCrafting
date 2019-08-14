package pfister.quickercrafting.common
import net.minecraft.item.Item
import net.minecraftforge.event.RegistryEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.network.NetworkRegistry
import pfister.quickercrafting.QuickerCrafting
import pfister.quickercrafting.common.gui.GuiHandler
import pfister.quickercrafting.common.item.ItemGuiTester
import pfister.quickercrafting.common.network.PacketHandler

open class CommonProxy {
    @Mod.EventHandler
    open fun preInit(event: FMLPreInitializationEvent) {
        PacketHandler.registerMessages()
    }
    @Mod.EventHandler
    open fun init(event: FMLInitializationEvent) {
        NetworkRegistry.INSTANCE.registerGuiHandler(QuickerCrafting, GuiHandler)
    }

    @Mod.EventHandler
    open fun postInit(event: FMLPostInitializationEvent) {
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