/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.Ambience;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
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
    private static void onGetWaterColor(BlockAndTintGetter level, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        Ambience ambience = Modules.get().get(Ambience.class);

        if (ambience.isActive() && ambience.customWaterColor.get()) {
            cir.setReturnValue(ambience.waterColor.get().getPacked());
        }
    }

    /**
     * @author Walaryne
     */
    @Inject(method = "getAverageFoliageColor", at = @At("HEAD"), cancellable = true)
    private static void onGetFoliageColor(BlockAndTintGetter level, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        Ambience ambience = Modules.get().get(Ambience.class);

        if (ambience.isActive() && ambience.customFoliageColor.get()) {
            cir.setReturnValue(ambience.foliageColor.get().getPacked());
        }
    }

    /**
     * @author Walaryne
     */
    @Inject(method = "getAverageGrassColor", at = @At("HEAD"), cancellable = true)
    private static void onGetGrassColor(BlockAndTintGetter level, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        Ambience ambience = Modules.get().get(Ambience.class);

        if (ambience.isActive() && ambience.customGrassColor.get()) {
            cir.setReturnValue(ambience.grassColor.get().getPacked());
        }
    }
}
