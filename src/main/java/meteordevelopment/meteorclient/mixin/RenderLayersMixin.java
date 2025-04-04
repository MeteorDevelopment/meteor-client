/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Xray;
import meteordevelopment.meteorclient.systems.modules.world.Ambience;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.fluid.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderLayers.class)
public abstract class RenderLayersMixin {
    @Inject(method = "getBlockLayer", at = @At("HEAD"), cancellable = true)
    private static void onGetBlockLayer(BlockState state, CallbackInfoReturnable<RenderLayer> cir) {
        if (Modules.get() == null) return;

        int alpha = Xray.getAlpha(state, null);
        if (alpha > 0 && alpha < 255) cir.setReturnValue(RenderLayer.getTranslucent());
    }

    @Inject(method = "getFluidLayer", at = @At("HEAD"), cancellable = true)
    private static void onGetFluidLayer(FluidState state, CallbackInfoReturnable<RenderLayer> cir) {
        if (Modules.get() == null) return;

        int alpha = Xray.getAlpha(state.getBlockState(), null);
        if (alpha > 0 && alpha < 255) {
            cir.setReturnValue(RenderLayer.getTranslucent());
        }

        else {
            Ambience ambience = Modules.get().get(Ambience.class);
            int a = ambience.lavaColor.get().a;
            if (ambience.isActive() && ambience.customLavaColor.get() && a > 0 && a < 255) {
                cir.setReturnValue(RenderLayer.getTranslucent());
            }
        }
    }
}
