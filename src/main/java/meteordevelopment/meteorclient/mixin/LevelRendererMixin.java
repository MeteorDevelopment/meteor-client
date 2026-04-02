/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.Stack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import meteordevelopment.meteorclient.mixininterface.IEntityRenderState;
import meteordevelopment.meteorclient.mixininterface.ILevelRenderer;
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
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.BlockOutlineRenderState;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.client.renderer.state.WeatherRenderState;
import net.minecraft.client.renderer.state.WorldBorderRenderState;
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

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin implements ILevelRenderer {

    @Unique
    private NoRender noRender;
    @Unique
    private ESP esp;

    // if a world exists, meteor is initialised
    @Inject(method = "setLevel", at = @At("TAIL"))
    private void onSetLevel(ClientLevel world, CallbackInfo ci) {
        esp = Modules.get().get(ESP.class);
        noRender = Modules.get().get(NoRender.class);
    }

    @Inject(method = "checkPoseStack", at = @At("HEAD"), cancellable = true)
    private void onCheckPoseStack(PoseStack matrixStack, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "renderHitOutline", at = @At("HEAD"), cancellable = true)
    private void onDrawHighlightedHitOutline(PoseStack poseStack, VertexConsumer vertexConsumer, double d, double e, double f, BlockOutlineRenderState blockOutlineRenderState, int i, float g, CallbackInfo ci) {
        if (Modules.get().isActive(BlockSelection.class)) ci.cancel();
    }

    @ModifyArg(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;cullTerrain(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/culling/Frustum;Z)V"), index = 2)
    private boolean renderLevelSetupTerrainModifyArg(boolean spectator) {
        return Modules.get().isActive(Freecam.class) || spectator;
    }

    // No Render

    @WrapWithCondition(method = "method_62216", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/WeatherEffectRenderer;render(Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/state/WeatherRenderState;)V"))
    private boolean shouldRenderPrecipitation(WeatherEffectRenderer instance, MultiBufferSource multiBufferSource, Vec3 vec3, WeatherRenderState weatherRenderState) {
        return !noRender.noWeather();
    }

    @WrapWithCondition(method = "method_62216", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/WorldBorderRenderer;render(Lnet/minecraft/client/renderer/state/WorldBorderRenderState;Lnet/minecraft/world/phys/Vec3;DD)V"))
    private boolean shouldRenderWorldBorder(WorldBorderRenderer instance, WorldBorderRenderState worldBorderRenderState, Vec3 vec3, double d, double e) {
        return !noRender.noWorldBorder();
    }

    @Inject(method = "doesMobEffectBlockSky", at = @At("HEAD"), cancellable = true)
    private void hasBlindnessOrDarkness(Camera camera, CallbackInfoReturnable<Boolean> cir) {
        if (noRender.noBlindness() || noRender.noDarkness()) cir.setReturnValue(null);
    }

    // Entity Shaders

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void onRenderLevelHead(GraphicsResourceAllocator allocator,
                                   DeltaTracker deltaTracker,
                                   boolean renderBlockOutline,
                                   Camera camera,
                                   Matrix4f positionMatrix,
                                   Matrix4f projectionMatrix,
                                   Matrix4f matrix4f,
                                   GpuBufferSlice fog,
                                   Vector4f fogColor,
                                   boolean shouldRenderSky,
                                   CallbackInfo ci) {
        PostProcessShaders.beginRender();
    }

    @Unique
    private final OutlineRenderCommandQueue outlineRenderCommandQueue = new OutlineRenderCommandQueue();

    @Unique
    private MultiBufferSource provider;

    @Unique
    private FeatureRenderDispatcher renderDispatcher;

    @Inject(method = "submitEntities", at = @At("TAIL"))
    private void onSubmitEntities(PoseStack matrices, LevelRenderState worldState, SubmitNodeCollector queue, CallbackInfo ci) {
        if (renderDispatcher == null) {
            renderDispatcher = new FeatureRenderDispatcher(
                outlineRenderCommandQueue,
                mc.getBlockRenderer(),
                new WrapperImmediateVertexConsumerProvider(() -> provider),
                mc.getAtlasManager(),
                NoopOutlineVertexConsumerProvider.INSTANCE,
                NoopImmediateVertexConsumerProvider.INSTANCE,
                mc.font
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

            var renderer = entityRenderDispatcher.getRenderer(state);
            var offset = renderer.getRenderOffset(state);

            matrices.pushPose();
            matrices.translate(state.x - camera.x + offset.x, state.y - camera.y + offset.y, state.z - camera.z + offset.z);
            renderer.submit(state, matrices, outlineRenderCommandQueue, worldState.cameraRenderState);
            matrices.popPose();

            empty = false;
        }

        if (empty)
            return;

        meteor$pushEntityOutlineFramebuffer(shader.framebuffer);
        provider = shader.vertexConsumerProvider;

        renderDispatcher.renderAllFeatures();
        outlineRenderCommandQueue.endFrame();

        provider = null;
        meteor$popEntityOutlineFramebuffer();
    }

    @ModifyExpressionValue(method = "extractVisibleEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;isSectionCompiledAndVisible(Lnet/minecraft/core/BlockPos;)Z"))
    boolean fillEntityRenderStatesIsRenderingReady(boolean original) {
        if (esp.forceRender()) return true;
        return original;
    }

    @Inject(method = "method_62214", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/OutlineBufferSource;endOutlineBatch()V"))
    private void onRender(CallbackInfo ci) {
        PostProcessShaders.submitEntityVertices();
    }

    @Inject(method = "resize", at = @At("HEAD"))
    private void onResize(int width, int height, CallbackInfo ci) {
        PostProcessShaders.onResized(width, height);
    }

    @ModifyArg(method = "method_62205", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/CloudRenderer;render(ILnet/minecraft/client/CloudStatus;FLnet/minecraft/world/phys/Vec3;JF)V"))
    private int modifyColor(int original) {
        Ambience ambience = Modules.get().get(Ambience.class);
        if (ambience.isActive() && ambience.customCloudColor.get()) {
            return ambience.cloudColor.get().getPacked();
        }

        return original;
    }

    // ILevelRenderer

    @Shadow
    private RenderTarget entityOutlineTarget;

    @Shadow
    @Final
    private LevelTargetBundle targets;

    @Shadow
    @Final
    private EntityRenderDispatcher entityRenderDispatcher;
    @Unique
    private Stack<RenderTarget> framebufferStack;

    @Unique
    private Stack<ResourceHandle<RenderTarget>> framebufferHandleStack;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void init$IWorldRenderer(CallbackInfo ci) {
        framebufferStack = new ObjectArrayList<>();
        framebufferHandleStack = new ObjectArrayList<>();
    }

    @Override
    public void meteor$pushEntityOutlineFramebuffer(RenderTarget framebuffer) {
        framebufferStack.push(this.entityOutlineTarget);
        this.entityOutlineTarget = framebuffer;

        framebufferHandleStack.push(this.targets.entityOutline);
        this.targets.entityOutline = () -> framebuffer;
    }

    @Override
    public void meteor$popEntityOutlineFramebuffer() {
        this.entityOutlineTarget = framebufferStack.pop();
        this.targets.entityOutline = framebufferHandleStack.pop();
    }
}
