/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.asm;

import meteordevelopment.meteorclient.asm.transformers.CanvasWorldRendererTransformer;
import meteordevelopment.meteorclient.asm.transformers.GameRendererTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.transformers.MixinClassWriter;

import java.util.HashMap;
import java.util.Map;

/** When mixins are just not good enough **/
public class Asm {
    private final Map<String, AsmTransformer> transformers = new HashMap<>();

    public Asm() {
        add(new GameRendererTransformer());
        add(new CanvasWorldRendererTransformer());
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

            ClassWriter writer = new MixinClassWriter(reader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            klass.accept(writer);
            return writer.toByteArray();
        }

        return  bytes;
    }
}
