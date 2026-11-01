/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class MixinPlugin implements IMixinConfigPlugin {
    private static final String mixinPackage = "meteordevelopment.meteorclient.mixin";

    private static boolean loaded;

    private static boolean isOriginsPresent;
    private static boolean isIndigoPresent;
    public static boolean isSodiumPresent;
    private static boolean isLithiumPresent;
    public static boolean isIrisPresent;
    private static boolean isBaritonePresent;
    private static boolean isMetallumPresent;

    private static boolean isModernSodium;

    @Override
    public void onLoad(String mixinPackage) {
        if (loaded) return;

        isIndigoPresent = FabricLoader.getInstance().isModLoaded("fabric-renderer-indigo");
        isOriginsPresent = FabricLoader.getInstance().isModLoaded("origins");
        isSodiumPresent = FabricLoader.getInstance().isModLoaded("sodium");
        isLithiumPresent = FabricLoader.getInstance().isModLoaded("lithium");
        isIrisPresent = FabricLoader.getInstance().isModLoaded("iris");
        isBaritonePresent = FabricLoader.getInstance().isModLoaded("baritone") || FabricLoader.getInstance().isModLoaded("baritone-meteor");
        isMetallumPresent = FabricLoader.getInstance().isModLoaded("metallum");

        isModernSodium = detectModernSodium();

        loaded = true;
    }

    private static boolean detectModernSodium() {
        var sodium = FabricLoader.getInstance().getModContainer("sodium");
        if (sodium.isEmpty()) {
            return false;
        }

        String version = sodium.get().getMetadata().getVersion().getFriendlyString();

        String versionMatch = version.replaceFirst(".*?(0\\.\\d+\\.\\d+).*", "$1");

        if (versionMatch.isEmpty() || versionMatch.equals(version)) {
            String normalized = version.toLowerCase();
            int idx = normalized.indexOf("0.");
            if (idx >= 0) {
                versionMatch = normalized.substring(idx);
                int end = versionMatch.indexOf('-');
                if (end >= 0) versionMatch = versionMatch.substring(0, end);
            }
        }

        if (versionMatch.isEmpty()) {
            return false;
        }

        String[] parts = versionMatch.split("\\.");
        if (parts.length < 2) {
            return false;
        }

        try {
            int major = Integer.parseInt(parts[0]);
            int minor = Integer.parseInt(parts[1]);
            int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;

            // 0.9.2 and below -> old
            // 0.9.3 and above -> new
            if (major == 0 && minor == 9) {
                return patch >= 3;
            }

            return major > 0 || minor > 9;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!mixinClassName.startsWith(mixinPackage)) {
            throw new RuntimeException("Mixin " + mixinClassName + " is not in the mixin package");
        } else if (mixinClassName.endsWith("PlayerEntityRendererMixin")) {
            return !isOriginsPresent;
        } else if (mixinClassName.startsWith(mixinPackage + ".sodium")) {
            if (!isSodiumPresent) return false;

            if (mixinClassName.endsWith("SodiumDefaultFluidRendererOldMixin")) {
                return !isModernSodium;
            }

            if (mixinClassName.endsWith("SodiumDefaultFluidRendererNewMixin")) {
                return isModernSodium;
            }

            return true;
        } else if (mixinClassName.startsWith(mixinPackage + ".indigo")) {
            return isIndigoPresent;
        } else if (mixinClassName.startsWith(mixinPackage + ".lithium")) {
            return isLithiumPresent;
        } else if (mixinClassName.startsWith(mixinPackage + ".baritone")) {
            return isBaritonePresent;
        } else if (mixinClassName.startsWith(mixinPackage + ".metallum")) {
            return isMetallumPresent;
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
