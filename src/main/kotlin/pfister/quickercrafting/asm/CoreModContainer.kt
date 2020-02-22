package pfister.quickercrafting.asm

import net.minecraftforge.fml.common.DummyModContainer
import net.minecraftforge.fml.common.ModMetadata
import pfister.quickercrafting.VERSION

class CoreModContainer : DummyModContainer(ModMetadata()) {
    init {
        this.metadata.authorList = listOf("MiddaPhofidda")
        this.metadata.modId = "quickercraftingcore"
        this.metadata.version = VERSION
        this.metadata.name = "Quicker Crafting Coremod"
        this.metadata.description = "Contains core modifications for QuickerCrafting"

    }
}