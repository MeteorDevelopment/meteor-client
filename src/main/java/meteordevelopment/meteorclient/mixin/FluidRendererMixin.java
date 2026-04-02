/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Xray;
import meteordevelopment.meteorclient.systems.modules.world.Ambience;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FluidRenderer.class)
public abstract class FluidRendererMixin {
    @Unique
    private static final ThreadLocal<Integer> ALPHAS = ThreadLocal.withInitial(() -> -1);
    @Unique
    private static final ThreadLocal<Boolean> AMBIENT = ThreadLocal.withInitial(() -> false);

    @Inject(method = "tesselate", at = @At("HEAD"), cancellable = true)
    private void onTesselate(BlockAndTintGetter level, BlockPos pos, FluidRenderer.Output output, BlockState blockState, FluidState fluidState, CallbackInfo ci) {
        Ambience ambience = Modules.get().get(Ambience.class);
        AMBIENT.set(ambience.isActive() && ambience.customLavaColor.get() && fluidState.is(FluidTags.LAVA));

        // Xray and Wallhack
        int alpha = Xray.getAlpha(fluidState.createLegacyBlock(), pos);

        if (alpha == 0) ci.cancel();
        else ALPHAS.set(alpha);
    }

    @Inject(method = "vertex", at = @At("HEAD"), cancellable = true)
    private void onVertex(VertexConsumer builder, float x, float y, float z, int color, float u, float v, int lightCoords, CallbackInfo ci) {
        int alpha = ALPHAS.get();

        if (AMBIENT.get()) {
            Color c = Modules.get().get(Ambience.class).lavaColor.get();
            vertex(builder, x, y, z, c.r, c.g, c.b, (alpha != -1 ? alpha : c.a), u, v, lightCoords);
            ci.cancel();
        } else if (alpha != -1) {
            int red = ARGB.red(color);
            int green = ARGB.green(color);
            int blue = ARGB.blue(color);

            vertex(builder, x, y, z, red * 255, green * 255, blue * 255, alpha, u, v, lightCoords);
            ci.cancel();
        }
    }

    @Unique
    private void vertex(VertexConsumer vertexConsumer, float x, float y, float z, int red, int green, int blue, int alpha, float u, float v, int light) {
        vertexConsumer.addVertex(x, y, z).setColor(red, green, blue, alpha).setUv(u, v).setLight(light).setNormal(0.0f, 1.0f, 0.0f);
    }
}
