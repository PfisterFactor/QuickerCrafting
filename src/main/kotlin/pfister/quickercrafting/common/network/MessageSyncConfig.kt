package pfister.quickercrafting.common.network

import io.netty.buffer.ByteBuf
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext
import pfister.quickercrafting.LOG
import pfister.quickercrafting.common.ConfigValues

// Syncs config from the server to the client
class MessageSyncConfig(var CraftingTableRadius: Int, var CraftingDepth: Int) : IMessage {
    constructor() : this(-1, 1)

    override fun toBytes(buf: ByteBuf?) {
        buf?.writeInt(CraftingTableRadius)
        buf?.writeInt(CraftingDepth)
    }

    override fun fromBytes(buf: ByteBuf?) {
        CraftingTableRadius = buf!!.readInt()
        CraftingDepth = buf.readInt()
    }


}

class MessageSyncConfigHandler : IMessageHandler<MessageSyncConfig, IMessage> {
    override fun onMessage(message: MessageSyncConfig?, ctx: MessageContext?): IMessage? {
        if (message == null) {
            LOG.warn("MessageSyncConfigHandler: Config message was null! Configuration is not synced between client and server.")
            return null
        }
        ConfigValues.CraftingTableRadius = message.CraftingTableRadius
        ConfigValues.CraftingDepth = message.CraftingDepth
        LOG.info("Successfully received config from server: { CraftingTableRadius: ${message.CraftingTableRadius}, CraftingDepth: ${message.CraftingDepth}}")
        return null
    }

}