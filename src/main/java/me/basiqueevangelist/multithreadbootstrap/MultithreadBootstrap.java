package me.basiqueevangelist.multithreadbootstrap;

import com.chocohead.mm.api.ClassTinkerers;
import com.google.common.collect.ImmutableMap;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;
import org.apache.commons.lang3.ArrayUtils;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.transformer.ClassInfo;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.transformers.MixinClassWriter;
import user11681.reflect.Classes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.StreamSupport;

public class MultithreadBootstrap implements Runnable {
    private static final String REMAPPED_BLOCKS = FabricLoader.getInstance().getMappingResolver().mapClassName("intermediary", "net.minecraft.class_2246").replace('.', '/');
    private static final String REMAPPED_ITEMS = FabricLoader.getInstance().getMappingResolver().mapClassName("intermediary", "net.minecraft.class_1802").replace('.', '/');
    private static final String REMAPPED_MAPPEDREGISTRY = FabricLoader.getInstance().getMappingResolver().mapClassName("intermediary", "net.minecraft.class_2370").replace('.', '/');
    private static final String REMAPPED_DISPENSERBLOCK = FabricLoader.getInstance().getMappingResolver().mapClassName("intermediary", "net.minecraft.class_2315").replace('.', '/');
    private static final String REMAPPED_BIOMES = FabricLoader.getInstance().getMappingResolver().mapClassName("intermediary", "net.minecraft.class_5504").replace('.', '/');

    private static final String REMAPPED_REGISTER_MAPPING = FabricLoader.getInstance().getMappingResolver().mapMethodName("intermediary", "net.minecraft.class_2370", "method_31051", "(ILnet/minecraft/class_5321;Ljava/lang/Object;Lcom/mojang/serialization/Lifecycle;Z)Ljava/lang/Object;").replace('.', '/');
    private static final String REMAPPED_REGISTER = FabricLoader.getInstance().getMappingResolver().mapMethodName("intermediary", "net.minecraft.class_2385", "method_10272", "(Lnet/minecraft/class_5321;Ljava/lang/Object;Lcom/mojang/serialization/Lifecycle;)Ljava/lang/Object;").replace('.', '/');
    private static final String REMAPPED_REGISTER_BEHAVIOR = FabricLoader.getInstance().getMappingResolver().mapMethodName("intermediary", "net.minecraft.class_2315", "method_10009", "(Lnet/minecraft/class_1935;Lnet/minecraft/class_2357;)V").replace('.', '/');
    private static final String REMAPPED_BIOMES_REGISTER = FabricLoader.getInstance().getMappingResolver().mapMethodName("intermediary", "net.minecraft.class_5504", "method_31145", "(ILnet/minecraft/class_5321;Lnet/minecraft/class_1959;)Lnet/minecraft/class_1959;").replace('.', '/');

    public static final boolean ENABLED = true;
    public static final int THRESHOLD = 5;

    public static final ImmutableMap<String, String> TRACKED_CLASSES = ImmutableMap.<String, String>builder()
        .put(REMAPPED_BLOCKS, "Blocks")
        .put(REMAPPED_ITEMS, "Items")
        .build();

    @Override
    public void run() {
        if (ENABLED) {
            System.out.println("I have become concern");

            ClassTinkerers.addTransformation(REMAPPED_BLOCKS, MultithreadBootstrap.makeTransformer(REMAPPED_BLOCKS, "Blocks"));
            ClassTinkerers.addTransformation(REMAPPED_ITEMS, MultithreadBootstrap.makeTransformer(REMAPPED_ITEMS, "Items"));
            ClassTinkerers.addTransformation(REMAPPED_MAPPEDREGISTRY, MultithreadBootstrap::transformMappedRegistry);
            ClassTinkerers.addTransformation(REMAPPED_DISPENSERBLOCK, MultithreadBootstrap::transformDispenserBlock);
        }
    }

    private static void transformDispenserBlock(ClassNode klass) {
        for (MethodNode method : klass.methods) {
            if (method.name.equals(REMAPPED_REGISTER_BEHAVIOR)) {
                method.access |= Opcodes.ACC_SYNCHRONIZED;
            }
        }
    }

    private static void transformMappedRegistry(ClassNode klass) {
        for (MethodNode method : klass.methods) {
            if (method.name.equals(REMAPPED_REGISTER_MAPPING) || method.name.equals(REMAPPED_REGISTER)) {
                method.access |= Opcodes.ACC_SYNCHRONIZED;
            }
        }
    }

    private static int getBlockFor(String className, String fieldName) {
        try {
            ClassNode klass = MixinService.getService().getBytecodeProvider().getClassNode(className);

            for (MethodNode method : klass.methods) {
                if (method.name.equals("<clinit>")) {
                    int i = 0;
                    int inBlock = 0;
                    List<AbstractInsnNode> currentInstructions = new ArrayList<>();

                    for (AbstractInsnNode insn : method.instructions) {
                        currentInstructions.add(insn);

                        if (insn instanceof FieldInsnNode fieldInsn && insn.getOpcode() == Opcodes.PUTSTATIC) {
                            inBlock++;
                            if (fieldInsn.name.equals(fieldName))
                                return i;
                            if (inBlock < THRESHOLD)  {
                                continue;
                            }

                            inBlock = 0;
                            i++;
                        }
                    }
                }
            }

        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return -1;
    }

    private static void makeBlock(String className, String shortName, int i, MethodNode method, List<AbstractInsnNode> currentInstructions, List<MethodNode> addedMethods, FieldInsnNode lastFieldInsn) {
        var tasksClassName = "me/basiqueevangelist/multithreadbootstrap/generated/" + shortName + "Tasks";

        String methodName = "register" + shortName + i;
        MethodNode tempMethod = new MethodNode(Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC, methodName, "()V", null, new String[0]);

        AsmUtil.insertInstructions(tempMethod, currentInstructions, lastFieldInsn);
        tempMethod.instructions.add(new InsnNode(Opcodes.RETURN));

        Set<String> dependencies = new HashSet<>();
        for (AbstractInsnNode targetInsn : tempMethod.instructions) {
            if (targetInsn instanceof FieldInsnNode fieldInsn && fieldInsn.getOpcode() == Opcodes.GETSTATIC && TRACKED_CLASSES.containsKey(fieldInsn.owner)) {
                int block = getBlockFor(fieldInsn.owner, fieldInsn.name);

                if (fieldInsn.owner.equals(className) && block == i) continue;

                dependencies.add(TRACKED_CLASSES.get(fieldInsn.owner) + "/" + block);
            }
        }

        AbstractInsnNode firstInsn = currentInstructions.get(0);

        addedMethods.add(tempMethod);

        InvokeDynamicInsnNode makeLambda = new InvokeDynamicInsnNode(
            "run",
            "()Ljava/lang/Runnable;",
            AsmUtil.LAMBDA_METAFACTORY,
            Type.getMethodType("()V"),
            new Handle(Opcodes.H_INVOKESTATIC, tasksClassName, methodName, "()V", false),
            Type.getMethodType("()V"));

        method.instructions.insertBefore(firstInsn, new LdcInsnNode(shortName + "/" + i));
        method.instructions.insertBefore(firstInsn, new LdcInsnNode(String.join(";", dependencies)));
        method.instructions.insertBefore(firstInsn, makeLambda);
        MethodInsnNode callUtil = new MethodInsnNode(Opcodes.INVOKESTATIC, "me/basiqueevangelist/multithreadbootstrap/MultithreadUtil", "runDelayed", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Runnable;)V");
        method.instructions.insertBefore(firstInsn, callUtil);

        for (AbstractInsnNode removed : currentInstructions) {
            method.instructions.remove(removed);
        }
        currentInstructions.clear();
    }

    private static Consumer<ClassNode> makeTransformer(String className, String shortName) {
        return (klass) -> {
            var tasksClassName = "me/basiqueevangelist/multithreadbootstrap/generated/" + shortName + "Tasks";

            List<MethodNode> addedMethods = new ArrayList<>();
            for (MethodNode method : klass.methods) {
                if (method.name.equals("<clinit>")) {
                    int i = 0;
                    int inBlock = 0;
                    List<AbstractInsnNode> currentInstructions = new ArrayList<>();
                    FieldInsnNode lastFieldInsn = null;

                    for (AbstractInsnNode insn : method.instructions) {
                        currentInstructions.add(insn);

                        if (insn instanceof FieldInsnNode && insn.getOpcode() == Opcodes.PUTSTATIC) {
                            lastFieldInsn = (FieldInsnNode) insn;
                            inBlock++;
                            if (inBlock < THRESHOLD) {
                                continue;
                            }
                            inBlock = 0;

                            makeBlock(className, shortName, i++, method, currentInstructions, addedMethods, lastFieldInsn);
                        }
                    }

                    if (inBlock > 0) {
                        makeBlock(className, shortName, i++, method, currentInstructions, addedMethods, lastFieldInsn);
                    }

                    for (AbstractInsnNode removed : currentInstructions) {
                        method.instructions.remove(removed);
                    }
                    method.instructions.insert(method.instructions.getLast(), new InsnNode(Opcodes.RETURN));

                    MethodInsnNode callInit = new MethodInsnNode(Opcodes.INVOKESTATIC, "me/basiqueevangelist/multithreadbootstrap/MultithreadUtil", "init", "()V");
                    method.instructions.insertBefore(method.instructions.get(0), callInit);

//                AbstractInsnNode firstInsn = currentInstructions.get(0);
//                MethodInsnNode waitForAll = new MethodInsnNode(Opcodes.INVOKESTATIC, "me/basiqueevangelist/multithreadbootstrap/MultithreadUtil", "waitForAll", "()V");
//                method.instructions.insertBefore(firstInsn, waitForAll);
                }
            }

            for (MethodNode addedMethod : addedMethods) {
                for (AbstractInsnNode insn : addedMethod.instructions) {
                    if (insn instanceof MethodInsnNode methodInsn && Objects.equals(methodInsn.owner, className)) {
                        AsmUtil.openMethod(klass, methodInsn.name, methodInsn.desc);
                    } else if (insn instanceof FieldInsnNode fieldInsn && Objects.equals(fieldInsn.owner, className)) {
                        AsmUtil.openField(klass, fieldInsn.name, fieldInsn.desc);
                    } else if (insn instanceof InvokeDynamicInsnNode indyInsn) {
                        for (Object bsmArg : indyInsn.bsmArgs) {
                            if (bsmArg instanceof Handle handle && Objects.equals(handle.getOwner(), className)) {
                                AsmUtil.openMethod(klass, handle.getName(), handle.getDesc());
                            }
                        }
                    }
                }
            }

//        klass.methods.addAll(addedMethods);

            ClassNode tasksClass = new ClassNode(Opcodes.ASM9);
            tasksClass.name = tasksClassName;
            tasksClass.access = Opcodes.ACC_SUPER | Opcodes.ACC_PUBLIC;
            tasksClass.version = Opcodes.V1_8;
            tasksClass.superName = "java/lang/Object";
            tasksClass.methods.addAll(addedMethods);
            ClassWriter writer = new MixinClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            tasksClass.accept(writer);

//            try {
//                Path classFile = Paths.get("multithread-bootstrap-dump/" + tasksClass.name + ".class");
//                Files.createDirectories(classFile.getParent());
//                Files.write(classFile, writer.toByteArray());
//            } catch (IOException i) {
//                i.printStackTrace();
//            }

            ClassTinkerers.define(tasksClass.name, writer.toByteArray());
        };
    }
}
