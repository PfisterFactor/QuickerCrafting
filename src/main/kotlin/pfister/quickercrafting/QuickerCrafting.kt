package pfister.quickercrafting

import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.SidedProxy
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import org.apache.logging.log4j.Logger
import pfister.quickercrafting.common.CommonProxy

const val MOD_ID = "quickercrafting"
const val MOD_NAME = "Quicker Crafting"
const val VERSION = "0.1"
lateinit var LOG: Logger

@Mod(modid = MOD_ID, name = MOD_NAME, version = VERSION, modLanguageAdapter = "net.shadowfacts.forgelin.KotlinAdapter", dependencies = "after:inventorytweaks;after:mousetweaks;")
object QuickerCrafting {
    @SidedProxy(
            clientSide = "pfister.quickercrafting.client.ClientProxy",
            serverSide = "pfister.quickercrafting.common.CommonProxy")
    private lateinit var proxy: CommonProxy

    @Mod.EventHandler
    fun preInit(event: FMLPreInitializationEvent) {
        LOG = event.modLog
        proxy.preInit(event)
    }
    @Mod.EventHandler
    fun init(event: FMLInitializationEvent) {
        proxy.init(event)
    }

    @Mod.EventHandler
    fun postInit(event: FMLPostInitializationEvent) {
        proxy.postInit(event)
    }
}
