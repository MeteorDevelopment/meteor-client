/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
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
import meteordevelopment.meteorclient.systems.modules.world.Ambience;
import meteordevelopment.meteorclient.utils.OutlineRenderCommandQueue;
import meteordevelopment.meteorclient.utils.render.NoopImmediateVertexConsumerProvider;
import meteordevelopment.meteorclient.utils.render.NoopOutlineVertexConsumerProvider;
import meteordevelopment.meteorclient.utils.render.WrapperImmediateVertexConsumerProvider;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.postprocess.EntityShader;
import meteordevelopment.meteorclient.utils.render.postprocess.PostProcessShaders;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.render.*;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.state.BlockOutlineRenderState;
import net.minecraft.client.renderer.state.WeatherRenderState;
import net.minecraft.client.renderer.state.WorldBorderRenderState;
import net.minecraft.client.renderer.state.LevelRenderState;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
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

// TODO(Ravel): can not resolve target class WorldRenderer
// TODO(Ravel): can not resolve target class WorldRenderer
@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin implements IWorldRenderer {

    @Unique
    private NoRender noRender;
    @Unique
    private ESP esp;

    // TODO(Ravel): no target class
// TODO(Ravel): no target class
// if a world exists, meteor is initialised
    @Inject(method = "setWorld", at = @At("TAIL"))
    private void onSetWorld(ClientLevel world, CallbackInfo ci) {
        esp = Modules.get().get(ESP.class);
        noRender = Modules.get().get(NoRender.class);
    }

    // TODO(Ravel): no target class
// TODO(Ravel): no target class
    @Inject(method = "checkEmpty", at = @At("HEAD"), cancellable = true)
    private void onCheckEmpty(PoseStack matrixStack, CallbackInfo info) {
        info.cancel();
    }

    // TODO(Ravel): no target class
// TODO(Ravel): no target class
    @Inject(method = "drawBlockOutline", at = @At("HEAD"), cancellable = true)
    private void onDrawHighlightedBlockOutline(PoseStack matrices, VertexConsumer vertexConsumer, double x, double y, double z, BlockOutlineRenderState state, int i, float f, CallbackInfo ci) {
        if (Modules.get().isActive(BlockSelection.class)) ci.cancel();
    }

    // TODO(Ravel): no target class
// TODO(Ravel): no target class
    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;cullTerrain(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/culling/Frustum;Z)V"), index = 2)
    private boolean renderSetupTerrainModifyArg(boolean spectator) {
        return Modules.get().isActive(Freecam.class) || spectator;
    }

    // No Render

    // TODO(Ravel): no target class
// TODO(Ravel): no target class
    @WrapWithCondition(method = "method_62216", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/WeatherEffectRenderer;render(Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/state/WeatherRenderState;)V"))
    private boolean shouldRenderPrecipitation(WeatherRendering instance, VertexConsumerProvider vertexConsumers, Vec3 pos, WeatherRenderState weatherRenderState) {
        return !noRender.noWeather();
    }

    // TODO(Ravel): no target class
// TODO(Ravel): no target class
    @WrapWithCondition(method = "method_62216", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/WorldBorderRenderer;render(Lnet/minecraft/client/renderer/state/WorldBorderRenderState;Lnet/minecraft/world/phys/Vec3;DD)V"))
    private boolean shouldRenderWorldBorder(WorldBorderRendering instance, WorldBorderRenderState state, Vec3 cameraPos, double viewDistanceBlocks, double farPlaneDistance) {
        return !noRender.noWorldBorder();
    }

    // TODO(Ravel): no target class
// TODO(Ravel): no target class
    @Inject(method = "hasBlindnessOrDarkness(Lnet/minecraft/client/render/Camera;)Z", at = @At("HEAD"), cancellable = true)
    private void hasBlindnessOrDarkness(Camera camera, CallbackInfoReturnable<Boolean> info) {
        if (noRender.noBlindness() || noRender.noDarkness()) info.setReturnValue(null);
    }

    // Entity Shaders

    // TODO(Ravel): no target class
// TODO(Ravel): no target class
    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderHead(GraphicsResourceAllocator allocator,
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
    private FeatureRenderDispatcher renderDispatcher;

    // TODO(Ravel): no target class
// TODO(Ravel): no target class
    @Inject(method = "pushEntityRenders", at = @At("TAIL"))
    private void onPushEntityRenders(PoseStack matrices, LevelRenderState worldState, SubmitNodeCollector queue, CallbackInfo info) {
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
        draw(worldState, matrices, PostProcessShaders.ENTITY_OUTLINE, entity -> esp.getColor(entity));
    }

    @Unique
    private void draw(LevelRenderState worldState, PoseStack matrices, EntityShader shader, Function<Entity, Color> colorGetter) {
        var camera = worldState.cameraRenderState.pos;
        var empty = true;

        for (var state : worldState.entityRenderStates) {
            Entity entity = ((IEntityRenderState) state).meteor$getEntity();
            if (entity == null) continue;

            if (!shader.shouldDraw(entity)) continue;

            var color = colorGetter.apply(entity);
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

    // TODO(Ravel): no target class
// TODO(Ravel): no target class
    @ModifyExpressionValue(method = "fillEntityRenderStates", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;isSectionCompiledAndVisible(Lnet/minecraft/core/BlockPos;)Z"))
    boolean fillEntityRenderStatesIsRenderingReady(boolean original) {
        if (esp.forceRender()) return true;
        return original;
    }

    // TODO(Ravel): no target class
// TODO(Ravel): no target class
    @Inject(method = "method_62214", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/OutlineBufferSource;endOutlineBatch()V"))
    private void onRender(CallbackInfo ci) {
        PostProcessShaders.submitEntityVertices();
    }

    // TODO(Ravel): no target class
// TODO(Ravel): no target class
    @Inject(method = "onResized", at = @At("HEAD"))
    private void onResized(int width, int height, CallbackInfo info) {
        PostProcessShaders.onResized(width, height);
    }

    // TODO(Ravel): no target class
// TODO(Ravel): no target class
    @ModifyArg(method = "method_62205", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/CloudRenderer;render(ILnet/minecraft/client/CloudStatus;FLnet/minecraft/world/phys/Vec3;JF)V"))
    private int modifyColor(int original) {
        Ambience ambience = Modules.get().get(Ambience.class);
        if (ambience.isActive() && ambience.customCloudColor.get()) {
            return ambience.cloudColor.get().getPacked();
        }

        return original;
    }

    // IWorldRenderer

    // TODO(Ravel): Could not determine a single target
// TODO(Ravel): Could not determine a single target
    @Shadow
    private RenderTarget entityOutlineFramebuffer;

    // TODO(Ravel): Could not determine a single target
// TODO(Ravel): Could not determine a single target
    @Shadow
    @Final
    private DefaultFramebufferSet framebufferSet;

    // TODO(Ravel): Could not determine a single target
// TODO(Ravel): Could not determine a single target
    @Shadow
    @Final
    private EntityRenderDispatcher entityRenderManager;
    @Unique
    private Stack<RenderTarget> framebufferStack;

    @Unique
    private Stack<ResourceHandle<RenderTarget>> framebufferHandleStack;

    // TODO(Ravel): no target class
// TODO(Ravel): no target class
    @Inject(method = "<init>", at = @At("TAIL"))
    private void init$IWorldRenderer(CallbackInfo info) {
        framebufferStack = new ObjectArrayList<>();
        framebufferHandleStack = new ObjectArrayList<>();
    }

    @Override
    public void meteor$pushEntityOutlineFramebuffer(RenderTarget framebuffer) {
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
