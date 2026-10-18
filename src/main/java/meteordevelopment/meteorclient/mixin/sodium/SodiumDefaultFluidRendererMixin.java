/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin.sodium;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
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
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
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
    @Unique
    private boolean forceXrayFluidSides;
    @Unique
    private Xray xray;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        xray = Modules.get().get(Xray.class);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(LevelSlice level, BlockState blockState, FluidState fluidState, BlockPos blockPos, BlockPos offset, TranslucentGeometryCollector collector, ChunkModelBuilder meshBuilder, Material material, ColorProvider<FluidState> colorProvider, FluidModel sprites, CallbackInfo ci) {
        xrayAlpha = Xray.getFluidAlpha(fluidState, blockPos);
        forceXrayFluidSides = xray.isActive();

        // Cancel block rendering when alpha is 0, required for Iris support but unnecessary to check for shaders, we already force be disabled when Xray is enabled
        if (xrayAlpha == 0) {
            forceXrayFluidSides = false;
            ci.cancel();
        }
    }

    @ModifyReturnValue(method = "isFullBlockFluidSideVisible", at = @At("RETURN"))
    private boolean onIsFullBlockFluidSideVisible(boolean original, BlockGetter view, BlockPos selfPos, Direction facing, FluidState fluid) {
        if (original || !forceXrayFluidSides || facing.getAxis().isVertical()) return original;
        // Sodium 0.9.2 for 26.3 no longer exposes the neighbor state as a local variable here,
        // so we query it from the BlockGetter ourselves. Semantics are identical.
        BlockState otherState = view.getBlockState(selfPos.relative(facing));
        if (!xray.isBlocked(otherState.getBlock(), null)) return false;

        return !otherState.getFluidState().getType().isSame(fluid.getType());
    }

    // In Sodium 0.9.2 (MC 26.3) the old 4-param isFluidSideExposed(BlockState, BlockState, Direction, float)
    // is inlined into its 5-param entrypoint, so we can no longer inject on it. Target the public entrypoint instead
    // and re-derive the neighbor state from BlockAndTintGetter + pos.
    @ModifyReturnValue(method = "isFluidSideExposed(Lnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;F)Z", at = @At("RETURN"))
    private boolean onIsFluidSideExposed(boolean original, BlockAndTintGetter level, BlockState ownBlockState, BlockPos pos, Direction facing, float height) {
        if (original || !forceXrayFluidSides || facing.getAxis().isVertical()) return original;
        BlockState neighborBlockState = level.getBlockState(pos.relative(facing));
        if (!xray.isBlocked(neighborBlockState.getBlock(), null)) return false;

        return !neighborBlockState.getFluidState().getType().isSame(ownBlockState.getFluidState().getType());
    }

    @Inject(method = "getUpFaceExposureByNeighbors", at = @At("HEAD"), cancellable = true)
    private void onGetUpFaceExposureByNeighbors(BlockAndTintGetter level, BlockPos origin, FluidState fluidState, CallbackInfoReturnable<Integer> cir) {
        if (forceXrayFluidSides) cir.setReturnValue(3);
    }

    @Inject(method = "updateQuad", at = @At("TAIL"))
    private void onUpdateQuad(ModelQuadViewMutable quad, LevelSlice level, BlockPos pos, LightPipeline lighter, Direction dir, ModelQuadFacing facing, float brightness, ColorProvider<FluidState> colorProvider, FluidState fluidState, CallbackInfo ci) {
        if (xrayAlpha != -1) {
            for (int i = 0; i < 4; i++) {
                quadColors[i] = (quadColors[i] & 0x00FFFFFF) | (xrayAlpha << 24);
            }
        }
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/pipeline/DefaultFluidRenderer;writeQuad(Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/buffers/ChunkModelBuilder;Lnet/caffeinemc/mods/sodium/client/render/chunk/translucent_sorting/TranslucentGeometryCollector;Lnet/caffeinemc/mods/sodium/client/render/chunk/terrain/material/Material;Lnet/minecraft/core/BlockPos;Lnet/caffeinemc/mods/sodium/client/model/quad/ModelQuadView;Lnet/caffeinemc/mods/sodium/client/model/quad/properties/ModelQuadFacing;Z)V"), index = 2)
    private Material modifyMaterial(Material material) {
        if (xrayAlpha != -1) {
            return DefaultMaterials.TRANSLUCENT;
        }
        return material;
    }
}
