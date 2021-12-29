/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class MixinPlugin implements IMixinConfigPlugin {
    private static final String MIXIN_PREFIX = "meteordevelopment.meteorclient.mixin.";
    private boolean isOriginsPresent = false;
    private boolean isSodiumPresent = false;
    private boolean isCanvasPresent = false;

    @Override
    public void onLoad(String mixinPackage) {
        for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            if (mod.getMetadata().getId().equals("origins")) isOriginsPresent = true;
            else if (mod.getMetadata().getId().equals("sodium")) isSodiumPresent = true;
            else if (mod.getMetadata().getId().equals("canvas")) isCanvasPresent = true;
        }
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!mixinClassName.startsWith(MIXIN_PREFIX)) throw new IllegalStateException("Mixin " + mixinClassName + " is not in the mixin package. This shouldn't happen!");
        if (mixinClassName.endsWith("PlayerEntityRendererMixin"))
            return !isOriginsPresent;
        if (mixinClassName.startsWith(MIXIN_PREFIX + "canvas"))
            return isCanvasPresent;
        if (mixinClassName.startsWith(MIXIN_PREFIX + "sodium"))
            return isSodiumPresent;

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
