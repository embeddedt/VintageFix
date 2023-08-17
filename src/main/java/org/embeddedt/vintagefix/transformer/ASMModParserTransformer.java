package org.embeddedt.vintagefix.transformer;

import static org.objectweb.asm.Opcodes.*;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import net.minecraft.launchwrapper.IClassTransformer;

/** Adds an empty constructor to ASMModParser so it can be serialized by JarDiscovererCache. */
public class ASMModParserTransformer implements IClassTransformer {

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if(name.equals("net.minecraftforge.fml.common.discovery.asm.ASMModParser")) {
            basicClass = doTransform(basicClass);
        }
        return basicClass;
    }

    private static byte[] doTransform(byte[] bytes) {
        try {
            ClassNode classNode = new ClassNode();
            ClassReader classReader = new ClassReader(bytes);
            classReader.accept(classNode, 0);
            MethodNode emptyConstructor = new MethodNode(ACC_PUBLIC, "<init>", "()V", null, null);

            emptyConstructor.instructions.add(new VarInsnNode(ALOAD, 0));
            emptyConstructor.instructions.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false));
            emptyConstructor.instructions.add(new VarInsnNode(ALOAD, 0));
            emptyConstructor.instructions.add(new MethodInsnNode(INVOKESTATIC, "com/google/common/collect/Lists", "newLinkedList", "()Ljava/util/LinkedList;", false));
            emptyConstructor.instructions.add(new FieldInsnNode(PUTFIELD, "net/minecraftforge/fml/common/discovery/asm/ASMModParser", "annotations", "Ljava/util/LinkedList;"));
            emptyConstructor.instructions.add(new InsnNode(Opcodes.RETURN));

            classNode.methods.add(emptyConstructor);

            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            classNode.accept(writer);
            return writer.toByteArray();
        } catch(Exception e) {
            e.printStackTrace();
        }
        return bytes;
    }
}
