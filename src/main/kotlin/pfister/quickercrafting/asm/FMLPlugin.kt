package pfister.quickercrafting.asm

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin

// Thank you minecraft forge for your extensive and totally complete documentation on coremods
// Mixins can't come soon enough :(

@IFMLLoadingPlugin.Name("QuickerCrafting Coremod")
@IFMLLoadingPlugin.TransformerExclusions("pfister.quickercrafting.asm", "kotlin")
@IFMLLoadingPlugin.MCVersion("1.12.2")
class FMLPlugin : IFMLLoadingPlugin {
    object FMLPlugin {
        @JvmStatic
        var runtimeDeobf = false
    }

    override fun getModContainerClass(): String? {
        return "pfister.quickercrafting.asm.CoreModContainer"
    }

    override fun getASMTransformerClass(): Array<String>? {
        return arrayOf("pfister.quickercrafting.asm.ASMTransformer")
    }

    override fun getSetupClass(): String? {
        return null
    }

    override fun injectData(data: MutableMap<String, Any>?) {
        FMLPlugin.runtimeDeobf = data?.get("runtimeDeobfuscationEnabled") as? Boolean ?: false
    }

    override fun getAccessTransformerClass(): String? {
        return null
    }

}