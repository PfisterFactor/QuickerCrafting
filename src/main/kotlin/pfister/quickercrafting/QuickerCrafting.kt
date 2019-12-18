package pfister.quickercrafting

import net.minecraftforge.common.config.Config
import net.minecraftforge.common.config.ConfigManager
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.SidedProxy
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import org.apache.logging.log4j.Logger
import pfister.quickercrafting.common.CommonProxy

// Some constants related to forge
const val MOD_ID = "quickercrafting"
const val MOD_NAME = "Quicker Crafting"
const val VERSION = "0.1"
//

// Our logger <3
lateinit var LOG: Logger


@Mod(modid = MOD_ID, name = MOD_NAME, version = VERSION, modLanguageAdapter = "net.shadowfacts.forgelin.KotlinAdapter", dependencies = "after:inventorytweaks;after:mousetweaks;")
object QuickerCrafting {
    @SidedProxy(
            clientSide = "pfister.quickercrafting.client.ClientProxy",
            serverSide = "pfister.quickercrafting.common.CommonProxy")

    // Reference to our proxy
    private lateinit var proxy: CommonProxy

    @Mod.EventHandler
    fun preInit(event: FMLPreInitializationEvent) {
        LOG = event.modLog
        proxy.preInit(event)
    }
    @Mod.EventHandler
    fun init(event: FMLInitializationEvent) {
        ConfigManager.sync(MOD_ID, Config.Type.INSTANCE)
        proxy.init(event)
    }

    @Mod.EventHandler
    fun postInit(event: FMLPostInitializationEvent) {
        proxy.postInit(event)
    }

    @Mod.EventHandler
    fun loadComplete(event: FMLLoadCompleteEvent) {
        proxy.loadComplete(event)
    }

}
