package pfister.quickercrafting.client.gui

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.ResourceLocation

class GuiButtonImageBiggerTexture(buttonId: Int, xIn: Int, yIn: Int, widthIn: Int, heightIn: Int, val textureOffestX: Int, val textureOffestY: Int, val hoveredYOffset: Int, val resource: ResourceLocation, val texSizeX: Float, val texSizeY: Float) : GuiButton(buttonId, xIn, yIn, widthIn, heightIn, "") {
    override fun drawButton(mc: Minecraft, mouseX: Int, mouseY: Int, partialTicks: Float) {
        if (visible) {
            hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height
            mc.textureManager.bindTexture(resource)
            GlStateManager.disableDepth()
            val i = textureOffestX
            var j = textureOffestY
            if (hovered) {
                j += hoveredYOffset
            }
            Gui.drawModalRectWithCustomSizedTexture(x, y, i.toFloat(), j.toFloat(), width, height, texSizeX, texSizeY)
            GlStateManager.enableDepth()
        }
    }
}