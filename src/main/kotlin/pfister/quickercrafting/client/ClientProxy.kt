package pfister.quickercrafting.client

import net.minecraft.client.Minecraft
import net.minecraft.client.settings.KeyBinding
import net.minecraftforge.client.event.ModelRegistryEvent
import net.minecraftforge.fml.client.registry.ClientRegistry
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent
import net.minecraftforge.fml.relauncher.Side
import org.lwjgl.input.Keyboard
import pfister.quickercrafting.MOD_ID
import pfister.quickercrafting.QuickerCrafting
import pfister.quickercrafting.common.CommonProxy
import pfister.quickercrafting.common.item.ModItems
import pfister.quickercrafting.common.network.MessageOpenGUI
import pfister.quickercrafting.common.network.PacketHandler

class ClientProxy : CommonProxy() {

    override fun init(event: FMLInitializationEvent) {
        super.init(event)
        ClientRegistry.registerKeyBinding(ClientEventListener.InvKeyBinding)
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
}