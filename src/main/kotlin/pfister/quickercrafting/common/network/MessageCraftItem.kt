package pfister.quickercrafting.common.network

import io.netty.buffer.ByteBuf
import net.minecraft.item.crafting.IRecipe
import net.minecraft.network.PacketBuffer
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext
import net.minecraftforge.fml.common.registry.ForgeRegistries
import pfister.quickercrafting.LOG
import pfister.quickercrafting.common.gui.ContainerQuickerCrafting
import pfister.quickercrafting.common.util.CraftHandler

class MessageCraftItem(var Recipe: IRecipe?, var Shift: Boolean = false) : IMessage {
    @Suppress("unused")
    constructor() : this(null, false)
    var recipeString: String = if (Recipe != null) Recipe!!.registryName.toString() else ""

    override fun toBytes(buf: ByteBuf?) {
        PacketBuffer(buf!!).writeBoolean(Shift)
        PacketBuffer(buf).writeString(recipeString)
    }

    override fun fromBytes(buf: ByteBuf?) {
        Shift = PacketBuffer(buf!!).readBoolean()
        recipeString = PacketBuffer(buf).readString(50)
        Recipe = try {
            ForgeRegistries.RECIPES.getValue(ResourceLocation(recipeString))
        } catch (e: Exception) {
            null
        }
    }


}

class MessageCraftItemHandler : IMessageHandler<MessageCraftItem, IMessage> {
    override fun onMessage(message: MessageCraftItem?, ctx: MessageContext?): IMessage? {
        if (message?.Recipe == null) {
            LOG.warn("MessageCraftItemHandler: Recipe '${message?.recipeString}' cannot be found.")
            return null
        }

        val player = ctx?.serverHandler?.player
        if (player!!.openContainer !is ContainerQuickerCrafting) {
            LOG.warn("MessageCraftItemHandler: ContainerQuickerCrafting is not open on the server.")
            return null
        }

        val container = player.openContainer as ContainerQuickerCrafting
        CraftHandler.tryCraftRecipe(container, message.Recipe!!, message.Shift)
        return null
    }
}