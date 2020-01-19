package pfister.quickercrafting.common.network

import io.netty.buffer.ByteBuf
import net.minecraft.network.PacketBuffer
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext
import pfister.quickercrafting.LOG
import pfister.quickercrafting.common.ConfigValues

// Syncs config from the server to the client
class MessageSyncConfig(var CraftingTableRadius: Int, var CraftingDepth: Int, var ValidCraftingTableBlocks: Array<String>, var ValidCraftingTableItems: Array<String>) : IMessage {
    constructor() : this(-1, 1, arrayOf(), arrayOf())

    override fun toBytes(buf: ByteBuf?) {
        val packetBuffer = PacketBuffer(buf!!)

        packetBuffer.writeInt(CraftingTableRadius)
        packetBuffer.writeInt(CraftingDepth)
        packetBuffer.writeInt(ValidCraftingTableBlocks.size)
        ValidCraftingTableBlocks.forEach { packetBuffer.writeString(it) }
        packetBuffer.writeInt(ValidCraftingTableItems.size)
        ValidCraftingTableItems.forEach { packetBuffer.writeString(it) }
    }

    override fun fromBytes(buf: ByteBuf?) {
        val packetBuffer = PacketBuffer(buf!!)
        CraftingTableRadius = packetBuffer.readInt()
        CraftingDepth = packetBuffer.readInt()
        val craftingTableBlocksSize = packetBuffer.readInt()
        ValidCraftingTableBlocks = (0 until craftingTableBlocksSize).map { packetBuffer.readString(100) }.toTypedArray()
        val craftingTableItemsSize = packetBuffer.readInt()
        ValidCraftingTableItems = (0 until craftingTableItemsSize).map { packetBuffer.readString(100) }.toTypedArray()
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
        ConfigValues.ValidCraftingTableBlocks = message.ValidCraftingTableBlocks
        ConfigValues.ValidCraftingTableItems = message.ValidCraftingTableItems
        LOG.info("Successfully received config from server: { CraftingTableRadius: ${message.CraftingTableRadius}, CraftingDepth: ${message.CraftingDepth}, ValidCraftingBlocks: ${message.ValidCraftingTableBlocks.contentToString()}, ValidCraftingItems: ${message.ValidCraftingTableItems.contentToString()} }")
        return null
    }

}