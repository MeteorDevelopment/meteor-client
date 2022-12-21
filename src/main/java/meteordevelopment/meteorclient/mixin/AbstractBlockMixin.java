/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.AmbientOcclusionEvent;
import meteordevelopment.meteorclient.mixininterface.IAbstractBlock;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.TextureRotations;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.security.SecureRandom;

@Mixin(AbstractBlock.class)
public class AbstractBlockMixin implements IAbstractBlock {
    @Unique private long modifier = 0;

    @Inject(method = "getAmbientOcclusionLightLevel", at = @At("HEAD"), cancellable = true)
    private void onGetAmbientOcclusionLightLevel(BlockState state, BlockView world, BlockPos pos, CallbackInfoReturnable<Float> info) {
        AmbientOcclusionEvent event = MeteorClient.EVENT_BUS.post(AmbientOcclusionEvent.get());

        if (event.lightLevel != -1) info.setReturnValue(event.lightLevel);
    }

    @Inject(method = "getRenderingSeed", at = @At("HEAD"), cancellable = true)
    private void onGetRenderingSeed(BlockState state, BlockPos pos, CallbackInfoReturnable<Long> info) {
        if(Modules.get().isActive(TextureRotations.class)) info.setReturnValue(getSeed(pos));
    }

    @Override
    public long getSeed(BlockPos pos) {
        modifier = modifier == 0 ? new SecureRandom().nextLong() : modifier;

        long l = (long)pos.getX() * 2460155L ^ (long)pos.getZ() * 15578214L ^ (long)pos.getY();
        l = l * l * modifier + l * 11L;

        return (l >> 16);
    }
}
