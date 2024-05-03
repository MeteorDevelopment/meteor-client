/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin.sodium;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import me.jellysquid.mods.sodium.client.world.biome.BiomeColorCache;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.Ambience;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BiomeColorCache.class, remap = false)
public abstract class SodiumBiomeColorCacheMixin {
    @Unique
    private Ambience ambience;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo info) {
        ambience = Modules.get().get(Ambience.class);
    }

    @ModifyExpressionValue(method = "getColor(Lme/jellysquid/mods/sodium/client/world/biome/BiomeColorSource;III)I", at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/world/biome/BiomeColorCache;getColor(Lnet/minecraft/world/biome/ColorResolver;III)I", ordinal = 0, remap = true))
    private int modify_getGrassColorAt(int color) {
        return ambience.isActive() && ambience.customGrassColor.get() ? ambience.grassColor.get().getPacked() : color;
    }

    @ModifyExpressionValue(method = "getColor(Lme/jellysquid/mods/sodium/client/world/biome/BiomeColorSource;III)I", at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/world/biome/BiomeColorCache;getColor(Lnet/minecraft/world/biome/ColorResolver;III)I", ordinal = 1, remap = true))
    private int modify_getFoliageColor(int color) {
        return ambience.isActive() && ambience.customFoliageColor.get() ? ambience.foliageColor.get().getPacked() : color;
    }

    @ModifyExpressionValue(method = "getColor(Lme/jellysquid/mods/sodium/client/world/biome/BiomeColorSource;III)I", at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/world/biome/BiomeColorCache;getColor(Lnet/minecraft/world/biome/ColorResolver;III)I", ordinal = 2, remap = true))
    private int modify_getWaterColor(int color) {
        return ambience.isActive() && ambience.customWaterColor.get() ? ambience.waterColor.get().getPacked() : color;
    }
}
