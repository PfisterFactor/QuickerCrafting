package pfister.quickercrafting.client.gui

import com.google.common.collect.Ordering
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.GuiTextField
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.InventoryEffectRenderer
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.client.resources.I18n
import net.minecraft.client.util.RecipeItemHelper
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.inventory.ClickType
import net.minecraft.inventory.Slot
import net.minecraft.item.crafting.IRecipe
import net.minecraft.potion.Potion
import net.minecraft.potion.PotionEffect
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
import yalter.mousetweaks.api.MouseTweaksDisableWheelTweak
import java.awt.Color


@Mod.EventBusSubscriber(Side.CLIENT)
// Tell mousetweaks to go away or it screws up scrolling through the list
@MouseTweaksDisableWheelTweak
class GuiQuickerCrafting(playerInv: InventoryPlayer) : InventoryEffectRenderer(ClientContainerQuickerCrafting(playerInv)) {
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
            val itemMap = gui.hoveredCraftingInfo!!.totalItemMap
            val inv = Minecraft.getMinecraft().player.openContainer.inventoryItemStacks
            val packedItemsAndCounts = mutableMapOf<Int, Int>()
            itemMap.forEach { (k, v) ->
                packedItemsAndCounts.merge(RecipeItemHelper.pack(inv[k]), v) { v1, v2 -> v1 + v2 }
            }

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
            val xOffset = (if (packedItemsAndCounts.size > 3) 3 else packedItemsAndCounts.size) * 18
            gui.fontRenderer.drawString("x" + gui.hoveredCraftingInfo!!.CraftingInfos.size, tooltipX.toFloat() + xOffset, tooltipY.toFloat(), Color.MAGENTA.rgb, true)
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
    private lateinit var ChangeMenuButton: GuiButtonImageBiggerTexture
    // If the mouse was down the last frame
    private var wasClicking = false

    var hoveredCraftingInfo: RecipeCalculator.CraftingPath? = null

    init {
        // Set size of window
        this.xSize = 283
        this.ySize = 170
    }

    override fun initGui() {
        super.initGui()
        Scrollbar = GuiScrollBar(guiLeft, guiTop)
        Searchfield = GuiTextField(0, fontRenderer, guiLeft + 144, guiTop + 7, 87, 9)
        ChangeMenuButton = GuiButtonImageBiggerTexture(0, guiLeft + 262, guiTop + 3, 17, 15, 492, 204, 16, GuiQuickerCrafting.TEXTURE, 512f, 256f)
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
            if (CraftHandler.tryCraftRecipe(this.inventorySlots as ContainerQuickerCrafting, hoveredCraftingInfo?.CraftingInfos?.first()?.Recipe!!, type == ClickType.QUICK_MOVE))
                PacketHandler.INSTANCE.sendToServer(MessageCraftItem(hoveredCraftingInfo?.CraftingInfos?.first()?.Recipe!!, type == ClickType.QUICK_MOVE))
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
        if (ChangeMenuButton.mousePressed(Minecraft.getMinecraft(), mouseX, mouseY)) {
            ChangeMenuButton.playPressSound(Minecraft.getMinecraft().soundHandler)
            Minecraft.getMinecraft().displayGuiScreen(GuiInventory(Minecraft.getMinecraft().player))
        }
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
            hoveredCraftingInfo!!.totalItemMap.forEach { (key, _) ->
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
        if (!wasClicking && isClicking && Scrollbar.isInScrollBarBounds(mouseX, mouseY)) {
            Scrollbar.isScrolling = true
        }
        if (!isClicking)
            Scrollbar.isScrolling = false

        wasClicking = isClicking

        if (Scrollbar.isScrolling) {
            Scrollbar.currentScroll = (mouseY - (guiTop + GuiScrollBar.GUI_POS_Y)) / GuiScrollBar.SCROLLBAR_HEIGHT.toDouble()
            Scrollbar.currentScroll = MathHelper.clamp(Scrollbar.currentScroll, 0.0, 1.0)
        }
        super.drawScreen(mouseX, mouseY, partialTicks)
        GlStateManager.disableLighting()
        ChangeMenuButton.drawButton(Minecraft.getMinecraft(), mouseX, mouseY, partialTicks)
        GlStateManager.enableLighting()
        hoveredCraftingInfo = if (slotUnderMouse != null && slotUnderMouse is ClientSlot) {
            val recipe: IRecipe? = (slotUnderMouse as ClientSlot).Recipes?.get((slotUnderMouse as ClientSlot).RecipeIndex)
            if (recipe != null) {
                if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
                    RecipeCalculator.CraftingPath.shiftCraftRecipe((inventorySlots as ContainerQuickerCrafting).getCraftInventory(), recipe)
                } else {
                    RecipeCalculator.doCraft((inventorySlots as ContainerQuickerCrafting).getCraftInventory(), recipe).toCraftPath()
                }

            } else null
        } else
            null

        renderHoveredToolTip(mouseX, mouseY)
    }

    override fun renderHoveredToolTip(mouseX: Int, mouseY: Int) {
        super.renderHoveredToolTip(mouseX, mouseY)
        if (hoveredCraftingInfo?.finalRecipeOutput() != null) {
            renderToolTip(hoveredCraftingInfo?.finalRecipeOutput()!!, mouseX, mouseY)
        }
    }

    override fun drawGuiContainerForegroundLayer(mouseX: Int, mouseY: Int) {
        this.fontRenderer.drawString(I18n.format("container.crafting"),
                99,
                6,
                4210752)

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F)
        this.mc.textureManager.bindTexture(TEXTURE)
        if (Scrollbar.isEnabled || (inventorySlots as ClientContainerQuickerCrafting).isPopulating) {
            Gui.drawModalRectWithCustomSizedTexture(
                    GuiScrollBar.GUI_POS_X,
                    MathHelper.clamp(GuiScrollBar.GUI_POS_Y - GuiScrollBar.TEX_HEIGHT / 2 + (GuiScrollBar.SCROLLBAR_HEIGHT * Scrollbar.currentScroll).toInt(), GuiScrollBar.GUI_POS_Y, GuiScrollBar.GUI_POS_Y + GuiScrollBar.SCROLLBAR_HEIGHT - GuiScrollBar.TEX_HEIGHT - 1
                    ),
                    GuiScrollBar.TEX_OFFSET_X.toFloat(),
                    GuiScrollBar.TEX_OFFSET_Y.toFloat(),
                    GuiScrollBar.TEX_WIDTH,
                    GuiScrollBar.TEX_HEIGHT, 512f, 256f
            )
        } else {

            Gui.drawModalRectWithCustomSizedTexture(
                    GuiScrollBar.GUI_POS_X,
                    GuiScrollBar.GUI_POS_Y,
                    GuiScrollBar.TEX_OFFSET_X.toFloat() + GuiScrollBar.TEX_WIDTH,
                    GuiScrollBar.TEX_OFFSET_Y.toFloat(),
                    GuiScrollBar.TEX_WIDTH,
                    GuiScrollBar.TEX_HEIGHT, 512f, 256f
            )
            if (!(inventorySlots as ClientContainerQuickerCrafting).isPopulating) {
                Scrollbar.currentScroll = 0.0
            }
        }
        GlStateManager.disableLighting()
        GlStateManager.disableDepth()
        inventorySlots.inventorySlots
                .filter { s -> s is ClientSlot && (s.State == SlotState.DISABLED || s.State == SlotState.EMPTY) }
                .forEach { slot -> drawGradientRect(slot.xPos, slot.yPos, slot.xPos + 16, slot.yPos + 16, 0x55000000, 0x55000000) }
        GlStateManager.enableLighting()
        GlStateManager.enableDepth()
    }

    // Most of this is ripped out of InventoryEffectRenderer
    // We just change it so the potion effects scale at differing screen widths
    override fun drawActivePotionEffects() {
        val i = if (this.guiLeft - 124 < 0) 0 else this.guiLeft - 124
        var j = this.guiTop
        val collection = this.mc.player.activePotionEffects

        if (!collection.isEmpty()) {
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f)
            GlStateManager.disableLighting()
            var l = 33

            if (collection.size > 5) {
                l = 132 / (collection.size - 1)
            }

            for (potioneffect in Ordering.natural<PotionEffect>().sortedCopy(collection)) {
                val potion = potioneffect.potion
                if (!potion.shouldRender(potioneffect)) continue
                var s1 = I18n.format(potion.name)

                if (potioneffect.amplifier == 1) {
                    s1 = s1 + " " + I18n.format("enchantment.level.2")
                } else if (potioneffect.amplifier == 2) {
                    s1 = s1 + " " + I18n.format("enchantment.level.3")
                } else if (potioneffect.amplifier == 3) {
                    s1 = s1 + " " + I18n.format("enchantment.level.4")
                }

                val guiOverSizeFlag = i + 28 + fontRenderer.getStringWidth(s1) + 3 >= this.guiLeft
                GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f)
                this.mc.textureManager.bindTexture(GuiContainer.INVENTORY_BACKGROUND)

                if (guiOverSizeFlag) {
                    this.drawTexturedModalRect(this.guiLeft - 24, j, 141, 166, 24, 24)
                    if (potion.hasStatusIcon()) {
                        val i1 = potion.statusIconIndex
                        this.drawTexturedModalRect(this.guiLeft - 21, j + 3, 0 + i1 % 8 * 18, 198 + i1 / 8 * 18, 18, 18)
                    }
                } else {
                    this.drawTexturedModalRect(i, j, 0, 166, this.guiLeft - i - 4, 32)
                    this.drawTexturedModalRect(this.guiLeft - 4, j, 116, 166, 4, 32)
                    if (potion.hasStatusIcon()) {
                        val i1 = potion.statusIconIndex
                        this.drawTexturedModalRect(i + 6, j + 7, 0 + i1 % 8 * 18, 198 + i1 / 8 * 18, 18, 18)
                    }
                }
                potion.renderInventoryEffect(i, j, potioneffect, mc)
                if (!potion.shouldRenderInvText(potioneffect) || guiOverSizeFlag) {
                    j += l
                    continue
                }


                this.fontRenderer.drawStringWithShadow(s1, (i + 10 + 18).toFloat(), (j + 6).toFloat(), 16777215)
                val s = Potion.getPotionDurationString(potioneffect, 1.0f)
                this.fontRenderer.drawStringWithShadow(s, (i + 10 + 18).toFloat(), (j + 6 + 10).toFloat(), 8355711)
                j += l
            }
        }
    }

    override fun hasClickedOutside(mouseX: Int, mouseY: Int, left: Int, top: Int): Boolean {
        return if (mouseY > top + 78) {
            if (mouseY > top + 147) mouseX < left || mouseX > left + 175 || mouseY < top || mouseY > top + ySize
            else mouseX < left || mouseX > left + 206 || mouseY < top || mouseY > top + ySize
        } else super.hasClickedOutside(mouseX, mouseY, left, top)
    }

}
