/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.asm;

import meteordevelopment.meteorclient.asm.transformers.CanvasWorldRendererTransformer;
import meteordevelopment.meteorclient.asm.transformers.GameRendererTransformer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.util.version.SemanticVersionImpl;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.transformers.MixinClassWriter;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** When mixins are just not good enough **/
public class Asm {
    public static Asm INSTANCE;

    private final Map<String, AsmTransformer> transformers = new HashMap<>();
    private final boolean export;

    public Asm() {
        INSTANCE = this;

        add(new GameRendererTransformer());
        add(new CanvasWorldRendererTransformer());

        export = System.getProperty("meteor.asm.export") != null;
    }

    private void add(AsmTransformer transformer) {
        transformers.put(transformer.targetName, transformer);
    }

    public byte[] transform(String name, byte[] bytes) {
        AsmTransformer transformer = transformers.get(name);

        if (transformer != null) {
            ClassNode klass = new ClassNode();
            ClassReader reader = new ClassReader(bytes);
            reader.accept(klass, ClassReader.EXPAND_FRAMES);

            transformer.transform(klass);

            ClassWriter writer = new MixinClassWriter(reader, ClassWriter.COMPUTE_FRAMES);
            klass.accept(writer);
            bytes = writer.toByteArray();

            export(name, bytes);
        }

        return  bytes;
    }

    private void export(String name, byte[] bytes) {
        if (export) {
            try {
                Path path = Path.of(FabricLoader.getInstance().getGameDir().toString(), ".meteor.asm.out", name.replace('.', '/') + ".class");
                new File(path.toUri()).getParentFile().mkdirs();
                Files.write(path, bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public Class<?> createTransformer() {
        // Check fabric loader version
        Version version = FabricLoader.getInstance().getModContainer("fabricloader").get().getMetadata().getVersion();
        boolean is12OrNewer = version instanceof SemanticVersionImpl v && v.getVersionComponent(1) >= 12;

        // Names
        String asmName = Asm.class.getCanonicalName().replace('.', '/');
        String fabricMixinTransformerProxyName = is12OrNewer ? "org/spongepowered/asm/mixin/transformer/IMixinTransformer" : "org/spongepowered/asm/mixin/transformer/FabricMixinTransformerProxy";

        // Create class
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

        String name = Asm.class.getPackageName().replace('.', '/') + "/FabricClassLoaderTransformer";
        cw.visit(Opcodes.V16, Opcodes.ACC_PUBLIC, name, null, is12OrNewer ? "java/lang/Object" : fabricMixinTransformerProxyName, is12OrNewer ? new String[] { fabricMixinTransformerProxyName } : null);

        // Add delegate field
        String delegateDescriptor = "L" + fabricMixinTransformerProxyName + ";";
        cw.visitField(Opcodes.ACC_PUBLIC, "delegate", delegateDescriptor, null, null);

        // Override transformClassBytes method
        {
            MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "transformClassBytes", "(Ljava/lang/String;Ljava/lang/String;[B)[B", null, null);
            mv.visitCode();

            // basicClass = delegate.transformClassBytes(name, transformedName, basicClass);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitFieldInsn(Opcodes.GETFIELD, name, "delegate", delegateDescriptor);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitVarInsn(Opcodes.ALOAD, 2);
            mv.visitVarInsn(Opcodes.ALOAD, 3);
            mv.visitMethodInsn(is12OrNewer ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL, fabricMixinTransformerProxyName, "transformClassBytes", "(Ljava/lang/String;Ljava/lang/String;[B)[B", is12OrNewer);
            mv.visitVarInsn(Opcodes.ASTORE, 3);

            // return Asm.INSTANCE.transform(name, basicClass);
            mv.visitFieldInsn(Opcodes.GETSTATIC, asmName, "INSTANCE", "L" + asmName + ";");
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitVarInsn(Opcodes.ALOAD, 3);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, asmName, "transform", "(Ljava/lang/String;[B)[B", false);
            mv.visitInsn(Opcodes.ARETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        // Generate dummy wrapper methods
        if (is12OrNewer) {
            try {
                for (Method method : Class.forName(fabricMixinTransformerProxyName.replace('/', '.')).getDeclaredMethods()) {
                    // Skip transformClassBytes
                    if (method.getName().equals("transformClassBytes")) continue;

                    // Generate descriptor
                    StringBuilder sb = new StringBuilder("(");
                    for (Class<?> klass : method.getParameterTypes()) sb.append(klass.descriptorString());
                    sb.append(')').append(method.getReturnType().descriptorString());
                    String descriptor = sb.toString();

                    // Create method
                    MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, method.getName(), descriptor, null, null);
                    mv.visitCode();

                    mv.visitVarInsn(Opcodes.ALOAD, 0);
                    mv.visitFieldInsn(Opcodes.GETFIELD, name, "delegate", delegateDescriptor);
                    for (int i = 0; i < method.getParameterCount(); i++) mv.visitVarInsn(Opcodes.ALOAD, i + 1);
                    mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, fabricMixinTransformerProxyName, method.getName(), descriptor, true);

                    if (method.getReturnType() == void.class) mv.visitInsn(Opcodes.RETURN);
                    else if (method.getReturnType() == boolean.class) mv.visitInsn(Opcodes.IRETURN);
                    else mv.visitInsn(Opcodes.ARETURN);

                    mv.visitMaxs(0, 0);
                    mv.visitEnd();
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        cw.visitEnd();

        // Load class
        try {
            byte[] bytes = cw.toByteArray();
            Class<?> klass = MethodHandles.lookup().defineHiddenClass(bytes, true).lookupClass();

            export(klass.getPackageName() + ".FabricClassLoaderTransformer", bytes);
            return klass;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }
}
