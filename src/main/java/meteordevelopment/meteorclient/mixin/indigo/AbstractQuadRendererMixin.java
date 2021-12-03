/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.mixin.indigo;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.WallHack;
import meteordevelopment.meteorclient.systems.modules.render.Xray;
import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.MutableQuadViewImpl;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.AbstractQuadRenderer;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.BlockRenderInfo;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Matrix3f;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Function;

@Mixin(value = AbstractQuadRenderer.class, remap = false)
public abstract class AbstractQuadRendererMixin {
    @Final @Shadow protected BlockRenderInfo blockInfo;
    @Final @Shadow protected Function<RenderLayer, VertexConsumer> bufferFunc;
    @Final @Shadow protected Vec3f normalVec;

    @Shadow protected abstract Matrix3f normalMatrix();
    @Shadow protected abstract Matrix4f matrix();
    @Shadow protected abstract int overlay();

    @SuppressWarnings("UnresolvedMixinReference")
    @Inject(method = "bufferQuad(Lnet/fabricmc/fabric/impl/client/indigo/renderer/mesh/MutableQuadViewImpl;Lnet/minecraft/class_1921;)V",
    at = @At("HEAD"), cancellable = true, remap = false)
    private void onBufferQuad(MutableQuadViewImpl quad, RenderLayer renderLayer, CallbackInfo ci) {
        WallHack wallHack = Modules.get().get(WallHack.class);
        Xray xray = Modules.get().get(Xray.class);

        if(wallHack.isActive() && wallHack.blocks.get().contains(blockInfo.blockState.getBlock())) {
            int alpha;

            if(xray.isActive()) {
                alpha = xray.opacity.get();
            } else {
                alpha = wallHack.opacity.get();
            }

            whBufferQuad(bufferFunc.apply(renderLayer), quad, matrix(), overlay(), normalMatrix(), normalVec, alpha);
            ci.cancel();
        } else if(xray.isActive() && !wallHack.isActive() && xray.isBlocked(blockInfo.blockState.getBlock(), blockInfo.blockPos)) {
            whBufferQuad(bufferFunc.apply(renderLayer), quad, matrix(), overlay(), normalMatrix(), normalVec, xray.opacity.get());
            ci.cancel();
        }
    }

    //https://github.com/FabricMC/fabric/blob/351679a7decdd3044d778e74001de67463bee205/fabric-renderer-indigo/src/main/java/net/fabricmc/fabric/impl/client/indigo/renderer/render/AbstractQuadRenderer.java#L86
    //Again, nasty problem with mixins and for loops, hopefully I can fix this at a later date - Wala
    private static void whBufferQuad(VertexConsumer buff, MutableQuadViewImpl quad, Matrix4f matrix, int overlay, Matrix3f normalMatrix, Vec3f normalVec, int alpha) {
        final boolean useNormals = quad.hasVertexNormals();

        if (useNormals) {
            quad.populateMissingNormals();
        } else {
            final Vec3f faceNormal = quad.faceNormal();
            normalVec.set(faceNormal.getX(), faceNormal.getY(), faceNormal.getZ());
            normalVec.transform(normalMatrix);
        }

        for (int i = 0; i < 4; i++) {
            buff.vertex(matrix, quad.x(i), quad.y(i), quad.z(i));
            final int color = quad.spriteColor(i, 0);
            buff.color(color & 0xFF, (color >> 8) & 0xFF, (color >> 16) & 0xFF, alpha);
            buff.texture(quad.spriteU(i, 0), quad.spriteV(i, 0));
            buff.overlay(overlay);
            buff.light(quad.lightmap(i));

            if (useNormals) {
                normalVec.set(quad.normalX(i), quad.normalY(i), quad.normalZ(i));
                normalVec.transform(normalMatrix);
            }

            buff.normal(normalVec.getX(), normalVec.getY(), normalVec.getZ());
            buff.next();
        }
    }
}
