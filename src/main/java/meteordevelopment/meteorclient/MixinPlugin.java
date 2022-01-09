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
    private static final String mixinPackage = "meteordevelopment.meteorclient.mixin";

    private static boolean loaded;

    private static boolean isOriginsPresent;
    private static boolean isSodiumPresent;
    private static boolean isCanvasPresent;

    @Override
    public void onLoad(String mixinPackage) {
        if (loaded) return;

        isOriginsPresent = FabricLoader.getInstance().isModLoaded("origins");
        isSodiumPresent = FabricLoader.getInstance().isModLoaded("sodium");
        isCanvasPresent = FabricLoader.getInstance().isModLoaded("canvas");

        loaded = true;
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!mixinClassName.startsWith(mixinPackage)) {
            throw new RuntimeException("Mixin " + mixinClassName + " is not in the mixin package");
        }
        else if (mixinClassName.endsWith("PlayerEntityRendererMixin")) {
            return !isOriginsPresent;
        }
        else if (mixinClassName.startsWith(mixinPackage + ".sodium")) {
            return isSodiumPresent;
        }
        else if (mixinClassName.startsWith(mixinPackage + ".canvas")) {
            return isCanvasPresent;
        }


        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
