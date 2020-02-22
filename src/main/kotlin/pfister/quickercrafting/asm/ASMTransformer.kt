package pfister.quickercrafting.asm

import net.minecraft.launchwrapper.IClassTransformer
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*


class ASMTransformer : IClassTransformer {
    override fun transform(name: String?, transformedName: String?, basicClass: ByteArray?): ByteArray? {
        if (basicClass == null || basicClass.isEmpty()) return basicClass

        if (transformedName == "net.minecraft.client.gui.inventory.GuiContainer") {
            return doGuiContainerTransform(basicClass)
        }
        return basicClass
    }

    fun doGuiContainerTransform(bytes: ByteArray?): ByteArray? {
        println("QuickerCrafting: Beginning GuiContainer.onKeyTyped Transform...")
        val classReader = ClassReader(bytes)
        val classNode = ClassNode(Opcodes.ASM4)
        val classWriter = ClassWriter(ClassWriter.COMPUTE_FRAMES.or(ClassWriter.COMPUTE_MAXS))

        classReader.accept(classNode, 0)

        val keyTypedMethod = classNode.methods.find { it.name == "keyTyped" || (it.name == "a" && it.desc == "(CI)V") }
                ?: return bytes
        println("Found keyTypedMethod")
        val code = keyTypedMethod.instructions
        var ifcmpeqInsn: AbstractInsnNode? = null
        val iterator = code.iterator()
        while (iterator.hasNext()) {
            val insn = iterator.next()
            if (insn.opcode == Opcodes.IF_ICMPEQ && insn.previous.opcode == Opcodes.ICONST_1) {
                ifcmpeqInsn = insn
                break
            }
        }
        if (ifcmpeqInsn == null) return bytes
        println("Found ifcmpeq Insn")
        var labelNode: LabelNode? = (ifcmpeqInsn as JumpInsnNode).label

        // Load keybinding onto stack
        val keybinding = MethodInsnNode(Opcodes.INVOKESTATIC, "pfister/quickercrafting/client/ClientEventListener", "getInvKeyBinding", "()Lnet/minecraft/client/settings/KeyBinding;", false)
        // Load keycode onto staack
        val load = VarInsnNode(Opcodes.ILOAD, 2)
        // Invoke isActiveAndMatches
        val methodInvoke = MethodInsnNode(Opcodes.INVOKEVIRTUAL, "net/minecraft/client/settings/KeyBinding", "isActiveAndMatches", "(I)Z", false)
        // If statement is true, jump to L1
        val ifStatement = JumpInsnNode(Opcodes.IFNE, labelNode)

        val insnList = InsnList()
        insnList.add(keybinding)
        insnList.add(load)
        insnList.add(methodInvoke)
        insnList.add(ifStatement)
        code.insert(ifcmpeqInsn, insnList)

        classNode.accept(classWriter)

        println("QuickerCrafting: Added hook into GuiContainer.onKeyTyped")
        return classWriter.toByteArray()
    }

}