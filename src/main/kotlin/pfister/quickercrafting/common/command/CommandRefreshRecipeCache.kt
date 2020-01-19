package pfister.quickercrafting.common.command

import net.minecraft.command.ICommand
import net.minecraft.command.ICommandSender
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.server.MinecraftServer
import net.minecraft.util.math.BlockPos
import net.minecraft.util.text.TextComponentString
import net.minecraft.util.text.TextFormatting
import pfister.quickercrafting.common.network.MessageRefreshCache
import pfister.quickercrafting.common.network.PacketHandler

class CommandRefreshRecipeCache : ICommand {
    override fun getName(): String = "refreshRecipeCache"
    override fun getTabCompletions(server: MinecraftServer, sender: ICommandSender, args: Array<String>, targetPos: BlockPos?): MutableList<String> {
        return mutableListOf()
    }

    override fun compareTo(other: ICommand?): Int {
        return if (other is CommandRefreshRecipeCache) 0 else -1
    }

    override fun checkPermission(server: MinecraftServer, sender: ICommandSender): Boolean {
        return true
    }

    override fun isUsernameIndex(args: Array<String>, index: Int): Boolean {
        return false
    }

    override fun getAliases(): MutableList<String> {
        return mutableListOf()
    }

    override fun execute(server: MinecraftServer, sender: ICommandSender, args: Array<String>) {
        if (sender.commandSenderEntity !is EntityPlayerMP) return
        val packet = MessageRefreshCache()
        PacketHandler.INSTANCE.sendTo(packet, sender.commandSenderEntity as EntityPlayerMP)
        val t = TextComponentString("Refreshing the QuickerCrafting RecipeCache...")
        t.style.color = TextFormatting.RED
        sender.sendMessage(t)

    }

    override fun getUsage(sender: ICommandSender): String = "/refreshRecipeCache"


}