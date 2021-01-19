/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.mixin;

import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.render.*;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.render.Outlines;
import minegame159.meteorclient.utils.render.color.Color;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
    @Shadow protected abstract void renderEntity(Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers);

    @Shadow private Framebuffer entityOutlinesFramebuffer;

    @Shadow @Final private MinecraftClient client;

    @Inject(method = "loadEntityOutlineShader", at = @At("TAIL"))
    private void onLoadEntityOutlineShader(CallbackInfo info) {
        Outlines.load();
    }

    @Inject(method = "checkEmpty", at = @At("HEAD"), cancellable = true)
    private void onCheckEmpty(MatrixStack matrixStack, CallbackInfo info) {
        info.cancel();
    }

    @Inject(method = "renderWeather", at = @At("HEAD"), cancellable = true)
    private void onRenderWeather(LightmapTextureManager manager, float f, double d, double e, double g, CallbackInfo info) {
        if (ModuleManager.INSTANCE.get(NoRender.class).noWeather()) info.cancel();
    }

    @Inject(method = "renderEntity", at = @At("HEAD"))
    private void onRenderEntitiesHead(Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, MatrixStack matrix, VertexConsumerProvider vertexConsumers, CallbackInfo info) {
        Utils.blockRenderingBlockEntitiesInXray = true;
    }

    @Inject(method = "renderEntity", at = @At("TAIL"))
    private void onRenderEntitiesTail(Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, MatrixStack matrix, VertexConsumerProvider vertexConsumers, CallbackInfo info) {
        Utils.blockRenderingBlockEntitiesInXray = false;
    }

    @Inject(method = "drawBlockOutline", at = @At("HEAD"), cancellable = true)
    private void onDrawHighlightedBlockOutline(MatrixStack matrixStack, VertexConsumer vertexConsumer, Entity entity, double d, double e, double f, BlockPos blockPos, BlockState blockState, CallbackInfo info) {
        if (ModuleManager.INSTANCE.isActive(BlockSelection.class)) info.cancel();
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isSpectator()Z"))
    private boolean renderIsSpectatorProxy(ClientPlayerEntity player) {
        return ModuleManager.INSTANCE.isActive(Freecam.class) || player.isSpectator();
    }

    // Outlines

    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderHead(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f, CallbackInfo info) {
        Outlines.beginRender();
    }

    @Inject(method = "renderEntity", at = @At("HEAD"), cancellable = true)
    private void renderEntity(Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo info) {
        if (vertexConsumers == Outlines.vertexConsumerProvider) return;

        ESP esp = ModuleManager.INSTANCE.get(ESP.class);
        if (!esp.isActive() || !esp.isOutline()) return;

        Color color = esp.getOutlineColor(entity);

        if (color != null) {
            Framebuffer fbo = this.entityOutlinesFramebuffer;
            this.entityOutlinesFramebuffer = Outlines.outlinesFbo;

            Outlines.vertexConsumerProvider.setColor(color.r, color.g, color.b, color.a);
            renderEntity(entity, cameraX, cameraY, cameraZ, tickDelta, matrices, Outlines.vertexConsumerProvider);

            this.entityOutlinesFramebuffer = fbo;
        }
    }
    
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/OutlineVertexConsumerProvider;draw()V"))
    private void onRender(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f, CallbackInfo info) {
        Outlines.endRender(tickDelta);
    }

    @Inject(method = "drawEntityOutlinesFramebuffer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/Framebuffer;draw(IIZ)V"))
    private void onDrawEntityOutlinesFramebuffer(CallbackInfo info) {
        Outlines.renderFbo();
    }

    @Inject(method = "onResized", at = @At("HEAD"))
    private void onResized(int i, int j, CallbackInfo info) {
        Outlines.onResized(i, j);
    }

    // Break Indicators start

    @Inject(method = "setBlockBreakingInfo", at = @At("HEAD"), cancellable = true)
    private void onBlockBreakingInfo(int entityId, BlockPos pos, int stage, CallbackInfo ci) {
        BreakIndicators bi = ModuleManager.INSTANCE.get(BreakIndicators.class);
        if(!bi.isActive())
            return;

        if(!bi.multiple.get() && entityId != client.player.getEntityId())
            return;

        if (0 <= stage && stage <= 8) {
            BlockBreakingInfo info = new BlockBreakingInfo(entityId, pos);
            info.setStage(stage);
            bi.blocks.put(entityId, info);

            if (bi.hideVanillaIndicators.get()) {
                ci.cancel();
            }

        } else {
            bi.blocks.remove(entityId);
        }
    }
    @Inject(method = "removeBlockBreakingInfo", at = @At("TAIL"))
    private void onBlockBreakingInfoRemoval(BlockBreakingInfo info, CallbackInfo ci) {
        BreakIndicators bi = ModuleManager.INSTANCE.get(BreakIndicators.class);
        if(!bi.isActive())
            return;

        bi.blocks.values().removeIf(info::equals);
    }

    // Break Indicators end

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/EntityRenderDispatcher;shouldRender(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/render/Frustum;DDD)Z"))
    private <E extends Entity> boolean shouldRenderRedirect(EntityRenderDispatcher entityRenderDispatcher, E entity, Frustum frustum, double x, double y, double z) {
        Chams chams = ModuleManager.INSTANCE.get(Chams.class);
        if(chams.isActive() && chams.throughWalls.get()) {
            return true;
        } else {
            return entityRenderDispatcher.shouldRender(entity, frustum, x, y, z);
        }
    }
}
