package pfister.quickercrafting.common


import net.minecraftforge.common.config.Config
import pfister.quickercrafting.MOD_ID
import pfister.quickercrafting.common.network.MessageSyncConfig

@Config(modid = MOD_ID, name = "QuickerCrafting")
object ConfigValues {
    fun generateSyncPacket(): MessageSyncConfig {
        return MessageSyncConfig(CraftingTableRadius, CraftingDepth)
    }

    @JvmField
    @Config.Comment("The distance (in blocks) one needs to be to a crafting table to unlock 3x3 recipes.", "Set to a negative value to always allow 3x3 recipes.", "A zero value will never allow 3x3 recipes.", "A crafting table is looked for in this radius on gui open.", "Increasing this will have a (small) negative effect on performance.")
    @Config.RangeInt(min = -1, max = 10)
    @Config.RequiresWorldRestart
    var CraftingTableRadius: Int = -1

    @JvmField
    @Config.RequiresMcRestart
    @Config.Comment("Enables hooking the QuickerCrafting keybind into the inventory keybind.", "Disabling this means that pressing the QuickerCrafting keybind will no longer close the GuiContainer to match vanilla inventory keybind functionality.", "You shouldn't have to disable this (unless you don't like it), and if you need to please give me a bug report.")
    var HookCraftingKeybind: Boolean = true

    @JvmField
    @Config.Comment("Enables the button in the inventory to switch to the QuickerCrafting menu.")
    var ShouldDisplayQuickerCraftingButton: Boolean = true

    @JvmField
    @Config.Comment("How many crafting steps you can do.", "A value greater than 1 enables multi-stage crafting, meaning you can crafting a pickaxe just from planks by going through intermediate steps to craft sticks.", "Higher values may slowdown crafting.", "Currently not-implemented, so changing this does nothing.")
    @Config.RangeInt(min = 1, max = 1)
    @Config.RequiresWorldRestart
    var CraftingDepth: Int = 1

    @JvmField
    @Config.Comment("How frequent to update the crafting recipes in the background, in ticks.", "Lower values incur higher performance hits.", "Default is once every 20 ticks (about once a second).")
    @Config.RangeInt(min = 1)
    var RecipeCheckFrequency = 20

    @JvmField
    @Config.Comment("Should we play the anvil sound on every craft?")
    var PlayCraftSound = true

    @JvmField
    @Config.Comment("Blocks that work as a valid crafting table, meaning they'll enable 3x3 recipes.", "Only used if distance to crafting table mechanics are on.")
    var ValidCraftingTableBlocks: Array<String> = arrayOf("minecraft:crafting_table")

    @JvmField
    @Config.Comment("Items that work as a valid crafting table, meaning they'll enable 3x3 recipes if they're in inventory.", "Only used if distance to crafting table mechanics are on.")
    var ValidCraftingTableItems: Array<String> = arrayOf("")
}