/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Xray;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelBlockRenderer.class)
public class BlockRenderLayersMixin {
    private final ThreadLocal<BlockState> meteor$state = new ThreadLocal<>();
    private final ThreadLocal<BlockPos> meteor$pos = new ThreadLocal<>();

    @Inject(method = "putQuadWithTint", at = @At("HEAD"))
    private void meteor$captureBlockContext(BlockQuadOutput output, float x, float y, float z, BlockAndTintGetter world, BlockState state, BlockPos pos, BakedQuad quad, CallbackInfo info) {
        meteor$state.set(state);
        meteor$pos.set(pos);
    }

    @Inject(method = "putQuadWithTint", at = @At("RETURN"))
    private void meteor$clearBlockContext(BlockQuadOutput output, float x, float y, float z, BlockAndTintGetter world, BlockState state, BlockPos pos, BakedQuad quad, CallbackInfo info) {
        meteor$state.remove();
        meteor$pos.remove();
    }

    @ModifyArg(
        method = "putQuadWithTint",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/block/BlockQuadOutput;put(FFFLnet/minecraft/client/resources/model/geometry/BakedQuad;Lcom/mojang/blaze3d/vertex/QuadInstance;)V"
        ),
        index = 3
    )
    private BakedQuad onPutBlockQuad(BakedQuad quad) {
        if (Modules.get() == null) return quad;

        BlockState state = meteor$state.get();
        BlockPos pos = meteor$pos.get();
        if (state == null) return quad;

        int alpha = Xray.getAlpha(state, pos);
        if (alpha <= 0 || alpha >= 255 || quad.materialInfo().layer() == ChunkSectionLayer.TRANSLUCENT) return quad;

        BakedQuad.MaterialInfo materialInfo = quad.materialInfo();
        BakedQuad.MaterialInfo translucentMaterial = new BakedQuad.MaterialInfo(
            materialInfo.sprite(),
            ChunkSectionLayer.TRANSLUCENT,
            materialInfo.itemRenderType(),
            materialInfo.tintIndex(),
            materialInfo.shade(),
            materialInfo.lightEmission()
        );

        return new BakedQuad(
            quad.position0(),
            quad.position1(),
            quad.position2(),
            quad.position3(),
            quad.packedUV0(),
            quad.packedUV1(),
            quad.packedUV2(),
            quad.packedUV3(),
            quad.direction(),
            translucentMaterial
        );
    }
}
