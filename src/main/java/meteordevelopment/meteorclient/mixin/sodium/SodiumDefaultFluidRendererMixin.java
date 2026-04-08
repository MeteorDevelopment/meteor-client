/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin.sodium;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Xray;
import net.caffeinemc.mods.sodium.client.model.color.ColorProvider;
import net.caffeinemc.mods.sodium.client.model.light.LightPipeline;
import net.caffeinemc.mods.sodium.client.model.quad.ModelQuadViewMutable;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.DefaultFluidRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.DefaultMaterials;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.TranslucentGeometryCollector;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.block.BlockState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.BlockView;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = DefaultFluidRenderer.class, remap = false)
public abstract class SodiumDefaultFluidRendererMixin {
    @Final
    @Shadow
    private int[] quadColors;

    @Unique
    private int xrayAlpha;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(LevelSlice level, BlockState blockState, FluidState fluidState, BlockPos blockPos, BlockPos offset, TranslucentGeometryCollector collector, ChunkModelBuilder meshBuilder, Material material, ColorProvider<FluidState> colorProvider, Sprite[] sprites, CallbackInfo ci) {
        xrayAlpha = Xray.getAlpha(fluidState.getBlockState(), blockPos);

        // Cancel block rendering when alpha is 0, required for Iris support but unnecessary to check for shaders, we already force be disabled when Xray is enabled
        if (xrayAlpha == 0) {
            ci.cancel();
        }
    }
    
    @Inject(method = "isFullBlockFluidSideVisible", at = @At("HEAD"), cancellable = true)
    private void meteor$forceFullBlockFluidSideVisible(BlockView view, BlockPos pos, Direction dir, FluidState fluid, CallbackInfoReturnable<Boolean> cir) {
        Xray xray = Modules.get().get(Xray.class);
            if (!xray.isActive()) return;
 
        BlockState state = view.getBlockState(pos);
            if (!xray.isBlocked(state.getBlock(), null)) {
                BlockPos neighborPos = pos.offset(dir);
                BlockState neighborState = view.getBlockState(neighborPos);
                    cir.setReturnValue(!neighborState.getFluidState().isOf(fluid.getFluid()));
        }
    }
    
    @Inject(method = "isFluidSideExposed", at = @At("HEAD"), cancellable = true)
    private void meteor$forceFluidSideExposed(BlockRenderView world, BlockState ownBlockState, BlockPos neighborPos, Direction dir, float height, CallbackInfoReturnable<Boolean> cir) {
        Xray xray = Modules.get().get(Xray.class);
            if (!xray.isActive()) return;

            if (!xray.isBlocked(ownBlockState.getBlock(), null)) {
                BlockState neighborState = world.getBlockState(neighborPos);
                    cir.setReturnValue(!neighborState.getFluidState().isOf(ownBlockState.getFluidState().getFluid()));
        }
    }
 
    @Inject(method = "getUpFaceExposureByNeighbors", at = @At("HEAD"), cancellable = true)
    private void meteor$forceUpFaceExposed(BlockRenderView level, BlockPos pos, FluidState fluidState, CallbackInfoReturnable<Integer> cir) {
        Xray xray = Modules.get().get(Xray.class);
            if (!xray.isActive()) return;
                BlockState state = level.getBlockState(pos);
        
            if (!xray.isBlocked(state.getBlock(), null)) {
                cir.setReturnValue(3);
        }
    }
    
    @Inject(method = "updateQuad", at = @At("TAIL"))
    private void onUpdateQuad(ModelQuadViewMutable quad, LevelSlice level, BlockPos pos, LightPipeline lighter, Direction dir, ModelQuadFacing facing, float brightness, ColorProvider<FluidState> colorProvider, FluidState fluidState, CallbackInfo ci) {
        if (xrayAlpha != -1) {
            for (int i = 0; i < 4; i++) {
                quadColors[i] = (quadColors[i] & 0x00FFFFFF) | (xrayAlpha << 24);
            }
        }
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/pipeline/DefaultFluidRenderer;writeQuad(Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/buffers/ChunkModelBuilder;Lnet/caffeinemc/mods/sodium/client/render/chunk/translucent_sorting/TranslucentGeometryCollector;Lnet/caffeinemc/mods/sodium/client/render/chunk/terrain/material/Material;Lnet/minecraft/util/math/BlockPos;Lnet/caffeinemc/mods/sodium/client/model/quad/ModelQuadView;Lnet/caffeinemc/mods/sodium/client/model/quad/properties/ModelQuadFacing;Z)V"), index = 2)
    private Material modifyMaterial(Material material) {
        if (xrayAlpha != -1) {
            return DefaultMaterials.TRANSLUCENT;
        }
        return material;
    }
}
