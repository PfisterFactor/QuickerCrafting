package pfister.quickercrafting.common.item

import net.minecraft.client.renderer.block.model.ModelResourceLocation
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Items
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.ActionResult
import net.minecraft.util.EnumActionResult
import net.minecraft.util.EnumHand
import net.minecraft.world.World
import net.minecraftforge.client.model.ModelLoader
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import pfister.quickercrafting.MOD_ID
import pfister.quickercrafting.QuickerCrafting

class ItemGuiTester : Item() {
    init {
        setRegistryName("gui_tester")
        unlocalizedName = "$MOD_ID.gui_tester"
        creativeTab = CreativeTabs.MISC
    }

    @SideOnly(Side.CLIENT)
    fun initModel() {
        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        ModelLoader.setCustomModelResourceLocation(this, 0, ModelResourceLocation(Items.STICK.registryName, "inventory"))
    }

    override fun onItemRightClick(worldIn: World, playerIn: EntityPlayer, handIn: EnumHand): ActionResult<ItemStack> {
        playerIn.openGui(QuickerCrafting, 0, worldIn,
                playerIn.posX.toInt(),
                playerIn.posY.toInt(),
                playerIn.posZ.toInt())
        return ActionResult(EnumActionResult.SUCCESS, playerIn.getHeldItem(handIn))
    }
}