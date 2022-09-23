/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin.indigo;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Xray;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.TerrainRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TerrainRenderContext.class, remap = false)
public class TerrainRenderContextMixin {
    @Inject(method = "tessellateBlock", at = @At("HEAD"), cancellable = true)
    private void onTessellateBlock(BlockState blockState, BlockPos blockPos, BakedModel model, MatrixStack matrixStack, CallbackInfoReturnable<Boolean> info) {
        Xray xray = Modules.get().get(Xray.class);

        if (xray.isActive() && xray.isBlocked(blockState.getBlock(), blockPos)) {
//            info.cancel();
        }
    }
}
