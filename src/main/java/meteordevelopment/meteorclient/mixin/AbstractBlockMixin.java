/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.AmbientOcclusionEvent;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.TextureRotations;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractBlock.class)
public class AbstractBlockMixin {
    @Inject(method = "getAmbientOcclusionLightLevel", at = @At("HEAD"), cancellable = true)
    private void onGetAmbientOcclusionLightLevel(BlockState state, BlockView world, BlockPos pos, CallbackInfoReturnable<Float> info) {
        AmbientOcclusionEvent event = MeteorClient.EVENT_BUS.post(AmbientOcclusionEvent.get());

        if (event.lightLevel != -1) info.setReturnValue(event.lightLevel);
    }

    @Inject(method = "getRenderingSeed", at = @At("HEAD"), cancellable = true)
    private void onGetRenderingSeed(BlockState state, BlockPos pos, CallbackInfoReturnable<Long> info) {
        if(Modules.get().isActive(TextureRotations.class)) {
            long l = (long)pos.getX() * 2460155L ^ (long)pos.getZ() * 15578214L ^ (long)pos.getY();
            l = l * l * System.currentTimeMillis() + l * 11L;

            info.setReturnValue(l >> 16);
        }
    }
}
