package me.basiqueevangelist.multithreadbootstrap;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class AsmUtil {
    private AsmUtil() {

    }

    public static void insertInstructions(MethodNode method, List<AbstractInsnNode> insns, AbstractInsnNode lastInsn) {
        IdentityHashMap<LabelNode, LabelNode> labels = new IdentityHashMap<>();
        for (AbstractInsnNode insn : insns) {
            if (insn instanceof LabelNode) {
                labels.put((LabelNode) insn, new LabelNode());
            }
        }

        for (AbstractInsnNode insn : insns) {
            method.instructions.add(insn.clone(labels));
            if (insn == lastInsn)
                return;
        }
    }

    public static void openMethod(ClassNode source, String name, String desc) {
        var targetedMethod = source.methods
            .stream()
            .filter(x -> x.name.equals(name) && x.desc.equals(desc))
            .findFirst()
            .get();
        targetedMethod.access &= ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED);
        targetedMethod.access |= Opcodes.ACC_PUBLIC;
    }

    public static void openField(ClassNode source, String name, String desc) {
        var targetedField = source.fields
            .stream()
            .filter(x -> x.name.equals(name) && x.desc.equals(desc))
            .findFirst()
            .get();
        targetedField.access &= ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED | Opcodes.ACC_FINAL);
        targetedField.access |= Opcodes.ACC_PUBLIC;
    }

    public static Stream<AbstractInsnNode> instructionsStream(InsnList list) {
        return StreamSupport.stream(Spliterators.spliterator(list.iterator(), list.size(), 0), false);
    }

    public static <T extends AbstractInsnNode> T getLastInstructionThat(InsnList list, Function<AbstractInsnNode, T> filterMapper) {
         AbstractInsnNode current = list.getLast();
         do {
             T transformed = filterMapper.apply(current);
             if (transformed != null)
                 return transformed;
             current = current.getPrevious();
         } while (current != null);
         return null;
    }

    public static final Handle LAMBDA_METAFACTORY = new Handle(Opcodes.H_INVOKESTATIC, "java/lang/invoke/LambdaMetafactory", "metafactory", "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;", false);
}
