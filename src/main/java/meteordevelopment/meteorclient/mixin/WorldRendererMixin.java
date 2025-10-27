/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import it.unimi.dsi.fastutil.Stack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import meteordevelopment.meteorclient.mixininterface.IEntityRenderState;
import meteordevelopment.meteorclient.mixininterface.IWorldRenderer;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.BlockSelection;
import meteordevelopment.meteorclient.systems.modules.render.ESP;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import meteordevelopment.meteorclient.utils.OutlineRenderCommandQueue;
import meteordevelopment.meteorclient.utils.render.NoopImmediateVertexConsumerProvider;
import meteordevelopment.meteorclient.utils.render.NoopOutlineVertexConsumerProvider;
import meteordevelopment.meteorclient.utils.render.WrapperImmediateVertexConsumerProvider;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.postprocess.EntityShader;
import meteordevelopment.meteorclient.utils.render.postprocess.PostProcessShaders;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.command.RenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.state.OutlineRenderState;
import net.minecraft.client.render.state.WeatherRenderState;
import net.minecraft.client.render.state.WorldBorderRenderState;
import net.minecraft.client.render.state.WorldRenderState;
import net.minecraft.client.util.Handle;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
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

import java.util.function.Function;

import static meteordevelopment.meteorclient.MeteorClient.mc;

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

    @Unique
    private final OutlineRenderCommandQueue outlineRenderCommandQueue = new OutlineRenderCommandQueue();

    @Unique
    private VertexConsumerProvider provider;

    @Unique
    private RenderDispatcher renderDispatcher;

    @Inject(method = "pushEntityRenders", at = @At("TAIL"))
    private void onPushEntityRenders(MatrixStack matrices, WorldRenderState worldState, OrderedRenderCommandQueue queue, CallbackInfo info) {
        if (renderDispatcher == null) {
            renderDispatcher = new RenderDispatcher(
                outlineRenderCommandQueue,
                mc.getBlockRenderManager(),
                new WrapperImmediateVertexConsumerProvider(() -> provider),
                mc.getAtlasManager(),
                NoopOutlineVertexConsumerProvider.INSTANCE,
                NoopImmediateVertexConsumerProvider.INSTANCE,
                mc.textRenderer
            );
        }

        draw(worldState, matrices, PostProcessShaders.CHAMS, entity -> Color.WHITE);
        draw(worldState, matrices, PostProcessShaders.ENTITY_OUTLINE, entity -> Modules.get().get(ESP.class).getColor(entity));
    }

    @Unique
    private void draw(WorldRenderState worldState, MatrixStack matrices, EntityShader shader, Function<Entity, Color> colorGetter) {
        var camera = worldState.cameraRenderState.pos;
        var empty = true;

        for (var state : worldState.entityRenderStates) {
            if (!shader.shouldDraw(((IEntityRenderState) state).meteor$getEntity())) continue;

            var color = colorGetter.apply(((IEntityRenderState) state).meteor$getEntity());
            if (color == null) continue;
            outlineRenderCommandQueue.setColor(color);

            var renderer = entityRenderManager.getRenderer(state);
            var offset = renderer.getPositionOffset(state);

            matrices.push();
            matrices.translate(state.x - camera.x + offset.x, state.y - camera.y + offset.y, state.z - camera.z + offset.z);
            renderer.render(state, matrices, outlineRenderCommandQueue, worldState.cameraRenderState);
            matrices.pop();

            empty = false;
        }

        if (empty)
            return;

        meteor$pushEntityOutlineFramebuffer(shader.framebuffer);
        provider = shader.vertexConsumerProvider;

        renderDispatcher.render();
        outlineRenderCommandQueue.onNextFrame();

        provider = null;
        meteor$popEntityOutlineFramebuffer();
    }

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

    @Shadow
    @Final
    private EntityRenderManager entityRenderManager;
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
