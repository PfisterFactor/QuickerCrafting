package pfister.quickercrafting.client

import net.minecraftforge.client.event.ModelRegistryEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.relauncher.Side
import pfister.quickercrafting.common.CommonProxy
import pfister.quickercrafting.common.item.ModItems

class ClientProxy : CommonProxy() {
    override fun preInit(event: FMLPreInitializationEvent) {
        super.preInit(event)
    }

    override fun init(event: FMLInitializationEvent) {
        super.init(event)
    }

    override fun postInit(event: FMLPostInitializationEvent) {
        super.postInit(event)
    }
}
@Mod.EventBusSubscriber(Side.CLIENT)
object ClientEventListener {
    @JvmStatic
    @SubscribeEvent
    fun registerItemModels(event: ModelRegistryEvent) {
        ModItems.initModels()
    }
}