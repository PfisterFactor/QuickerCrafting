package pfister.quickercrafting.common.gui

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.world.World
import net.minecraftforge.fml.common.network.IGuiHandler

object GuiHandler: IGuiHandler {
    override fun getClientGuiElement(ID: Int, player: EntityPlayer?, world: World?, x: Int, y: Int, z: Int): Any? {
        return when (ID) {
            0 -> GuiQuickerCrafting(player!!.inventory)
            else -> null
        }
    }

    override fun getServerGuiElement(ID: Int, player: EntityPlayer?, world: World?, x: Int, y: Int, z: Int): Any? {
        return when (ID) {
            0 -> ContainerQuickerCrafting(true, player!!.inventory)
            else -> null
        }

    }
}