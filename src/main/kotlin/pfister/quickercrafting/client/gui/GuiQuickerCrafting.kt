package pfister.quickercrafting.client.gui

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.GuiTextField
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.client.resources.I18n
import net.minecraft.client.util.RecipeItemHelper
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.inventory.ClickType
import net.minecraft.inventory.Slot
import net.minecraft.item.crafting.IRecipe
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.MathHelper
import net.minecraftforge.client.event.RenderTooltipEvent
import net.minecraftforge.fml.client.config.GuiUtils
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.relauncher.Side
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import pfister.quickercrafting.MOD_ID
import pfister.quickercrafting.client.ClientEventListener
import pfister.quickercrafting.common.crafting.CraftHandler
import pfister.quickercrafting.common.crafting.RecipeCache
import pfister.quickercrafting.common.crafting.RecipeCalculator
import pfister.quickercrafting.common.gui.ContainerQuickerCrafting
import pfister.quickercrafting.common.network.MessageCraftItem
import pfister.quickercrafting.common.network.PacketHandler
import yalter.mousetweaks.api.MouseTweaksIgnore


@Mod.EventBusSubscriber(Side.CLIENT)
// Tell mousetweaks to go away or it screws up scrolling through the list
// Todo: Might be able to change this to @MouseTweaksDisableWheelTweak
@MouseTweaksIgnore
class GuiQuickerCrafting(playerInv: InventoryPlayer) : GuiContainer(ClientContainerQuickerCrafting(playerInv)) {
    companion object {
        val TEXTURE: ResourceLocation = ResourceLocation(MOD_ID, "textures/gui/quickercrafting_inv.png")

        @JvmStatic
        @SubscribeEvent
        // Cancels the tooltip rendering
        fun onToolTipRenderPre(event: RenderTooltipEvent.Pre) {
            if (Minecraft.getMinecraft().currentScreen !is GuiQuickerCrafting) return

            val gui = Minecraft.getMinecraft().currentScreen as GuiQuickerCrafting
            if (gui.hoveredCraftingInfo?.canCraft() == false) {
                event.isCanceled = true
                return
            }
        }
        @JvmStatic
        @SubscribeEvent
        fun onToolTipRender(event: RenderTooltipEvent.PostText) {
            if (Minecraft.getMinecraft().currentScreen !is GuiQuickerCrafting) return

            val gui = Minecraft.getMinecraft().currentScreen as GuiQuickerCrafting

            if (gui.hoveredCraftingInfo == null || gui.hoveredCraftingInfo?.canCraft() == false) return
            val itemMap = gui.hoveredCraftingInfo!!.ItemMap
            val inv = Minecraft.getMinecraft().player.openContainer.inventoryItemStacks
            val packedItemsAndCounts = itemMap.map { RecipeItemHelper.pack(inv[it.key]) to it.value }.toMap()

            val tooltipX = event.x + event.width + 7
            val tooltipY = event.y

            val tooltipTextWidth =
                    if (packedItemsAndCounts.size >= 3) 54 else packedItemsAndCounts.size * 18
            val tooltipHeight = (1 + (packedItemsAndCounts.size - 1) / 3) * 18
            val backgroundColor = 0xF0100010.toInt()
            val borderColorStart = 0x505000FF.toInt()

            val borderColorEnd = (borderColorStart and 0xFEFEFE) shr 1 or borderColorStart and 0xFF000000.toInt()

            GuiUtils.drawGradientRect(300,
                    tooltipX - 3,
                    tooltipY - 4,
                    tooltipX + tooltipTextWidth + 3,
                    tooltipY - 3,
                    backgroundColor,
                    backgroundColor)
            GuiUtils.drawGradientRect(300,
                    tooltipX - 3,
                    tooltipY + tooltipHeight + 3,
                    tooltipX + tooltipTextWidth + 3,
                    tooltipY + tooltipHeight + 4,
                    backgroundColor,
                    backgroundColor)
            GuiUtils.drawGradientRect(300,
                    tooltipX - 3,
                    tooltipY - 3,
                    tooltipX + tooltipTextWidth + 3,
                    tooltipY + tooltipHeight + 3,
                    backgroundColor,
                    backgroundColor)
            GuiUtils.drawGradientRect(300,
                    tooltipX - 4,
                    tooltipY - 3,
                    tooltipX - 3,
                    tooltipY + tooltipHeight + 3,
                    backgroundColor,
                    backgroundColor)
            GuiUtils.drawGradientRect(300,
                    tooltipX + tooltipTextWidth + 3,
                    tooltipY - 3,
                    tooltipX + tooltipTextWidth + 4,
                    tooltipY + tooltipHeight + 3,
                    backgroundColor,
                    backgroundColor)
            GuiUtils.drawGradientRect(300,
                    tooltipX - 3,
                    tooltipY - 3 + 1,
                    tooltipX - 3 + 1,
                    tooltipY + tooltipHeight + 3 - 1,
                    borderColorStart,
                    borderColorEnd)
            GuiUtils.drawGradientRect(300,
                    tooltipX + tooltipTextWidth + 2,
                    tooltipY - 3 + 1,
                    tooltipX + tooltipTextWidth + 3,
                    tooltipY + tooltipHeight + 3 - 1,
                    borderColorStart,
                    borderColorEnd)
            GuiUtils.drawGradientRect(300,
                    tooltipX - 3,
                    tooltipY - 3,
                    tooltipX + tooltipTextWidth + 3,
                    tooltipY - 3 + 1,
                    borderColorStart,
                    borderColorStart)
            GuiUtils.drawGradientRect(300,
                    tooltipX - 3,
                    tooltipY + tooltipHeight + 2,
                    tooltipX + tooltipTextWidth + 3,
                    tooltipY + tooltipHeight + 3,
                    borderColorEnd,
                    borderColorEnd)

            var x = 0
            var y = 0
            packedItemsAndCounts.forEach { pair ->
                GlStateManager.pushMatrix()
                RenderHelper.disableStandardItemLighting()
                RenderHelper.enableGUIStandardItemLighting()
                GlStateManager.translate(0.0, 0.0, 301.0)
                val stack = RecipeItemHelper.unpack(pair.key)
                gui.drawItemStack(stack, tooltipX + x * 18, tooltipY + y * 18, "" + pair.value)
                GlStateManager.popMatrix()
                y = if (x == 2) y + 1 else y
                x = (x + 1) % 3
            }
        }
    }

    private lateinit var Searchfield: GuiTextField
    private lateinit var Scrollbar: GuiScrollBar


    // If the mouse was down the last frame
    private var wasClicking = false

    var hoveredCraftingInfo: RecipeCalculator.CraftingInfo? = null
    init {
        // Set size of window
        this.xSize = 283
        this.ySize = 170
    }

    override fun initGui() {
        super.initGui()
        Scrollbar = GuiScrollBar(guiLeft, guiTop)
        Searchfield = GuiTextField(0, fontRenderer, guiLeft + 144, guiTop + 7, 87, 9)
        Searchfield.maxStringLength = 50
        Searchfield.enableBackgroundDrawing = false
        Searchfield.setTextColor(16777215)
        Searchfield.isFocused = false

    }

    override fun updateScreen() {
        super.updateScreen()
        (this.inventorySlots as ClientContainerQuickerCrafting).currentSearchQuery = Searchfield.text
        Searchfield.updateCursorCounter()
        inventorySlots.detectAndSendChanges()
    }

    override fun handleMouseClick(slotIn: Slot?, slotId: Int, mouseButton: Int, type: ClickType) {
        if (slotId < (inventorySlots as ClientContainerQuickerCrafting).ClientSlotsStart)
            super.handleMouseClick(slotIn, slotId, mouseButton, type)
        else if (hoveredCraftingInfo != null && mouseButton == 0 && type != ClickType.THROW && Minecraft.getMinecraft().player.inventory.itemStack.isEmpty) {
            if (CraftHandler.tryCraftRecipe(this.inventorySlots as ContainerQuickerCrafting, hoveredCraftingInfo!!.Recipe, type == ClickType.QUICK_MOVE))
                PacketHandler.INSTANCE.sendToServer(MessageCraftItem(hoveredCraftingInfo!!.Recipe, type == ClickType.QUICK_MOVE))
        }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (!checkHotbarKeys(keyCode)) {
            if (Searchfield.textboxKeyTyped(typedChar, keyCode)) {
                val slots = (inventorySlots as ClientContainerQuickerCrafting)
                slots.handleSearch(Searchfield.text)
                slots.updateDisplay(Scrollbar.currentScroll, null, true)
            } else {
                when (keyCode) {
                    Keyboard.KEY_TAB -> Searchfield.isFocused = !Searchfield.isFocused
                    ClientEventListener.InvKeyBinding.keyCode -> Minecraft.getMinecraft().player.closeScreen()
                    else -> super.keyTyped(typedChar, keyCode)
                }
            }
        }
    }

    override fun handleMouseInput() {
        super.handleMouseInput()
        var i = Mouse.getDWheel()
        if (i != 0 && Scrollbar.isEnabled) {
            val rows = (RecipeCache.CraftableRecipes.size + 8) / 9
            if (i > 0) i = 1
            else if (i < 0) i = -1
            Scrollbar.isScrolling = true
            Scrollbar.currentScroll = Scrollbar.currentScroll - i / rows.toFloat()
            Scrollbar.currentScroll = MathHelper.clamp(Scrollbar.currentScroll, 0.0, 1.0)
        }
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        super.mouseClicked(mouseX, mouseY, mouseButton)
        val slot = slotUnderMouse
        if (slot != null && slot is ClientSlot && mouseButton == 1) {
            slot.RecipeIndex = (slot.RecipeIndex + 1) % (slot.Recipes?.size ?: 1)
        }
        Searchfield.mouseClicked(mouseX, mouseY, mouseButton)
    }

    // Draws the background to the GUI
    override fun drawGuiContainerBackgroundLayer(partialTicks: Float, mouseX: Int, mouseY: Int) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F)
        // Bind the GUI texture
        this.mc.textureManager.bindTexture(TEXTURE)
        Gui.drawModalRectWithCustomSizedTexture(this.guiLeft, this.guiTop, 0f, 0f, this.xSize, this.ySize, 512f, 256f)

        GuiInventory.drawEntityOnScreen(this.guiLeft + 51, this.guiTop + 75, 30, this.guiLeft.toFloat() + 51 - mouseX, this.guiTop.toFloat() + 25 - mouseY, Minecraft.getMinecraft().player)
        Searchfield.drawTextBox()
        if (hoveredCraftingInfo?.canCraft() == true) {
            GlStateManager.pushMatrix()
            // Draws green semi-transparent rectangles behind items that will be used to craft the recipe
            hoveredCraftingInfo!!.ItemMap.forEach { (key, _) ->
                val row = key / 9 // 1-4 is the player's inventory, 5 is the craft buffer on the right
                val column = key % 9
                var xToDrawAt = guiLeft + 8 + column * 18
                var yToDrawAt = if (row < 4) guiTop + 70 + row * 18 else guiTop + 146
                if (row >= 5) {
                    xToDrawAt = guiLeft + 184
                    yToDrawAt = guiTop + 70 + column * 18
                }
                GuiUtils.drawGradientRect(0,
                        xToDrawAt,
                        yToDrawAt,
                        xToDrawAt + 16,
                        yToDrawAt + 16, 0x9900CC00.toInt(), 0xFF00CC00.toInt())

            }
            GlStateManager.popMatrix()
        }


    }

    // Draws the buttons and stuff on top of the background
    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        val hoveredSlot = slotUnderMouse as? ClientSlot
        (inventorySlots as ClientContainerQuickerCrafting).updateDisplay(Scrollbar.currentScroll, hoveredSlot, Scrollbar.isScrolling)
        Scrollbar.isEnabled = (inventorySlots as ClientContainerQuickerCrafting).shouldDisplayScrollbar
        drawDefaultBackground()
        val isClicking: Boolean = Mouse.isButtonDown(0)
        if (!wasClicking && isClicking && Scrollbar.isInScrollBarBounds(mouseX, mouseY))
            Scrollbar.isScrolling = true

        if (!isClicking)
            Scrollbar.isScrolling = false

        wasClicking = isClicking

        if (Scrollbar.isScrolling) {
            Scrollbar.currentScroll = (mouseY - (guiTop + GuiScrollBar.GUI_POS_Y)) / GuiScrollBar.SCROLLBAR_HEIGHT.toDouble()
            Scrollbar.currentScroll = MathHelper.clamp(Scrollbar.currentScroll, 0.0, 1.0)
        }
        super.drawScreen(mouseX, mouseY, partialTicks)

        if (slotUnderMouse != null && slotUnderMouse
                        is ClientSlot) {
            val recipe: IRecipe? = (slotUnderMouse as ClientSlot).Recipes?.get((slotUnderMouse as ClientSlot).RecipeIndex)
            hoveredCraftingInfo = if (recipe != null) RecipeCalculator.doCraft(inventorySlots.inventory, recipe) else null
        } else
            hoveredCraftingInfo = null

        renderHoveredToolTip(mouseX, mouseY)
    }

    override fun renderHoveredToolTip(mouseX: Int, mouseY: Int) {
        super.renderHoveredToolTip(mouseX, mouseY)
        if (hoveredCraftingInfo != null)
            renderToolTip(hoveredCraftingInfo!!.Recipe.recipeOutput, mouseX, mouseY)
    }

    override fun drawGuiContainerForegroundLayer(mouseX: Int, mouseY: Int) {
        this.fontRenderer.drawString(I18n.format("container.crafting"),
                99,
                6,
                4210752)

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F)
        this.mc.textureManager.bindTexture(TEXTURE)
        if (Scrollbar.isEnabled || (inventorySlots as ClientContainerQuickerCrafting).isPopulating)

            Gui.drawModalRectWithCustomSizedTexture(
                    GuiScrollBar.GUI_POS_X,
                    MathHelper.clamp(GuiScrollBar.GUI_POS_Y - GuiScrollBar.TEX_HEIGHT / 2 + (GuiScrollBar.SCROLLBAR_HEIGHT * Scrollbar.currentScroll).toInt(), GuiScrollBar.GUI_POS_Y, GuiScrollBar.GUI_POS_Y + GuiScrollBar.SCROLLBAR_HEIGHT - GuiScrollBar.TEX_HEIGHT - 1
                    ),
                    GuiScrollBar.TEX_OFFSET_X.toFloat(),
                    GuiScrollBar.TEX_OFFSET_Y.toFloat(),
                    GuiScrollBar.TEX_WIDTH,
                    GuiScrollBar.TEX_HEIGHT, 512f, 256f
            )
        else {
            Gui.drawModalRectWithCustomSizedTexture(
                    GuiScrollBar.GUI_POS_X,
                    GuiScrollBar.GUI_POS_Y,
                    GuiScrollBar.TEX_OFFSET_X.toFloat() + GuiScrollBar.TEX_WIDTH,
                    GuiScrollBar.TEX_OFFSET_Y.toFloat(),
                    GuiScrollBar.TEX_WIDTH,
                    GuiScrollBar.TEX_HEIGHT, 512f, 256f
            )
            if (!(inventorySlots as ClientContainerQuickerCrafting).isPopulating)
                Scrollbar.currentScroll = 0.0
        }
        GlStateManager.disableLighting()
        GlStateManager.disableDepth()
        inventorySlots.inventorySlots
                .filter { s -> s is ClientSlot && (s.State == SlotState.DISABLED || s.State == SlotState.EMPTY) }
                .forEach { slot -> drawGradientRect(slot.xPos, slot.yPos, slot.xPos + 16, slot.yPos + 16, 0x55000000, 0x55000000) }
        GlStateManager.enableLighting()
        GlStateManager.enableDepth()
    }

    override fun hasClickedOutside(mouseX: Int, mouseY: Int, left: Int, top: Int): Boolean {
        return if (mouseY > top + 78) {
            if (mouseY > top + 147)
                mouseX < left || mouseX > left + 175 || mouseY < top || mouseY > top + ySize
            else
                mouseX < left || mouseX > left + 206 || mouseY < top || mouseY > top + ySize
        } else
            super.hasClickedOutside(mouseX, mouseY, left, top)
    }

}
