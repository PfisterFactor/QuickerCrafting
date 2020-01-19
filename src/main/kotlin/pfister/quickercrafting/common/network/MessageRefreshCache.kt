package pfister.quickercrafting.common.network

import io.netty.buffer.ByteBuf
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext
import pfister.quickercrafting.LOG
import pfister.quickercrafting.common.crafting.RecipeCache

class MessageRefreshCache : IMessage {
    override fun fromBytes(buf: ByteBuf?) {

    }

    override fun toBytes(buf: ByteBuf?) {

    }
}

class MessageRefreshCacheHandler : IMessageHandler<MessageRefreshCache, IMessage> {
    override fun onMessage(message: MessageRefreshCache?, ctx: MessageContext?): IMessage? {
        Minecraft.getMinecraft().player.closeScreen()
        RecipeCache.updateCache(true)
        LOG.info("MessageRefreshCacheHandler: Refreshed client's recipe cache.")
        return null
    }

}