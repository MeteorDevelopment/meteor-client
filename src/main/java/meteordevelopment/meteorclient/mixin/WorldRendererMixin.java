/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.*;
import meteordevelopment.meteorclient.systems.modules.world.Ambience;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.EntityShaders;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.block.BlockState;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
    @Shadow protected abstract void renderEntity(Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers);

    @Shadow private Framebuffer entityOutlinesFramebuffer;

    @Inject(method = "checkEmpty", at = @At("HEAD"), cancellable = true)
    private void onCheckEmpty(MatrixStack matrixStack, CallbackInfo info) {
        info.cancel();
    }

    @Inject(method = "renderWeather", at = @At("HEAD"), cancellable = true)
    private void onRenderWeather(LightmapTextureManager manager, float f, double d, double e, double g, CallbackInfo info) {
        if (Modules.get().get(NoRender.class).noWeather()) info.cancel();
    }

    @Inject(method = "drawBlockOutline", at = @At("HEAD"), cancellable = true)
    private void onDrawHighlightedBlockOutline(MatrixStack matrixStack, VertexConsumer vertexConsumer, Entity entity, double d, double e, double f, BlockPos blockPos, BlockState blockState, CallbackInfo info) {
        if (Modules.get().isActive(BlockSelection.class)) info.cancel();
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;setupTerrain(Lnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/Frustum;ZZ)V"), index = 3)
    private boolean renderSetupTerrainModifyArg(boolean spectator) {
        return Modules.get().isActive(Freecam.class) || spectator;
    }

    // EntityShaders

    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderHead(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f, CallbackInfo info) {
        Utils.minimumLightLevel = Modules.get().get(Fullbright.class).getMinimumLightLevel();

        EntityShaders.beginRender();
    }

    @Inject(method = "renderEntity", at = @At("HEAD"))
    private void renderEntity(Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo info) {
        if (EntityShaders.shouldDrawOverlay(entity) && vertexConsumers != EntityShaders.overlayVertexConsumerProvider  && vertexConsumers != EntityShaders.outlinesVertexConsumerProvider) {
            Framebuffer prevBuffer = this.entityOutlinesFramebuffer;
            this.entityOutlinesFramebuffer = EntityShaders.overlayFramebuffer;

            EntityShaders.overlayVertexConsumerProvider.setColor(0, 0, 0, 100);

            GlStateManager._disableDepthTest();
            renderEntity(entity, cameraX, cameraY, cameraZ, tickDelta, matrices, EntityShaders.overlayVertexConsumerProvider);
            GlStateManager._enableDepthTest();

            this.entityOutlinesFramebuffer = prevBuffer;
        }

        if (EntityShaders.shouldDrawOutline(entity) && vertexConsumers != EntityShaders.outlinesVertexConsumerProvider && vertexConsumers != EntityShaders.overlayVertexConsumerProvider) {
            Framebuffer prevBuffer = this.entityOutlinesFramebuffer;
            this.entityOutlinesFramebuffer = EntityShaders.outlinesFramebuffer;
            Utils.renderingEntityOutline = true;

            Color color = Modules.get().get(ESP.class).getOutlineColor(entity);
            EntityShaders.outlinesVertexConsumerProvider.setColor(color.r, color.g, color.b, color.a);

            GlStateManager._disableDepthTest();
            renderEntity(entity, cameraX, cameraY, cameraZ, tickDelta, matrices, EntityShaders.outlinesVertexConsumerProvider);
            GlStateManager._enableDepthTest();

            Utils.renderingEntityOutline = false;
            this.entityOutlinesFramebuffer = prevBuffer;
        }
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/OutlineVertexConsumerProvider;draw()V"))
    private void onRender(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f, CallbackInfo info) {
        EntityShaders.endRender();
    }

    @Inject(method = "onResized", at = @At("HEAD"))
    private void onResized(int i, int j, CallbackInfo info) {
        EntityShaders.onResized(i, j);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/EntityRenderDispatcher;shouldRender(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/render/Frustum;DDD)Z"))
    private <E extends Entity> boolean shouldRenderRedirect(EntityRenderDispatcher entityRenderDispatcher, E entity, Frustum frustum, double x, double y, double z) {
        return Modules.get().isActive(Chams.class) || entityRenderDispatcher.shouldRender(entity, frustum, x, y, z);
    }

    /**
     * @author Walaryne
     */
    @Inject(method = "renderEndSky", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Tessellator;draw()V"))
    private void onRenderEndSkyDraw(MatrixStack matrices, CallbackInfo info) {
        Ambience ambience = Modules.get().get(Ambience.class);

        if (ambience.isActive() && ambience.endSky.get() && ambience.customSkyColor.get()) {
            Color customEndSkyColor = ambience.skyColor();

            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferBuilder = tessellator.getBuffer();
            Matrix4f matrix4f = matrices.peek().getPositionMatrix();

            bufferBuilder.clear();

            bufferBuilder.vertex(matrix4f, -100.0F, -100.0F, -100.0F).texture(0.0F, 0.0F).color(customEndSkyColor.r, customEndSkyColor.g, customEndSkyColor.b, 255).next();
            bufferBuilder.vertex(matrix4f, -100.0F, -100.0F, 100.0F).texture(0.0F, 16.0F).color(customEndSkyColor.r, customEndSkyColor.g, customEndSkyColor.b, 255).next();
            bufferBuilder.vertex(matrix4f, 100.0F, -100.0F, 100.0F).texture(16.0F, 16.0F).color(customEndSkyColor.r, customEndSkyColor.g, customEndSkyColor.b, 255).next();
            bufferBuilder.vertex(matrix4f, 100.0F, -100.0F, -100.0F).texture(16.0F, 0.0F).color(customEndSkyColor.r, customEndSkyColor.g, customEndSkyColor.b, 255).next();
        }
    }

    @ModifyVariable(method = "getLightmapCoordinates(Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;)I", at = @At(value = "STORE", ordinal = 0))
    private static int getLightmapCoordinatesModifySkyLight(int sky) {
        return Math.max(Utils.minimumLightLevel, sky);
    }
}
