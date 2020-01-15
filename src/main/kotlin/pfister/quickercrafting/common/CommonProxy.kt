package pfister.quickercrafting.common
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraftforge.common.config.Config
import net.minecraftforge.common.config.ConfigManager
import net.minecraftforge.event.entity.EntityJoinWorldEvent
import net.minecraftforge.fml.client.event.ConfigChangedEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.network.NetworkRegistry
import pfister.quickercrafting.LOG
import pfister.quickercrafting.MOD_ID
import pfister.quickercrafting.QuickerCrafting
import pfister.quickercrafting.common.gui.GuiHandler
import pfister.quickercrafting.common.network.PacketHandler

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

    }

    @Mod.EventHandler
    open fun loadComplete(event: FMLLoadCompleteEvent) {

    }


}

@Suppress("unused")
@Mod.EventBusSubscriber
object CommonEventListener {

    @JvmStatic
    @SubscribeEvent
    fun onConfigChange(event: ConfigChangedEvent.OnConfigChangedEvent) {
        if (event.modID == MOD_ID) {
            ConfigManager.sync(MOD_ID, Config.Type.INSTANCE)
        }
    }

    @JvmStatic
    @SubscribeEvent
    fun onPlayerJoined(event: EntityJoinWorldEvent) {
        if (event.world.isRemote || event.entity !is EntityPlayer) return
        val player = event.entity as EntityPlayerMP
        LOG.info("Syncing config from server to " + player.displayName.formattedText)
        PacketHandler.INSTANCE.sendTo(ConfigValues.generateSyncPacket(), player)
    }


}