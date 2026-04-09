/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.vertex.QuadInstance;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Xray;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ModelBlockRenderer.class)
public abstract class ModelBlockRendererMixin {
    @Shadow
    @Final
    private QuadInstance quadInstance;

    @Unique
    private static final ThreadLocal<Integer> ALPHAS = ThreadLocal.withInitial(() -> -1);

    @Inject(method = {"tesselateFlat", "tesselateAmbientOcclusion"}, at = @At("HEAD"), cancellable = true)
    private void tesselate$xray(BlockQuadOutput output, float x, float y, float z, List<BlockStateModelPart> parts, BlockAndTintGetter level, BlockState state, BlockPos pos, CallbackInfo ci) {
        int alpha = Xray.getAlpha(state, pos);

        if (alpha == 0) ci.cancel();
        else ALPHAS.set(alpha);
    }

    @Inject(method = "putQuadWithTint", at = @At("HEAD"))
    private void putQuadWithTint$xray(BlockQuadOutput output, float x, float y, float z, BlockAndTintGetter level, BlockState state, BlockPos pos, BakedQuad quad, CallbackInfo ci) {
        int alpha = ALPHAS.get();

        if (alpha != -1) {
            quadInstance.multiplyColor(ARGB.color(alpha, 255, 255, 255));
        }
    }

    @ModifyReturnValue(method = "shouldRenderFace", at = @At("RETURN"))
    private static boolean shouldRenderFace$xray(boolean original, BlockAndTintGetter level, BlockState state, Direction direction, BlockPos neighborPos) {
        Xray xray = Modules.get().get(Xray.class);

        if (xray.isActive()) {
            return xray.modifyDrawSide(state, level, neighborPos.relative(direction.getOpposite()), direction, original);
        }

        return original;
    }
}
