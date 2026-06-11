/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.Stack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import meteordevelopment.meteorclient.mixininterface.IEntityRenderState;
import meteordevelopment.meteorclient.mixininterface.ILevelRenderer;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.BlockSelection;
import meteordevelopment.meteorclient.systems.modules.render.ESP;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import meteordevelopment.meteorclient.utils.OutlineRenderCommandQueue;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.postprocess.EntityShader;
import meteordevelopment.meteorclient.utils.render.postprocess.PostProcessShaders;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Function;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin implements ILevelRenderer {
    @Unique
    private FeatureRenderDispatcher renderDispatcher;
    @Unique
    private final OutlineRenderCommandQueue outlineRenderCommandQueue = new OutlineRenderCommandQueue();

    @Inject(method = "checkPoseStack", at = @At("HEAD"), cancellable = true)
    private void onCheckPoseStack(PoseStack poseStack, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "submitBlockOutline", at = @At("HEAD"), cancellable = true)
    private void onDrawHighlightedHitOutline(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, LevelRenderState levelRenderState, CallbackInfo ci) {
        if (Modules.get().isActive(BlockSelection.class)) ci.cancel();
    }

    // No Render

    @ModifyExpressionValue(method = "addSkyPass", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/state/level/CameraEntityRenderState;doesMobEffectBlockSky:Z", opcode = Opcodes.GETFIELD))
    private boolean modifyMobEffectBlocksSky(boolean original) {
        NoRender noRender = Modules.get().get(NoRender.class);
        if (noRender.noBlindness() || noRender.noDarkness()) return false;
        return original;
    }

    // Entity Shaders

    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderLevelHead(GraphicsResourceAllocator resourceAllocator, DeltaTracker deltaTracker, boolean renderOutline, CameraRenderState cameraState, Matrix4fc modelViewMatrix, GpuBufferSlice terrainFog, Vector4f fogColor, boolean shouldRenderSky, CallbackInfo ci) {
        PostProcessShaders.beginRender();
    }

    @Inject(method = "submitEntities", at = @At("TAIL"))
    private void onSubmitEntities(PoseStack poseStack, LevelRenderState levelRenderState, SubmitNodeCollector output, CallbackInfo ci) {
        if (renderDispatcher == null) {
            renderDispatcher = new FeatureRenderDispatcher(
                renderBuffers,
                mc.getModelManager(),
                mc.getAtlasManager(),
                mc.font,
                mc.gameRenderer.gameRenderState()
            );
        }

        draw(levelRenderState, poseStack, PostProcessShaders.CHAMS, _ -> Color.WHITE);
        draw(levelRenderState, poseStack, PostProcessShaders.ENTITY_OUTLINE, entity -> Modules.get().get(ESP.class).getColor(entity));
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
        renderDispatcher.renderAllFeatures(outlineRenderCommandQueue);
        outlineRenderCommandQueue.submitsPerOrder.clear();
        meteor$popEntityOutlineFramebuffer();
    }

    @Inject(method = "lambda$addMainPass$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;executeOutline()V", shift = At.Shift.AFTER))
    private void addMainPass$submitEntityVertices(CallbackInfo ci) {
        PostProcessShaders.submitEntityVertices();
    }

    @Inject(method = "resize", at = @At("HEAD"))
    private void onResize(int width, int height, CallbackInfo ci) {
        PostProcessShaders.onResized(width, height);
    }

    // ILevelRenderer

    // FIXME(26.2): this is effectively @Final, yet we change it later, causing a crash in ESP Shader mode.
    @Shadow
    private RenderTarget entityOutlineTarget;

    @Shadow
    @Final
    private LevelTargetBundle targets;

    @Shadow
    @Final
    private EntityRenderDispatcher entityRenderDispatcher;
    @Shadow
    @Final
    private RenderBuffers renderBuffers;
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
