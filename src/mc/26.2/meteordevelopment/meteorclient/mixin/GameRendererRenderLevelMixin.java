/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.MeteorRenderPipelines;
import meteordevelopment.meteorclient.renderer.Renderer3D;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.profiling.Profiler;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The arguments of GameRenderer#renderLevel are not the same in every version, so this
 * injection lives in the version tree instead of the shared sources.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererRenderLevelMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Final
    private Camera mainCamera;

    @Shadow
    @Final
    private GameRenderState gameRenderState;

    @Shadow
    protected abstract void bobView(final CameraRenderState cameraState, final PoseStack poseStack);

    @Shadow
    protected abstract void bobHurt(final CameraRenderState cameraState, final PoseStack poseStack);

    @Unique
    private Renderer3D renderer;

    @Unique
    private Renderer3D depthRenderer;

    @Unique
    private final PoseStack matrices = new PoseStack();

    @Inject(method = "renderLevel", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", args = "ldc=hand"))
    private void onRenderLevel(DeltaTracker deltaTracker, CallbackInfo ci, @Local(name = "projectionMatrix") Matrix4f projectionMatrix, @Local(name = "modelViewMatrix") Matrix4fc modelViewMatrix, @Local(name = "worldPartialTicks") float worldPartialTicks, @Local(name = "bobStack") PoseStack bobStack) {
        if (!Utils.canUpdate()) return;

        Profiler.get().push(MeteorClient.MOD_ID + "_render");

        // Create renderer and event

        if (renderer == null)
            renderer = new Renderer3D(MeteorRenderPipelines.WORLD_COLORED_LINES, MeteorRenderPipelines.WORLD_COLORED);
        if (depthRenderer == null)
            depthRenderer = new Renderer3D(MeteorRenderPipelines.WORLD_COLORED_LINES_DEPTH, MeteorRenderPipelines.WORLD_COLORED_DEPTH);
        Render3DEvent event = Render3DEvent.get(bobStack, renderer, depthRenderer, worldPartialTicks, mainCamera.position().x, mainCamera.position().y, mainCamera.position().z);

        // Update model view matrix

        RenderSystem.getModelViewStack().pushMatrix().mul(modelViewMatrix);

        matrices.pushPose();
        bobHurt(this.gameRenderState.levelRenderState.cameraRenderState, matrices);
        if (minecraft.options.bobView().get()) {
            bobView(this.gameRenderState.levelRenderState.cameraRenderState, matrices);
        }

        Matrix4f inverseBob = new Matrix4f(matrices.last().pose()).invert();
        RenderSystem.getModelViewStack().mul(inverseBob);
        matrices.popPose();

        // Call utility classes (apply bob correction when Iris shaders are active)

        Matrix4fc correctedPosition = modelViewMatrix;
        RenderUtils.updateScreenCenter(projectionMatrix, correctedPosition);
        NametagUtils.onRender(modelViewMatrix);

        // Render

        renderer.begin();
        depthRenderer.begin();
        MeteorClient.EVENT_BUS.post(event);
        renderer.render(bobStack);
        depthRenderer.render(bobStack);

        // Revert model view matrix

        RenderSystem.getModelViewStack().popMatrix();

        Profiler.get().pop();
    }
}
