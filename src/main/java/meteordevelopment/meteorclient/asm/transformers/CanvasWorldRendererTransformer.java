/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.asm.transformers;

import meteordevelopment.meteorclient.asm.AsmTransformer;
import meteordevelopment.meteorclient.asm.Descriptor;
import meteordevelopment.meteorclient.asm.MethodInfo;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class CanvasWorldRendererTransformer extends AsmTransformer {
    private final MethodInfo renderWorldMethod, drawMethod;

    public CanvasWorldRendererTransformer() {
        super("grondag.canvas.render.world.CanvasWorldRenderer");

        renderWorldMethod = new MethodInfo(null, "renderWorld", null, false);

        // OutlineVertexConsumerProvider.draw()
        drawMethod = new MethodInfo("net/minecraft/class_4618", "method_23285", new Descriptor("V"), true);
    }

    @Override
    public void transform(ClassNode klass) {
        // Inject EntityShaders.endRender() after OutlineVertexConsumerProvider.draw() in CanvasWorldRenderer
        MethodNode method = getMethod(klass, renderWorldMethod);
        if (method == null) return;

        for (AbstractInsnNode insn : method.instructions) {
            if (!(insn instanceof MethodInsnNode in)) continue;

            if (drawMethod.equals(in)) {
                method.instructions.insert(insn, new MethodInsnNode(Opcodes.INVOKESTATIC, "meteordevelopment/meteorclient/utils/render/EntityShaders", "endRender", "()V"));
                break;
            }
        }
    }
}
