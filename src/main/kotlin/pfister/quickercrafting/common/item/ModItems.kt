package pfister.quickercrafting.common.item

import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import pfister.quickercrafting.MOD_ID

object ModItems {
    val guiTester: ItemGuiTester by lazy {
        Item.REGISTRY.getObject(ResourceLocation(MOD_ID, "gui_tester")) as ItemGuiTester
    }

    @SideOnly(Side.CLIENT)
    fun initModels() {
        guiTester.initModel()
    }
}