package pfister.quickercrafting.client

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.settings.KeyBinding
import net.minecraftforge.client.event.GuiContainerEvent
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.client.event.ModelRegistryEvent
import net.minecraftforge.client.settings.KeyModifier
import net.minecraftforge.fml.client.registry.ClientRegistry
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent
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
import pfister.quickercrafting.common.crafting.RecipeCache
import pfister.quickercrafting.common.item.ModItems
import pfister.quickercrafting.common.network.MessageOpenGUI
import pfister.quickercrafting.common.network.PacketHandler
import kotlin.system.measureTimeMillis

class ClientProxy : CommonProxy() {

    override fun init(event: FMLInitializationEvent) {
        super.init(event)
        ClientRegistry.registerKeyBinding(ClientEventListener.InvKeyBinding)

    }

    override fun postInit(event: FMLPostInitializationEvent) {
        super.postInit(event)
        // Evaluate the lazy variables, thus calculating the item set and recipe graph
        val ms1 = measureTimeMillis { RecipeCache.ItemsUsedInRecipes }
        LOG.info("Building ingredient set took ${ms1}ms.")
        val ms2 = measureTimeMillis { RecipeCache.RecipeGraph }
        LOG.info("Building recipe graph took ${ms2}ms.")
    }

    override fun loadComplete(event: FMLLoadCompleteEvent) {
        if (ConfigValues.HookCraftingKeybind) {
            hookInventoryKeybind()
        }

    }

    // Creates a keybinding hook and replaces the inventory keybinding with it
    // Makes it so pressing the key for QuickerCrafing will close other guis like how pressing E closes guis
    private fun hookInventoryKeybind() {
        val settings = Minecraft.getMinecraft().gameSettings
        val invKeyIndex = settings.keyBindings.indexOf(settings.keyBindInventory)
        if (invKeyIndex != -1) {
            val hook = KeybindingHook(settings.keyBindings[invKeyIndex], ClientEventListener.InvKeyBinding)
            settings.keyBindings[invKeyIndex] = hook
            settings.keyBindInventory = hook
        } else {
            LOG.warn("ClientProxy: There was an issue finding the inventory keybinding (${settings.keyBindInventory}) within the keybinding array, could not install hook!")
        }
        if (ClientEventListener.InvKeyBinding.isSetToDefaultValue && settings.keyBindInventory.isSetToDefaultValue) {
            settings.keyBindInventory.setKeyModifierAndCode(KeyModifier.NONE, Keyboard.KEY_NONE)
        }

    }


}
@Mod.EventBusSubscriber(Side.CLIENT)
object ClientEventListener {
    val InvKeyBinding: KeyBinding = KeyBinding("key.$MOD_ID.desc", Keyboard.KEY_E, "key.$MOD_ID.category")
    @JvmStatic
    @SubscribeEvent
    fun registerItemModels(event: ModelRegistryEvent) {
        ModItems.initModels()
    }

    @JvmStatic
    @SubscribeEvent
    fun onKeybinding(event: InputEvent.KeyInputEvent) {
        if (!InvKeyBinding.isPressed) return
        val player = Minecraft.getMinecraft().player
        PacketHandler.INSTANCE.sendToServer(MessageOpenGUI())
        player.openGui(QuickerCrafting, 0, player.world,
                player.posX.toInt(),
                player.posY.toInt(),
                player.posZ.toInt())
    }


    private var tickCounter = 0
    @JvmStatic
    @SubscribeEvent
    // Updates the recipe cache when the player is not in an inventory so the user doesnt have to wait to populate it
    // Only happens once per 20 ticks (usually every second)
    fun onPlayerTick(event: TickEvent.PlayerTickEvent) {
        if (event.side == Side.CLIENT && event.phase == TickEvent.Phase.START) {
            tickCounter += 1
            if (tickCounter < ConfigValues.RecipeCheckFrequency) return
            tickCounter = 0
            val player = Minecraft.getMinecraft().player
            // Let the container handle the recipe cache updating if its open, otherwise the recipecache falls out of sync
            if ((player.openContainer == null || player.openContainer !is ClientContainerQuickerCrafting) && !RecipeCache.isPopulating()) {
                RecipeCache.updateCache()
            }
        }
    }

    val quickerCraftingButton: GuiButtonImageBiggerTexture = GuiButtonImageBiggerTexture(100, 0, 0, 17, 15, 473, 204, 16, GuiQuickerCrafting.TEXTURE, 512f, 256f)
    @JvmStatic
    @SubscribeEvent
    // Adds our button to go to the quicker crafting menu in the inventory
    fun onRenderTick(event: GuiContainerEvent.DrawForeground) {
        if (!ConfigValues.ShouldDisplayQuickerCraftingButton || Minecraft.getMinecraft().currentScreen !is GuiInventory) return
        val inv = (Minecraft.getMinecraft().currentScreen as GuiInventory)

        quickerCraftingButton.x = inv.inventorySlots.inventorySlots[0].xPos
        quickerCraftingButton.y = inv.inventorySlots.inventorySlots[0].yPos - 24
        GlStateManager.pushMatrix()
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F)
        GlStateManager.disableLighting()
        quickerCraftingButton.drawButton(Minecraft.getMinecraft(), event.mouseX - inv.guiLeft, event.mouseY - inv.guiTop, 1f)
        GlStateManager.popMatrix()


    }

    @JvmStatic
    @SubscribeEvent
    fun onGuiInput(event: GuiScreenEvent.MouseInputEvent.Pre) {
        if (!ConfigValues.ShouldDisplayQuickerCraftingButton || Minecraft.getMinecraft().currentScreen !is GuiInventory || !Mouse.isButtonDown(0)) return
        val inv = (Minecraft.getMinecraft().currentScreen as GuiInventory)
        val scaledresolution = ScaledResolution(Minecraft.getMinecraft())
        val mouseX: Int = Mouse.getX() * scaledresolution.scaledWidth / Minecraft.getMinecraft().displayWidth
        val mouseY: Int = scaledresolution.scaledHeight - Mouse.getY() * scaledresolution.scaledHeight / Minecraft.getMinecraft().displayHeight - 1
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