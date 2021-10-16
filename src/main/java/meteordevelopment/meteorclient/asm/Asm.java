/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.asm;

import io.gitlab.jfronny.libjf.unsafe.asm.AsmConfig;
import io.gitlab.jfronny.libjf.unsafe.asm.patch.Patch;
import meteordevelopment.meteorclient.asm.transformers.CanvasWorldRendererTransformer;
import meteordevelopment.meteorclient.asm.transformers.GameRendererTransformer;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.transformers.MixinClassWriter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** When mixins are just not good enough **/
public class Asm implements AsmConfig {
    private final Set<Patch> transformers = new HashSet<>();

    public Asm() {
        add(new GameRendererTransformer());
        add(new CanvasWorldRendererTransformer());
    }

    private void add(AsmTransformer transformer) {
        transformers.add(new Patch() {
            @Override
            public void apply(ClassNode klazz) {
                if (klazz.name.equals(transformer.targetName))
                    transformer.transform(klazz);
            }
        });
    }

    @Override
    public Set<String> skipClasses() {
        return null;
    }

    @Override
    public Set<Patch> getPatches() {
        return transformers;
    }
}
