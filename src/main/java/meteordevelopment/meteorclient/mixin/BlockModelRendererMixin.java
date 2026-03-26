/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Xray;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.List;

@Mixin(ModelBlockRenderer.class)
public abstract class BlockModelRendererMixin {
    @Unique
    private final ThreadLocal<Integer> alphas = new ThreadLocal<>();

    @Inject(method = {"tesselateAmbientOcclusion", "tesselateFlat"}, at = @At("HEAD"), cancellable = true)
    private void onRenderSmooth(BlockQuadOutput output, float red, float green, float blue, List<BlockStateModelPart> parts, BlockAndTintGetter world, BlockState state, BlockPos pos, CallbackInfo ci) {
        int alpha = Xray.getAlpha(state, pos);

        if (alpha == 0) ci.cancel();
        else alphas.set(alpha);
    }

    @ModifyArgs(method = "putQuadWithTint", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/BlockQuadOutput;put(FFFLnet/minecraft/client/resources/model/geometry/BakedQuad;Lcom/mojang/blaze3d/vertex/QuadInstance;)V"))
    private void modifyXrayAlpha(final Args args) {
        final int alpha = alphas.get();
        if (alpha == -1) return;

        QuadInstance quad = args.get(4);
        for (int i = 0; i < 4; i++) {
            int color = quad.getColor(i);
            quad.setColor(i, (alpha << 24) | (color & 0x00FFFFFF));
        }
    }

    @ModifyReturnValue(method = "shouldRenderFace", at = @At("RETURN"))
    private static boolean modifyShouldDrawFace(boolean original, BlockAndTintGetter world, BlockState state, Direction side, BlockPos pos) {
        Xray xray = Modules.get().get(Xray.class);

        if (xray.isActive()) {
            return xray.modifyDrawSide(state, world, pos.relative(side.getOpposite()), side, original); // thanks mojang
        }

        return original;
    }
}
