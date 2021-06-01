/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class MixinPlugin implements IMixinConfigPlugin {
    private boolean isResourceLoaderPresent = false;
    private String gameRenderer, getFovDesc;

    @Override
    public void onLoad(String mixinPackage) {
        for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            if (mod.getMetadata().getId().startsWith("fabric-resource-loader")) {
                isResourceLoaderPresent = true;
                break;
            }
        }

        gameRenderer = FabricLoader.getInstance().getMappingResolver().mapClassName("intermediary", "net.minecraft.class_757");
        getFovDesc = "(L" + FabricLoader.getInstance().getMappingResolver().mapClassName("intermediary", "net.minecraft.class_4184").replace('.', '/') + ";FZ)D";
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.endsWith("NamespaceResourceManagerMixin") || mixinClassName.endsWith("ReloadableResourceManagerImplMixin")) {
            return !isResourceLoaderPresent;
        }

        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        // Couldn't get it working with a mixins so here we go

        // Modify GameRenderer.getFov()
        // return MeteorClient.EVENT_BUS.post(GetFovEvent.get(d));
        if (targetClassName.equals(gameRenderer)) {
            MethodNode method = getMethod(targetClass, getFovDesc);
            if (method == null) throw new RuntimeException("[Meteor Client] Could not find method GameRenderer.getFov()");

            // Find injection point
            AbstractInsnNode lastInsn = null;
            AbstractInsnNode lastLabel = null;

            VarInsnNode targetInsn = null;

            for (AbstractInsnNode insn : method.instructions) {
                if (insn.getOpcode() == Opcodes.DRETURN && lastInsn instanceof VarInsnNode && lastInsn.getOpcode() == Opcodes.DLOAD && lastLabel != null) {
                    targetInsn = (VarInsnNode) lastInsn;
                    break;
                }

                lastInsn = insn;
                if (insn instanceof LabelNode) lastLabel = lastInsn;
            }

            if (targetInsn == null) throw new RuntimeException("[Meteor Client] Could not find injection point for GameRenderer.getFov()");

            // Inject
            InsnList insns = new InsnList();

            insns.add(new FieldInsnNode(Opcodes.GETSTATIC, "minegame159/meteorclient/MeteorClient", "EVENT_BUS", "Lmeteordevelopment/orbit/IEventBus;"));
            insns.add(new VarInsnNode(Opcodes.DLOAD, targetInsn.var));
            insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "minegame159/meteorclient/events/render/GetFovEvent", "get", "(D)Lminegame159/meteorclient/events/render/GetFovEvent;"));
            insns.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "meteordevelopment/orbit/IEventBus", "post", "(Ljava/lang/Object;)Ljava/lang/Object;"));
            insns.add(new TypeInsnNode(Opcodes.CHECKCAST, "minegame159/meteorclient/events/render/GetFovEvent"));
            insns.add(new FieldInsnNode(Opcodes.GETFIELD, "minegame159/meteorclient/events/render/GetFovEvent", "fov", "D"));

            method.instructions.insert(targetInsn, insns);
            method.instructions.remove(targetInsn);
        }
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    private MethodNode getMethod(ClassNode classNode, String desc) {
        for (MethodNode method : classNode.methods) {
            if (method.desc.equals(desc)) return method;
        }

        return null;
    }
}
