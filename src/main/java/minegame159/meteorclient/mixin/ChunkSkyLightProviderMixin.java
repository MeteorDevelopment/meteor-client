/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.mixin;

import minegame159.meteorclient.systems.modules.Modules;
import minegame159.meteorclient.systems.modules.render.NoRender;
import net.minecraft.world.chunk.light.ChunkSkyLightProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkSkyLightProvider.class)
public class ChunkSkyLightProviderMixin {
    @Inject(at = @At("HEAD"), method = "recalculateLevel", cancellable = true)
    private void recalculateLevel(long long_1, long long_2, int int_1, CallbackInfoReturnable<Integer> ci) {

        if (Modules.get().get(NoRender.class).noSkylightUpdates()) {
            //return;
            ci.setReturnValue(15);
            ci.cancel();
        }
    }
}