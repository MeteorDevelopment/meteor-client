/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.asm;

import meteordevelopment.meteorclient.asm.transformers.CanvasWorldRendererTransformer;
import meteordevelopment.meteorclient.asm.transformers.GameRendererTransformer;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.mixin.transformer.ext.IExtensionRegistry;
import org.spongepowered.asm.transformers.MixinClassWriter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** When mixins are just not good enough **/
public class Asm {
    public static Asm INSTANCE;

    private final Map<String, AsmTransformer> transformers = new HashMap<>();
    private final boolean export;

    public Asm(boolean export) {
        this.export = export;
    }

    public static void init() {
        if (INSTANCE != null) return;

        INSTANCE = new Asm(System.getProperty("meteor.asm.export") != null);
        INSTANCE.add(new GameRendererTransformer());
        INSTANCE.add(new CanvasWorldRendererTransformer());
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

    public static class Transformer implements IMixinTransformer {
        public IMixinTransformer delegate;

        @Override
        public void audit(MixinEnvironment environment) {
            delegate.audit(environment);
        }

        @Override
        public List<String> reload(String mixinClass, ClassNode classNode) {
            return delegate.reload(mixinClass, classNode);
        }

        @Override
        public boolean computeFramesForClass(MixinEnvironment environment, String name, ClassNode classNode) {
            return delegate.computeFramesForClass(environment, name, classNode);
        }

        @Override
        public byte[] transformClassBytes(String name, String transformedName, byte[] basicClass) {
            basicClass = delegate.transformClassBytes(name, transformedName, basicClass);
            return Asm.INSTANCE.transform(name, basicClass);
        }

        @Override
        public byte[] transformClass(MixinEnvironment environment, String name, byte[] classBytes) {
            return delegate.transformClass(environment, name, classBytes);
        }

        @Override
        public boolean transformClass(MixinEnvironment environment, String name, ClassNode classNode) {
            return delegate.transformClass(environment, name, classNode);
        }

        @Override
        public byte[] generateClass(MixinEnvironment environment, String name) {
            return delegate.generateClass(environment, name);
        }

        @Override
        public boolean generateClass(MixinEnvironment environment, String name, ClassNode classNode) {
            return delegate.generateClass(environment, name, classNode);
        }

        @Override
        public IExtensionRegistry getExtensions() {
            return delegate.getExtensions();
        }
    }
}
