package pfister.quickercrafting.common.network

import io.netty.buffer.ByteBuf
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext
import pfister.quickercrafting.QuickerCrafting
import pfister.quickercrafting.common.gui.ContainerQuickerCrafting

class MessageOpenGUI : IMessage {
    override fun toBytes(buf: ByteBuf?) {
    }

    override fun fromBytes(buf: ByteBuf?) {
    }

}

class MessageOpenGUIHandler : IMessageHandler<MessageOpenGUI, IMessage> {
    override fun onMessage(message: MessageOpenGUI?, ctx: MessageContext?): IMessage? {
        val player = ctx!!.serverHandler.player
        if (player.openContainer !is ContainerQuickerCrafting)
            player.openGui(QuickerCrafting, 0, player.world, player.posX.toInt(), player.posY.toInt(), player.posZ.toInt())
        return null
    }

}