/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin.sodium;

import me.jellysquid.mods.sodium.client.model.light.LightPipeline;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.model.quad.blender.ColorSampler;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.FluidRenderer;
import me.jellysquid.mods.sodium.client.util.color.ColorABGR;
import me.jellysquid.mods.sodium.client.util.color.ColorARGB;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Xray;
import meteordevelopment.meteorclient.systems.modules.world.Ambience;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;

@Mixin(value = FluidRenderer.class, remap = false)
public class SodiumFluidRendererMixin {
    @Final @Shadow private int[] quadColors;

    @Unique private final ThreadLocal<Integer> alphas = new ThreadLocal<>();

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(BlockRenderView world, FluidState fluidState, BlockPos pos, BlockPos offset, ChunkModelBuilder buffers, CallbackInfoReturnable<Boolean> info) {
        int alpha = Xray.getAlpha(fluidState.getBlockState(), pos);

        if (alpha == 0) info.setReturnValue(false);
        else alphas.set(alpha);
    }

    /**
     * @author Walaryne
     */
    @Inject(method = "updateQuad", at = @At("TAIL"))
    private void onUpdateQuad(ModelQuadView quad, BlockRenderView world, BlockPos pos, LightPipeline lighter, Direction dir, float brightness, ColorSampler<FluidState> colorSampler, FluidState fluidState, CallbackInfo info) {
        // Ambience
        Ambience ambience = Modules.get().get(Ambience.class);

        if (ambience.isActive() && ambience.customLavaColor.get() && fluidState.isIn(FluidTags.LAVA)) {
            Arrays.fill(quadColors, ColorARGB.toABGR(ambience.lavaColor.get().getPacked()));
        }
        else {
            // XRay and Wallhack
            int alpha = alphas.get();

            for (int i = 0; i < quadColors.length; i++) {
                quadColors[i] = ColorABGR.withAlpha(quadColors[i], alpha / 255f);
            }
        }
    }
}
