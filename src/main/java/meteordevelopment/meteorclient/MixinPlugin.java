/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient;

import meteordevelopment.meteorclient.asm.Asm;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

public class MixinPlugin implements IMixinConfigPlugin {
    private boolean isResourceLoaderPresent = false;
    private boolean isOriginsPresent = false;

    @Override
    public void onLoad(String mixinPackage) {
        try {
            // Get class loader
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Class<?> classLoaderClass = classLoader.getClass();

            // Get delegate
            Field delegateField = classLoaderClass.getDeclaredField("delegate");
            delegateField.setAccessible(true);
            Object delegate = delegateField.get(classLoader);
            Class<?> delegateClass = delegate.getClass();

            // Get mixinTransformer field
            Field mixinTransformerField = delegateClass.getDeclaredField("mixinTransformer");
            mixinTransformerField.setAccessible(true);

            // Get unsafe
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            Unsafe unsafe = (Unsafe) unsafeField.get(null);

            // Create Asm
            Asm asm = new Asm();

            // Change delegate
            Class<?> klass = asm.createTransformer();

            Object mixinTransformer = unsafe.allocateInstance(klass);
            mixinTransformer.getClass().getDeclaredField("delegate").set(mixinTransformer, mixinTransformerField.get(delegate));

            mixinTransformerField.set(delegate, mixinTransformer);
        } catch (NoSuchFieldException | IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        }

        for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            if (mod.getMetadata().getId().startsWith("fabric-resource-loader")) isResourceLoaderPresent = true;
            else if (mod.getMetadata().getId().equals("origins")) isOriginsPresent = true;
        }
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
        else if (mixinClassName.endsWith("PlayerEntityRendererMixin")) {
            return !isOriginsPresent;
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
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
