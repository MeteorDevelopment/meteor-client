/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.meteorclient.MixinPlugin;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.GetFovEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.render.RenderAfterWorldEvent;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.mixininterface.IGameRenderer;
import meteordevelopment.meteorclient.renderer.MeteorRenderPipelines;
import meteordevelopment.meteorclient.renderer.Renderer3D;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import meteordevelopment.meteorclient.systems.modules.render.Zoom;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin implements IGameRenderer {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Final
    private GameRenderState gameRenderState;

    @Shadow
    public abstract void resetData();

    @Shadow
    @Final
    private Camera mainCamera;

    @Unique
    private Renderer3D renderer;

    @Unique
    private Renderer3D depthRenderer;

    @Unique
    private final PoseStack matrices = new PoseStack();

    @Shadow
    protected abstract void bobView(CameraRenderState cameraRenderState, PoseStack matrices);

    @Shadow
    protected abstract void bobHurt(CameraRenderState cameraRenderState, PoseStack matrices);

    @Shadow
    @Final
    private GuiRenderer guiRenderer;

    @Shadow
    @Final
    private FogRenderer fogRenderer;

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;<init>(Lnet/minecraft/client/renderer/state/gui/GuiRenderState;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher;Ljava/util/List;)V"))
    private List<PictureInPictureRenderer<?>> meteor$addSpecialRenderers(List<PictureInPictureRenderer<?>> list) {
        return list;
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", args = {"ldc=hand"}))
    private void onRenderWorld(DeltaTracker tickCounter, CallbackInfo ci, @Local Matrix4f projectionMatrix, @Local Matrix4fc modelViewMatrix, @Local PoseStack bobStack) {
        if (!Utils.canUpdate()) return;

        Profiler.get().push(MeteorClient.MOD_ID + "_render");
        float tickDelta = tickCounter.getGameTimeDeltaPartialTick(false);

        // Create renderer and event

        if (renderer == null) renderer = new Renderer3D(MeteorRenderPipelines.WORLD_COLORED_LINES, MeteorRenderPipelines.WORLD_COLORED);
        if (depthRenderer == null) depthRenderer = new Renderer3D(MeteorRenderPipelines.WORLD_COLORED_LINES_DEPTH, MeteorRenderPipelines.WORLD_COLORED_DEPTH);
        Render3DEvent event = Render3DEvent.get(bobStack, renderer, depthRenderer, tickDelta, mainCamera.position().x, mainCamera.position().y, mainCamera.position().z);

        // Update model view matrix

        RenderSystem.getModelViewStack().pushMatrix().mul(modelViewMatrix);

        matrices.pushPose();
        bobHurt(gameRenderState.levelRenderState.cameraRenderState, matrices);
        if (minecraft.options.bobView().get())
            bobView(gameRenderState.levelRenderState.cameraRenderState, matrices);

        Matrix4f inverseBob = new Matrix4f(matrices.last().pose()).invert();
        RenderSystem.getModelViewStack().mul(inverseBob);
        matrices.popPose();

        // Call utility classes (apply bob correction when Iris shaders are active)

        Matrix4f modelView = new Matrix4f(modelViewMatrix);
        Matrix4f correctedPosition = MixinPlugin.isIrisPresent && RenderUtils.isShaderPackInUse() ? new Matrix4f(modelView).mul(inverseBob) : modelView;
        RenderUtils.updateScreenCenter(projectionMatrix, correctedPosition);
        NametagUtils.onRender(modelView);

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

    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void onRenderWorldTail(CallbackInfo info) {
        MeteorClient.EVENT_BUS.post(RenderAfterWorldEvent.get());
    }

    @Inject(method = "displayItemActivation", at = @At("HEAD"), cancellable = true)
    private void onShowFloatingItem(ItemStack floatingItem, CallbackInfo info) {
        if (floatingItem.getItem() == Items.TOTEM_OF_UNDYING && Modules.get().get(NoRender.class).noTotemAnimation()) {
            info.cancel();
        }
    }

    @ModifyExpressionValue(method = "renderLevel", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F", ordinal = 0))
    private float applyCameraTransformationsMathHelperLerpProxy(float original) {
        return Modules.get().get(NoRender.class).noNausea() ? 0 : original;
    }

    @ModifyExpressionValue(method = "renderLevel", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/state/level/CameraRenderState;hudFov:F"))
    private float modifyFov(float original) {
        return MeteorClient.EVENT_BUS.post(GetFovEvent.get(original)).fov;
    }

    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void renderHand(CameraRenderState cameraRenderState, float tickProgress, Matrix4fc positionMatrix, CallbackInfo ci) {
        if (!Modules.get().get(Freecam.class).renderHands() ||
            !Modules.get().get(Zoom.class).renderHands())
            ci.cancel();
    }

    @Override
    public void meteor$flushGuiState() {
        guiRenderer.render(fogRenderer.getBuffer(FogRenderer.FogMode.NONE));
    }
}
