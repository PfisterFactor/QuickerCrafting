package pfister.quickercrafting.client.gui

class GuiScrollBar(val guiLeft: Int, val guiTop: Int) {
    companion object {
        val TEX_OFFSET_X = 322
        val TEX_OFFSET_Y = 0
        val TEX_WIDTH = 12
        val TEX_HEIGHT = 15
        val GUI_POS_X = 264
        val GUI_POS_Y = 20
        val SCROLLBAR_HEIGHT = 53
    }
    var isScrolling: Boolean = false
    var currentScroll: Double = 0.0
    var isEnabled: Boolean = true

    fun isInScrollBarBounds(mouseX: Int, mouseY: Int): Boolean = mouseX >= guiLeft + GUI_POS_X && mouseX < guiLeft + GUI_POS_X + TEX_WIDTH && mouseY >= guiTop + GUI_POS_Y && mouseY < guiTop + GUI_POS_Y + SCROLLBAR_HEIGHT

}