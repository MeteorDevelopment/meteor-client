/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import it.unimi.dsi.fastutil.Stack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import meteordevelopment.meteorclient.mixininterface.IWorldRenderer;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.BlockSelection;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import meteordevelopment.meteorclient.utils.render.postprocess.PostProcessShaders;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.render.state.OutlineRenderState;
import net.minecraft.client.render.state.WeatherRenderState;
import net.minecraft.client.render.state.WorldBorderRenderState;
import net.minecraft.client.util.Handle;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin implements IWorldRenderer {
    @Inject(method = "checkEmpty", at = @At("HEAD"), cancellable = true)
    private void onCheckEmpty(MatrixStack matrixStack, CallbackInfo info) {
        info.cancel();
    }

    @Inject(method = "drawBlockOutline", at = @At("HEAD"), cancellable = true)
    private void onDrawHighlightedBlockOutline(MatrixStack matrices, VertexConsumer vertexConsumer, double x, double y, double z, OutlineRenderState state, int i, CallbackInfo ci) {
        if (Modules.get().isActive(BlockSelection.class)) ci.cancel();
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;method_74752(Lnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/Frustum;Z)V"), index = 2)
    private boolean renderSetupTerrainModifyArg(boolean spectator) {
        return Modules.get().isActive(Freecam.class) || spectator;
    }

    // No Render

    @WrapWithCondition(method = "method_62216", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WeatherRendering;renderPrecipitation(Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/client/render/state/WeatherRenderState;)V"))
    private boolean shouldRenderPrecipitation(WeatherRendering instance, VertexConsumerProvider vertexConsumers, Vec3d pos, WeatherRenderState weatherRenderState) {
        return !Modules.get().get(NoRender.class).noWeather();
    }

    @WrapWithCondition(method = "method_62216", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldBorderRendering;render(Lnet/minecraft/client/render/state/WorldBorderRenderState;Lnet/minecraft/util/math/Vec3d;DD)V"))
    private boolean shouldRenderWorldBorder(WorldBorderRendering instance, WorldBorderRenderState state, Vec3d cameraPos, double viewDistanceBlocks, double farPlaneDistance) {
        return !Modules.get().get(NoRender.class).noWorldBorder();
    }

	@Inject(method = "hasBlindnessOrDarkness(Lnet/minecraft/client/render/Camera;)Z", at = @At("HEAD"), cancellable = true)
	private void hasBlindnessOrDarkness(Camera camera, CallbackInfoReturnable<Boolean> info) {
		if (Modules.get().get(NoRender.class).noBlindness() || Modules.get().get(NoRender.class).noDarkness()) info.setReturnValue(null);
	}

    // Entity Shaders

    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderHead(ObjectAllocator allocator,
                              RenderTickCounter tickCounter,
                              boolean renderBlockOutline,
                              Camera camera,
                              Matrix4f positionMatrix,
                              Matrix4f projectionMatrix,
                              Matrix4f matrix4f2,
                              GpuBufferSlice fog,
                              Vector4f fogColor,
                              boolean shouldRenderSky,
                              CallbackInfo ci) {
        PostProcessShaders.beginRender();
    }

    /*
    todo Entity rendering is batched in pushEntityRenders before they are all rendered at once by calling
      net.minecraft.client.render.entity.command.EntityRenderDispatcher.render
    Our renders need to be rewritten

    @Inject(method = "renderEntity", at = @At("HEAD"))
    private void renderEntity(Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo info) {
        draw(entity, cameraX, cameraY, cameraZ, tickDelta, vertexConsumers, matrices, PostProcessShaders.CHAMS, Color.WHITE);
        draw(entity, cameraX, cameraY, cameraZ, tickDelta, vertexConsumers, matrices, PostProcessShaders.ENTITY_OUTLINE, Modules.get().get(ESP.class).getColor(entity));
    }

    @Unique
    private void draw(Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, VertexConsumerProvider vertexConsumers, MatrixStack matrices, EntityShader shader, Color color) {
        if (shader.shouldDraw(entity) && !PostProcessShaders.isCustom(vertexConsumers) && color != null) {
            meteor$pushEntityOutlineFramebuffer(shader.framebuffer);
            PostProcessShaders.rendering = true;

            shader.vertexConsumerProvider.setColor(color.getPacked());
            renderEntity(entity, cameraX, cameraY, cameraZ, tickDelta, matrices, shader.vertexConsumerProvider);

            PostProcessShaders.rendering = false;
            meteor$popEntityOutlineFramebuffer();
        }
    }
     */

    @Inject(method = "method_62214", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/OutlineVertexConsumerProvider;draw()V"))
    private void onRender(CallbackInfo ci) {
        PostProcessShaders.endRender();
    }

    @Inject(method = "onResized", at = @At("HEAD"))
    private void onResized(int width, int height, CallbackInfo info) {
        PostProcessShaders.onResized(width, height);
    }

    // IWorldRenderer

    @Shadow
    private Framebuffer entityOutlineFramebuffer;

    @Shadow
    @Final
    private DefaultFramebufferSet framebufferSet;

    @Unique
    private Stack<Framebuffer> framebufferStack;

    @Unique
    private Stack<Handle<Framebuffer>> framebufferHandleStack;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void init$IWorldRenderer(CallbackInfo info) {
        framebufferStack = new ObjectArrayList<>();
        framebufferHandleStack = new ObjectArrayList<>();
    }

    @Override
    public void meteor$pushEntityOutlineFramebuffer(Framebuffer framebuffer) {
        framebufferStack.push(this.entityOutlineFramebuffer);
        this.entityOutlineFramebuffer = framebuffer;

        framebufferHandleStack.push(this.framebufferSet.entityOutlineFramebuffer);
        this.framebufferSet.entityOutlineFramebuffer = () -> framebuffer;
    }

    @Override
    public void meteor$popEntityOutlineFramebuffer() {
        this.entityOutlineFramebuffer = framebufferStack.pop();
        this.framebufferSet.entityOutlineFramebuffer = framebufferHandleStack.pop();
    }
}
