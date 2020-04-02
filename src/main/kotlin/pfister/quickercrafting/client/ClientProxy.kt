package pfister.quickercrafting.client

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.gui.inventory.GuiContainerCreative
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.InventoryEffectRenderer
import net.minecraft.client.settings.KeyBinding
import net.minecraft.creativetab.CreativeTabs
import net.minecraftforge.client.event.GuiContainerEvent
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.client.settings.KeyConflictContext
import net.minecraftforge.fml.client.registry.ClientRegistry
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.relauncher.Side
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import pfister.quickercrafting.LOG
import pfister.quickercrafting.MOD_ID
import pfister.quickercrafting.QuickerCrafting
import pfister.quickercrafting.client.gui.ClientContainerQuickerCrafting
import pfister.quickercrafting.client.gui.GuiButtonImageBiggerTexture
import pfister.quickercrafting.client.gui.GuiQuickerCrafting
import pfister.quickercrafting.common.CommonProxy
import pfister.quickercrafting.common.ConfigValues
import pfister.quickercrafting.common.crafting.InventoryChangeManager
import pfister.quickercrafting.common.network.MessageOpenGUI
import pfister.quickercrafting.common.network.PacketHandler
import pfister.quickercrafting.common.util.canQuickCraft3x3
import kotlin.system.measureTimeMillis

class ClientProxy : CommonProxy() {

    override fun init(event: FMLInitializationEvent) {
        super.init(event)
        ClientRegistry.registerKeyBinding(ClientEventListener.InvKeyBinding)

    }

    override fun loadComplete(event: FMLLoadCompleteEvent) {
        val ms2 = measureTimeMillis { RecipeWorker.buildRecipeGraph() }
        LOG.info("Building recipe graph took ${ms2}ms.")
    }

}
@Mod.EventBusSubscriber(Side.CLIENT)
object ClientEventListener {
    // Referenced by the core mod, do not change
    @JvmStatic
    val InvKeyBinding: KeyBinding = KeyBinding("key.$MOD_ID.desc", KeyConflictContext.UNIVERSAL, Keyboard.KEY_E, "key.$MOD_ID.category")

    @JvmStatic
    @SubscribeEvent
    fun onClientTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.END) return
        if (!InvKeyBinding.isPressed) return

        val player = Minecraft.getMinecraft().player
        if (player.isCreative) {
            Minecraft.getMinecraft().displayGuiScreen(GuiContainerCreative(player))
        } else {
            PacketHandler.INSTANCE.sendToServer(MessageOpenGUI())
            player.openGui(QuickerCrafting, 0, player.world,
                    player.posX.toInt(),
                    player.posY.toInt(),
                    player.posZ.toInt())
        }
    }


    private var tickCounter = 0
    @JvmStatic
    @SubscribeEvent
    // Updates the recipe cache when the player is not in an inventory so the user doesnt have to wait to populate it
    // Only happens once per 20 ticks (usually every second)
    fun onPlayerTick(event: TickEvent.PlayerTickEvent) {
        if (event.side != Side.CLIENT || event.phase != TickEvent.Phase.START) return
        RecipeWorker.startRecipeDaemon()
        tickCounter += 1
        if (tickCounter < ConfigValues.RecipeCheckFrequency) return
        tickCounter = 0
        val player = Minecraft.getMinecraft().player
        // Let the container handle the recipe cache updating if its open, otherwise the recipecache falls out of sync
        val screen = Minecraft.getMinecraft().currentScreen as? GuiQuickerCrafting
        if (screen == null) {
            InventoryChangeManager.computeChanges(false)
        } else {
            (screen.inventorySlots as ClientContainerQuickerCrafting).CanCraft3x3 = player.canQuickCraft3x3()
        }


    }

    private val quickerCraftingButton: GuiButtonImageBiggerTexture = GuiButtonImageBiggerTexture(100, 0, 0, 17, 15, 473, 204, 16, GuiQuickerCrafting.TEXTURE, 512f, 256f)
    @JvmStatic
    @SubscribeEvent
    // Adds our button to go to the quicker crafting menu in the inventory
    fun onRenderTick(event: GuiContainerEvent.DrawForeground) {
        if (Minecraft.getMinecraft().currentScreen == null || !ConfigValues.ShouldDisplayQuickerCraftingButton) return
        val clazz = Minecraft.getMinecraft().currentScreen!!::class.simpleName!!
        if (clazz != "GuiInventory" && clazz != "GuiPlayerExpanded" && clazz != "GuiContainerCreative") return
        val inv = (Minecraft.getMinecraft().currentScreen as InventoryEffectRenderer)
        quickerCraftingButton.visible = true
        quickerCraftingButton.enabled = true

        if (clazz == "GuiContainerCreative") {
            val creativeInv = inv as GuiContainerCreative
            if (creativeInv.selectedTabIndex == CreativeTabs.INVENTORY.index) {
                val destroyItemSlot = creativeInv.inventorySlots.inventorySlots.find { it.slotIndex == 0 && it !is GuiContainerCreative.CreativeSlot }
                if (destroyItemSlot != null) {
                    quickerCraftingButton.x = destroyItemSlot.xPos
                    quickerCraftingButton.y = destroyItemSlot.yPos - 22
                }
            } else {
                quickerCraftingButton.x = 0
                quickerCraftingButton.y = 0
                quickerCraftingButton.enabled = false
                quickerCraftingButton.visible = false
            }
        } else {
            val craftResult = inv.inventorySlots.inventorySlots[0]
            quickerCraftingButton.x = craftResult.xPos
            quickerCraftingButton.y = inv.inventorySlots.inventorySlots[0].yPos - 24
            if (QuickerCrafting.InvTweaksLoaded) {
                // Moves the invtweaks button up a bit so our buttons don't overlap
                val invTweaksButton = inv.buttonList.find { it.displayString == "..." }
                invTweaksButton?.y = inv.guiTop - 11
            }
        }

        GlStateManager.pushMatrix()
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F)
        GlStateManager.disableLighting()
        quickerCraftingButton.drawButton(Minecraft.getMinecraft(), event.mouseX - inv.guiLeft, event.mouseY - inv.guiTop, 1f)
        GlStateManager.popMatrix()


    }


    // Why the heck does the event not pass the mouse position?
    @JvmStatic
    @SubscribeEvent
    fun onGuiInput(event: GuiScreenEvent.MouseInputEvent.Pre) {
        if (Minecraft.getMinecraft().currentScreen == null || !ConfigValues.ShouldDisplayQuickerCraftingButton || !Mouse.isButtonDown(0)) return
        val clazz = Minecraft.getMinecraft().currentScreen!!::class.simpleName!!
        if (clazz != "GuiInventory" && clazz != "GuiPlayerExpanded" && clazz != "GuiContainerCreative") return

        val inv = (Minecraft.getMinecraft().currentScreen as InventoryEffectRenderer)
        // Dumb workaround because forge doesn't give us a mouse position
        val scaledresolution = ScaledResolution(Minecraft.getMinecraft())
        val mouseX: Int = Mouse.getX() * scaledresolution.scaledWidth / Minecraft.getMinecraft().displayWidth
        val mouseY: Int = scaledresolution.scaledHeight - Mouse.getY() * scaledresolution.scaledHeight / Minecraft.getMinecraft().displayHeight - 1
        //
        if (quickerCraftingButton.mousePressed(Minecraft.getMinecraft(), mouseX - inv.guiLeft, mouseY - inv.guiTop)) {
            val player = Minecraft.getMinecraft().player
            quickerCraftingButton.playPressSound(Minecraft.getMinecraft().soundHandler)
            PacketHandler.INSTANCE.sendToServer(MessageOpenGUI())
            player.openGui(QuickerCrafting, 0, player.world,
                    player.posX.toInt(),
                    player.posY.toInt(),
                    player.posZ.toInt())
        }
    }
}