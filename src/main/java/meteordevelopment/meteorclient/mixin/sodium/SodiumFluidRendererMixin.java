/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.mixin.sodium;

import me.jellysquid.mods.sodium.client.model.light.LightPipeline;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.model.quad.blender.ColorSampler;
import me.jellysquid.mods.sodium.client.render.pipeline.FluidRenderer;
import me.jellysquid.mods.sodium.client.util.color.ColorARGB;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.Ambience;
import net.minecraft.fluid.FluidState;
import net.minecraft.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;

@Mixin(value = FluidRenderer.class, remap = false)
public class SodiumFluidRendererMixin {
    @Final @Shadow private int[] quadColors;

    /**
     * @author Walaryne
     */
    @Inject(method = "calculateQuadColors", at = @At("TAIL"), remap = false)
    private void onCalculateQuadColors(ModelQuadView quad, BlockRenderView world, BlockPos pos, LightPipeline lighter, Direction dir, float brightness, ColorSampler<FluidState> colorSampler, FluidState fluidState, CallbackInfo info) {
        Ambience ambience = Modules.get().get(Ambience.class);

        if (ambience.isActive() && ambience.customLavaColor.get() && fluidState.isIn(FluidTags.LAVA)) {
            Arrays.fill(quadColors, ColorARGB.toABGR(ambience.lavaColor.get().getPacked()));
        }
    }
}
