package pfister.quickercrafting.client

import net.minecraft.client.settings.KeyBinding
import net.minecraftforge.client.settings.IKeyConflictContext
import net.minecraftforge.client.settings.KeyModifier

// Hooks a keybinding into another one, essentially combines them. If one is pressed then the entire keybinding is pressed.
class KeybindingHook(private val hookedKeyBinding: KeyBinding, private val callbackKeyBinding: KeyBinding) : KeyBinding(hookedKeyBinding.keyDescription, hookedKeyBinding.keyConflictContext, hookedKeyBinding.keyCode, hookedKeyBinding.keyCategory) {
    var IsHookEnabled = true
    override fun isActiveAndMatches(keyCode: Int): Boolean {
        return if (IsHookEnabled) {
            hookedKeyBinding.isActiveAndMatches(keyCode) || callbackKeyBinding.isActiveAndMatches(keyCode)
        } else {
            hookedKeyBinding.isActiveAndMatches(keyCode)
        }
    }

    override fun isKeyDown(): Boolean {
        hookedKeyBinding.pressTime = this.pressTime
        hookedKeyBinding.pressed = this.pressed
        return hookedKeyBinding.isKeyDown || callbackKeyBinding.isKeyDown
    }

    override fun isPressed(): Boolean {
        hookedKeyBinding.pressTime = this.pressTime
        hookedKeyBinding.pressed = this.pressed
        return hookedKeyBinding.isPressed || callbackKeyBinding.isPressed
    }

    override fun setKeyCode(keyCode: Int) {
        hookedKeyBinding.keyCode = keyCode
    }

    override fun setKeyConflictContext(keyConflictContext: IKeyConflictContext) {
        hookedKeyBinding.keyConflictContext = keyConflictContext
    }

    override fun conflicts(other: KeyBinding): Boolean = hookedKeyBinding.conflicts(other)
    override fun compareTo(other: KeyBinding): Int = hookedKeyBinding.compareTo(other)
    override fun equals(other: Any?): Boolean = hookedKeyBinding == other
    override fun hashCode(): Int = hookedKeyBinding.hashCode()
    override fun getKeyCategory(): String = hookedKeyBinding.keyCategory
    override fun getKeyCode(): Int = hookedKeyBinding.keyCode
    override fun getKeyCodeDefault(): Int = hookedKeyBinding.keyCodeDefault
    override fun getKeyConflictContext(): IKeyConflictContext = hookedKeyBinding.keyConflictContext
    override fun getKeyDescription(): String = hookedKeyBinding.keyDescription
    // Don't remove the null check, the parent constructor accesses this method and hookedKeyBinding isn't assigned at that time for some reason
    // Thanks kotlin
    @Suppress("SENSELESS_COMPARISON")
    override fun getKeyModifier(): KeyModifier = if (hookedKeyBinding != null) hookedKeyBinding.keyModifier else super.getKeyModifier()

    override fun getKeyModifierDefault(): KeyModifier = hookedKeyBinding.keyModifierDefault
    override fun hasKeyCodeModifierConflict(other: KeyBinding): Boolean = hookedKeyBinding.hasKeyCodeModifierConflict(other)
    override fun isSetToDefaultValue(): Boolean = hookedKeyBinding.isSetToDefaultValue
    override fun setKeyModifierAndCode(keyModifier: KeyModifier, keyCode: Int) = hookedKeyBinding.setKeyModifierAndCode(keyModifier, keyCode)
    override fun setToDefault() = hookedKeyBinding.setToDefault()
    override fun toString(): String = hookedKeyBinding.toString()
    override fun getDisplayName(): String = hookedKeyBinding.displayName
}