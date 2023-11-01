/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.asm.transformers;

import meteordevelopment.meteorclient.asm.AsmTransformer;
import meteordevelopment.meteorclient.asm.Descriptor;
import meteordevelopment.meteorclient.asm.MethodInfo;
import meteordevelopment.meteorclient.systems.modules.misc.AntiPacketKick;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

// Future compatibility
// Future uses @ModifyConstant which does not chain when multiple mods do it and mixins / mixinextra can't target throw
// statements. So using a custom ASM transformer we wrap the throw statement inside another if statement.
public class PacketInflaterTransformer extends AsmTransformer {
    private final MethodInfo decodeMethod;

    public PacketInflaterTransformer() {
        super(mapClassName("net/minecraft/class_2532"));

        decodeMethod = new MethodInfo("net/minecraft/class_2532", "decode", new Descriptor("Lio/netty/channel/ChannelHandlerContext;", "Lio/netty/buffer/ByteBuf;", "Ljava/util/List;", "V"), true);
    }

    @Override
    public void transform(ClassNode klass) {
        MethodNode method = getMethod(klass, decodeMethod);
        if (method == null) error("[Meteor Client] Could not find method PacketInflater.decode()");

        int newCount = 0;
        LabelNode label = new LabelNode(new Label());

        //noinspection DataFlowIssue
        for (AbstractInsnNode insn : method.instructions) {
            if (insn instanceof TypeInsnNode typeInsn && typeInsn.getOpcode() == Opcodes.NEW && typeInsn.desc.equals("io/netty/handler/codec/DecoderException")) {
                newCount++;

                if (newCount == 2) {
                    InsnList list = new InsnList();

                    list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "meteordevelopment/meteorclient/systems/modules/Modules", "get", "()Lmeteordevelopment/meteorclient/systems/modules/Modules;", false));
                    list.add(new LdcInsnNode(Type.getType(AntiPacketKick.class)));
                    list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "meteordevelopment/meteorclient/systems/modules/Modules", "isActive", "(Ljava/lang/Class;)Z", false));

                    list.add(new JumpInsnNode(Opcodes.IFNE, label));

                    method.instructions.insertBefore(insn, list);
                }
            }
            else if (newCount == 2 && insn.getOpcode() == Opcodes.ATHROW) {
                method.instructions.insert(insn, label);
                return;
            }
        }

        error("[Meteor Client] Failed to modify PacketInflater.decode()");
    }
}
