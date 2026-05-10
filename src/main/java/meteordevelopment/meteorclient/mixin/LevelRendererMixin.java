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
import meteordevelopment.meteorclient.systems.modules.render.*;
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
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.level.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
    private void onSetLevel(ClientLevel level, CallbackInfo ci) {
        esp = Modules.get().get(ESP.class);
        noRender = Modules.get().get(NoRender.class);
    }

    @Inject(method = "checkPoseStack", at = @At("HEAD"), cancellable = true)
    private void onCheckPoseStack(PoseStack poseStack, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "renderHitOutline", at = @At("HEAD"), cancellable = true)
    private void onDrawHighlightedHitOutline(PoseStack poseStack, VertexConsumer builder, double camX, double camY, double camZ, BlockOutlineRenderState state, int color, float width, CallbackInfo ci) {
        if (Modules.get().isActive(BlockSelection.class)) ci.cancel();
    }

    @ModifyArg(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;cullTerrain(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/culling/Frustum;Z)V"))
    private boolean update$cullTerraion$modifySpectator(boolean spectator) {
        return Modules.get().isActive(Freecam.class) || spectator;
    }

    // No Render

    @WrapWithCondition(method = "extractLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/WeatherEffectRenderer;extractRenderState(Lnet/minecraft/world/level/Level;IFLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/state/level/WeatherRenderState;)V"))
    private boolean extractLevel$noWeather(WeatherEffectRenderer instance, Level level, int ticks, float partialTicks, Vec3 cameraPos, WeatherRenderState renderState) {
        if (noRender.noWeather()) {
            renderState.intensity = 0;
            return false;
        }

        return true;
    }

    @WrapWithCondition(method = "extractLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/WorldBorderRenderer;extract(Lnet/minecraft/world/level/border/WorldBorder;FLnet/minecraft/world/phys/Vec3;DLnet/minecraft/client/renderer/state/level/WorldBorderRenderState;)V"))
    private boolean extractLevel$noWorldBorder(WorldBorderRenderer instance, WorldBorder border, float deltaPartialTick, Vec3 cameraPos, double renderDistance, WorldBorderRenderState state) {
        if (noRender.noWorldBorder()) {
            state.alpha = 0;
            return false;
        }

        return true;
    }

    @ModifyExpressionValue(method = "addSkyPass", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/state/level/CameraEntityRenderState;doesMobEffectBlockSky:Z", opcode = Opcodes.GETFIELD))
    private boolean modifyMobEffectBlocksSky(boolean original) {
        if (noRender.noBlindness() || noRender.noDarkness()) return false;
        return original;
    }

    // Entity Shaders

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void onRenderLevelHead(GraphicsResourceAllocator resourceAllocator, DeltaTracker deltaTracker, boolean renderOutline, CameraRenderState cameraState, Matrix4fc modelViewMatrix, GpuBufferSlice terrainFog, Vector4f fogColor, boolean shouldRenderSky, ChunkSectionsToRender chunkSectionsToRender, CallbackInfo ci) {
        PostProcessShaders.beginRender();
    }

    @Unique
    private final OutlineRenderCommandQueue outlineRenderCommandQueue = new OutlineRenderCommandQueue();

    @Unique
    private MultiBufferSource provider;

    @Unique
    private FeatureRenderDispatcher renderDispatcher;

    @Inject(method = "submitEntities", at = @At("TAIL"))
    private void onSubmitEntities(PoseStack poseStack, LevelRenderState levelRenderState, SubmitNodeCollector output, CallbackInfo ci) {
        if (renderDispatcher == null) {
            renderDispatcher = new FeatureRenderDispatcher(
                outlineRenderCommandQueue,
                mc.getModelManager(),
                new WrapperImmediateVertexConsumerProvider(() -> provider),
                mc.getAtlasManager(),
                NoopOutlineVertexConsumerProvider.INSTANCE,
                NoopImmediateVertexConsumerProvider.INSTANCE,
                mc.font,
                mc.gameRenderer.getGameRenderState()
            );
        }

        draw(levelRenderState, poseStack, PostProcessShaders.CHAMS, _ -> Color.WHITE);
        draw(levelRenderState, poseStack, PostProcessShaders.ENTITY_OUTLINE, entity -> esp.getColor(entity));
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

    @Inject(method = "lambda$addMainPass$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/OutlineBufferSource;endOutlineBatch()V", shift = At.Shift.AFTER))
    private void addMainPass$submitEntityVertices(CallbackInfo ci) {
        PostProcessShaders.submitEntityVertices();
    }

    @Inject(method = "resize", at = @At("HEAD"))
    private void onResize(int width, int height, CallbackInfo ci) {
        PostProcessShaders.onResized(width, height);
    }

    @Inject(method = "extractLevel", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/state/level/LevelRenderState;cloudColor:I", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
    private void extractLevel$cloudColor(DeltaTracker deltaTracker, Camera camera, float deltaPartialTick, CallbackInfo ci) {
        Ambience ambience = Modules.get().get(Ambience.class);

        if (ambience.isActive() && ambience.customCloudColor.get()) {
            levelRenderState.cloudColor = ambience.cloudColor.get().getPacked();
        }
    }

    // BreakIndicators

    @Inject(method = "extractBlockDestroyAnimation", at = @At("HEAD"), cancellable = true)
    private void onExtractBlockDestroyAnimation(CallbackInfo ci) {
        if (Modules.get().isActive(BreakIndicators.class) || Modules.get().get(NoRender.class).noBlockBreakOverlay()) {
            ci.cancel();
        }
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
    @Shadow
    @Final
    private LevelRenderState levelRenderState;
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
