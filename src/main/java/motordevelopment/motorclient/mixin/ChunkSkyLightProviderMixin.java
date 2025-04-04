/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.mixin;

import motordevelopment.motorclient.systems.modules.Modules;
import motordevelopment.motorclient.systems.modules.render.NoRender;
import net.minecraft.world.chunk.light.ChunkSkyLightProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkSkyLightProvider.class)
public abstract class ChunkSkyLightProviderMixin {
    @Inject(at = @At("HEAD"), method = "method_51531", cancellable = true)
    private void recalculateLevel(long blockPos, long l, int lightLevel, CallbackInfo ci) {
        if (Modules.get().get(NoRender.class).noSkylightUpdates()) ci.cancel();
    }
}
