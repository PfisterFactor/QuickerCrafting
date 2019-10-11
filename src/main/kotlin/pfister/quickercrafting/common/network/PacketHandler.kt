package pfister.quickercrafting.common.network

import net.minecraftforge.fml.common.network.NetworkRegistry
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper
import net.minecraftforge.fml.relauncher.Side
import pfister.quickercrafting.MOD_ID

object PacketHandler {
    val INSTANCE: SimpleNetworkWrapper = NetworkRegistry.INSTANCE.newSimpleChannel(MOD_ID)

    fun registerMessages() {
        INSTANCE.registerMessage(MessageCraftItemHandler::class.java, MessageCraftItem::class.java, 0, Side.SERVER)
        INSTANCE.registerMessage(MessageOpenGUIHandler::class.java, MessageOpenGUI::class.java, 1, Side.SERVER)
    }
}