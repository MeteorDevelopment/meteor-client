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
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LiquidBlockRenderer.class)
public abstract class LiquidBlockRendererMixin {
    @Unique
    private static final ThreadLocal<Integer> ALPHAS = ThreadLocal.withInitial(() -> -1);
    @Unique
    private static final ThreadLocal<Boolean> AMBIENT = ThreadLocal.withInitial(() -> false);

    @Inject(method = "tesselate", at = @At("HEAD"), cancellable = true)
    private void onRender(BlockAndTintGetter world, BlockPos pos, VertexConsumer vertexConsumer, BlockState blockState, FluidState fluidState, CallbackInfo ci) {
        Ambience ambience = Modules.get().get(Ambience.class);
        AMBIENT.set(ambience.isActive() && ambience.customLavaColor.get() && fluidState.is(FluidTags.LAVA));

        // Xray and Wallhack
        int alpha = Xray.getAlpha(fluidState.createLegacyBlock(), pos);

        if (alpha == 0) ci.cancel();
        else ALPHAS.set(alpha);
    }

    @Inject(method = "vertex", at = @At("HEAD"), cancellable = true)
    private void onVertex(VertexConsumer vertexConsumer, float x, float y, float z, float red, float green, float blue, float u, float v, int light, CallbackInfo ci) {
        int alpha = ALPHAS.get();

        if (AMBIENT.get()) {
            Color color = Modules.get().get(Ambience.class).lavaColor.get();
            vertex(vertexConsumer, x, y, z, color.r, color.g, color.b, (alpha != -1 ? alpha : color.a), u, v, light);
            ci.cancel();
        } else if (alpha != -1) {
            vertex(vertexConsumer, x, y, z, (int) (red * 255), (int) (green * 255), (int) (blue * 255), alpha, u, v, light);
            ci.cancel();
        }
    }

    @Unique
    private void vertex(VertexConsumer vertexConsumer, float x, float y, float z, int red, int green, int blue, int alpha, float u, float v, int light) {
        vertexConsumer.addVertex(x, y, z).setColor(red, green, blue, alpha).setUv(u, v).setLight(light).setNormal(0.0f, 1.0f, 0.0f);
    }
}
