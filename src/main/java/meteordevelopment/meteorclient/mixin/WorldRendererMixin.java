/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.*;
import meteordevelopment.meteorclient.systems.modules.world.Ambience;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.postprocess.EntityShader;
import meteordevelopment.meteorclient.utils.render.postprocess.PostProcessShaders;
import net.minecraft.block.BlockState;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
    @Shadow
    private Framebuffer entityOutlinesFramebuffer;

    @Unique private ESP esp;

    @Shadow
    protected abstract void renderEntity(Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers);

    @Inject(method = "checkEmpty", at = @At("HEAD"), cancellable = true)
    private void onCheckEmpty(MatrixStack matrixStack, CallbackInfo info) {
        info.cancel();
    }

    @Inject(method = "drawBlockOutline", at = @At("HEAD"), cancellable = true)
    private void onDrawHighlightedBlockOutline(MatrixStack matrixStack, VertexConsumer vertexConsumer, Entity entity, double d, double e, double f, BlockPos blockPos, BlockState blockState, CallbackInfo info) {
        if (Modules.get().isActive(BlockSelection.class)) info.cancel();
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;setupTerrain(Lnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/Frustum;ZZ)V"), index = 3)
    private boolean renderSetupTerrainModifyArg(boolean spectator) {
        return Modules.get().isActive(Freecam.class) || spectator;
    }

    // No Render

    @Inject(method = "renderWeather", at = @At("HEAD"), cancellable = true)
    private void onRenderWeather(LightmapTextureManager manager, float f, double d, double e, double g, CallbackInfo info) {
        if (Modules.get().get(NoRender.class).noWeather()) info.cancel();
    }

	@Inject(method = "hasBlindnessOrDarkness(Lnet/minecraft/client/render/Camera;)Z", at = @At("HEAD"), cancellable = true)
	private void hasBlindnessOrDarkness(Camera camera, CallbackInfoReturnable<Boolean> info) {
		if (Modules.get().get(NoRender.class).noBlindness() || Modules.get().get(NoRender.class).noDarkness()) info.setReturnValue(null);
	}

    // Entity Shaders

    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderHead(RenderTickCounter tickCounter, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci) {
        PostProcessShaders.beginRender();
    }

    @Inject(method = "renderEntity", at = @At("HEAD"))
    private void renderEntity(Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo info) {
        draw(entity, cameraX, cameraY, cameraZ, tickDelta, vertexConsumers, matrices, PostProcessShaders.CHAMS, Color.WHITE);
        draw(entity, cameraX, cameraY, cameraZ, tickDelta, vertexConsumers, matrices, PostProcessShaders.ENTITY_OUTLINE, Modules.get().get(ESP.class).getColor(entity));
    }

    @Unique
    private void draw(Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, VertexConsumerProvider vertexConsumers, MatrixStack matrices, EntityShader shader, Color color) {
        if (shader.shouldDraw(entity) && !PostProcessShaders.isCustom(vertexConsumers) && color != null) {
            Framebuffer prevBuffer = this.entityOutlinesFramebuffer;
            this.entityOutlinesFramebuffer = shader.framebuffer;
            PostProcessShaders.rendering = true;

            shader.vertexConsumerProvider.setColor(color.r, color.g, color.b, color.a);
            renderEntity(entity, cameraX, cameraY, cameraZ, tickDelta, matrices, shader.vertexConsumerProvider);

            PostProcessShaders.rendering = false;
            this.entityOutlinesFramebuffer = prevBuffer;
        }
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/OutlineVertexConsumerProvider;draw()V"))
    private void onRender(RenderTickCounter tickCounter, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci) {
        PostProcessShaders.endRender();
    }

    @ModifyExpressionValue(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;hasOutline(Lnet/minecraft/entity/Entity;)Z"))
    private boolean shouldMobGlow(boolean original, @Local Entity entity) {
        if (!getESP().isGlow() || getESP().shouldSkip(entity)) return original;

        return getESP().getColor(entity) != null || original;
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/OutlineVertexConsumerProvider;setColor(IIII)V"))
    private void setGlowColor(OutlineVertexConsumerProvider instance, int red, int green, int blue, int alpha, Operation<Void> original, @Local LocalRef<Entity> entity) {
        if (!getESP().isGlow() || getESP().shouldSkip(entity.get())) original.call(instance, red, green, blue, alpha);
        else {
            Color color = getESP().getColor(entity.get());

            if (color == null) original.call(instance, red, green, blue, alpha);
            else instance.setColor(color.r, color.g, color.b, color.a);
        }
    }

    @Inject(method = "onResized", at = @At("HEAD"))
    private void onResized(int width, int height, CallbackInfo info) {
        PostProcessShaders.onResized(width, height);
    }

    // Ambience

    @Inject(method = "renderEndSky", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/BufferRenderer;drawWithGlobalProgram(Lnet/minecraft/client/render/BuiltBuffer;)V"))
    private void onRenderEndSkyDraw(MatrixStack matrices, CallbackInfo info) {
        Ambience ambience = Modules.get().get(Ambience.class);

        if (ambience.isActive() && ambience.endSky.get() && ambience.customSkyColor.get()) {
            Color customEndSkyColor = ambience.skyColor();

            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            Matrix4f matrix4f = matrices.peek().getPositionMatrix();

            bufferBuilder.vertex(matrix4f, -100.0F, -100.0F, -100.0F).texture(0.0F, 0.0F).color(customEndSkyColor.r, customEndSkyColor.g, customEndSkyColor.b, 255);
            bufferBuilder.vertex(matrix4f, -100.0F, -100.0F, 100.0F).texture(0.0F, 16.0F).color(customEndSkyColor.r, customEndSkyColor.g, customEndSkyColor.b, 255);
            bufferBuilder.vertex(matrix4f, 100.0F, -100.0F, 100.0F).texture(16.0F, 16.0F).color(customEndSkyColor.r, customEndSkyColor.g, customEndSkyColor.b, 255);
            bufferBuilder.vertex(matrix4f, 100.0F, -100.0F, -100.0F).texture(16.0F, 0.0F).color(customEndSkyColor.r, customEndSkyColor.g, customEndSkyColor.b, 255);
        }
    }

    // Fullbright

    @ModifyVariable(method = "getLightmapCoordinates(Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;)I", at = @At(value = "STORE"), ordinal = 0)
    private static int getLightmapCoordinatesModifySkyLight(int sky) {
        return Math.max(Modules.get().get(Fullbright.class).getLuminance(LightType.SKY), sky);
    }

    @ModifyVariable(method = "getLightmapCoordinates(Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;)I", at = @At(value = "STORE"), ordinal = 1)
    private static int getLightmapCoordinatesModifyBlockLight(int sky) {
        return Math.max(Modules.get().get(Fullbright.class).getLuminance(LightType.BLOCK), sky);
    }

    @Unique
    private ESP getESP() {
        if (esp == null) {
            esp = Modules.get().get(ESP.class);
        }

        return esp;
    }
}
