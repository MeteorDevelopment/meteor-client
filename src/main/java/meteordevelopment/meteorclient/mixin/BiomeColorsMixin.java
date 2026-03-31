/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.Ambience;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BiomeColors.class)
public abstract class BiomeColorsMixin {
    /**
     * @author Walaryne
     */
    @Inject(method = "getAverageWaterColor", at = @At("HEAD"), cancellable = true)
    private static void onGetWaterColor(BlockAndTintGetter world, BlockPos pos, CallbackInfoReturnable<Integer> info) {
        Ambience ambience = Modules.get().get(Ambience.class);

        if (ambience.isActive() && ambience.customWaterColor.get()) {
            info.setReturnValue(ambience.waterColor.get().getPacked());
        }
    }

    /**
     * @author Walaryne
     */
    @Inject(method = "getAverageFoliageColor", at = @At("HEAD"), cancellable = true)
    private static void onGetFoliageColor(BlockAndTintGetter world, BlockPos pos, CallbackInfoReturnable<Integer> info) {
        Ambience ambience = Modules.get().get(Ambience.class);

        if (ambience.isActive() && ambience.customFoliageColor.get()) {
            info.setReturnValue(ambience.foliageColor.get().getPacked());
        }
    }

    /**
     * @author Walaryne
     */
    @Inject(method = "getAverageGrassColor", at = @At("HEAD"), cancellable = true)
    private static void onGetGrassColor(BlockAndTintGetter world, BlockPos pos, CallbackInfoReturnable<Integer> info) {
        Ambience ambience = Modules.get().get(Ambience.class);

        if (ambience.isActive() && ambience.customGrassColor.get()) {
            info.setReturnValue(ambience.grassColor.get().getPacked());
        }
    }
}
