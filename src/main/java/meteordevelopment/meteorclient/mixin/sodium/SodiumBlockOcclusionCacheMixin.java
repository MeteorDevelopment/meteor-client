/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin.sodium;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Xray;
import net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AbstractBlockRenderContext.class, remap = false)
public abstract class SodiumBlockOcclusionCacheMixin {

    @Shadow protected BlockState state;
    @Shadow protected BlockPos pos; 
    @Shadow protected LevelSlice slice;

    @Unique private Xray xray;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo info) {
        xray = Modules.get().get(Xray.class);
    }
    // For More Culling compatibility - runs before More Culling's inject to force-render whitelisted Xray blocks
    @Inject(method = "shouldDrawSide", at = @At("HEAD"), cancellable = true)
    private void meteor$forceXrayFace(Direction facing, CallbackInfoReturnable<Boolean> cir) {
        if (xray != null && xray.isActive() && !xray.isBlocked(state.getBlock(), null)) {
            cir.setReturnValue(true);
        }
    }

    @ModifyReturnValue(method = "shouldDrawSide", at = @At("RETURN"))
    private boolean shouldDrawSide(boolean original, Direction facing) {
        if (!xray.isActive()) return original;
        return xray.modifyDrawSide(state, slice, pos, facing, original);
    }
}
