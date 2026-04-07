/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.MixinPlugin;
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
import meteordevelopment.meteorclient.utils.render.CustomBannerGuiElementRenderer;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderBuffers;
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

import java.util.ArrayList;
import java.util.List;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin implements IGameRenderer {
    @Shadow
    @Final
    private Minecraft minecraft;

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
    protected abstract void bobView(final CameraRenderState cameraState, final PoseStack poseStack);

    @Shadow
    protected abstract void bobHurt(final CameraRenderState cameraState, final PoseStack poseStack);

    @Shadow
    @Final
    private RenderBuffers renderBuffers;

    @Shadow
    @Final
    private GuiRenderer guiRenderer;

    @Shadow
    @Final
    private FogRenderer fogRenderer;

    @Shadow
    @Final
    private GameRenderState gameRenderState;

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;<init>(Lnet/minecraft/client/renderer/state/gui/GuiRenderState;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher;Ljava/util/List;)V"))
    private List<PictureInPictureRenderer<?>> meteor$addSpecialRenderers(List<PictureInPictureRenderer<?>> list) {
        list = new ArrayList<>(list);
        list.add(new CustomBannerGuiElementRenderer(renderBuffers.bufferSource(), minecraft.getAtlasManager()));

        return List.of(list.toArray(new PictureInPictureRenderer<?>[0]));
    }

    // TODO(26.1)
    /*@Inject(method = "renderLevel", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", args = {"ldc=hand"}))
    private void onRenderLevel(DeltaTracker tickCounter, CallbackInfo ci, @Local(name = "projectionMatrix") Matrix4f projection, @Local(ordinal = 1) Matrix4f position, @Local(name = "worldPartialTicks") float tickDelta, @Local(name = "bobStack") PoseStack matrixStack) {
        if (!Utils.canUpdate()) return;

        Profiler.get().push(MeteorClient.MOD_ID + "_render");

        // Create renderer and event

        if (renderer == null)
            renderer = new Renderer3D(MeteorRenderPipelines.WORLD_COLORED_LINES, MeteorRenderPipelines.WORLD_COLORED);
        if (depthRenderer == null)
            depthRenderer = new Renderer3D(MeteorRenderPipelines.WORLD_COLORED_LINES_DEPTH, MeteorRenderPipelines.WORLD_COLORED_DEPTH);
        Render3DEvent event = Render3DEvent.get(matrixStack, renderer, depthRenderer, tickDelta, mainCamera.position().x, mainCamera.position().y, mainCamera.position().z);

        // Update model view matrix

        RenderSystem.getModelViewStack().pushMatrix().mul(position);

        matrices.pushPose();
        bobHurt(matrices, mainCamera.getCameraEntityPartialTicks(tickCounter));
        if (minecraft.options.bobView().get())
            bobView(matrices, mainCamera.getCameraEntityPartialTicks(tickCounter));

        Matrix4f inverseBob = new Matrix4f(matrices.last().pose()).invert();
        RenderSystem.getModelViewStack().mul(inverseBob);
        matrices.popPose();

        // Call utility classes (apply bob correction when Iris shaders are active)

        Matrix4f correctedPosition = MixinPlugin.isIrisPresent && RenderUtils.isShaderPackInUse() ? new Matrix4f(position).mul(inverseBob) : position;
        RenderUtils.updateScreenCenter(projection, correctedPosition);
        NametagUtils.onRender(position);

        // Render

        renderer.begin();
        depthRenderer.begin();
        MeteorClient.EVENT_BUS.post(event);
        renderer.render(matrixStack);
        depthRenderer.render(matrixStack);

        // Revert model view matrix

        RenderSystem.getModelViewStack().popMatrix();

        Profiler.get().pop();
    }*/

    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void onRenderLevelTail(CallbackInfo ci) {
        MeteorClient.EVENT_BUS.post(RenderAfterWorldEvent.get());
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", shift = At.Shift.AFTER))
    private void onRenderGui(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo ci) {
        if (minecraft.screen instanceof WidgetScreen widgetScreen) {
            gameRenderState.guiRenderState.reset();
            var mouseX = (int) minecraft.mouseHandler.getScaledXPos(minecraft.getWindow());
            var mouseY = (int) minecraft.mouseHandler.getScaledYPos(minecraft.getWindow());

            var context = new GuiGraphicsExtractor(minecraft, gameRenderState.guiRenderState, mouseX, mouseY);

            widgetScreen.renderCustom(context, mouseX, mouseY, deltaTracker.getGameTimeDeltaTicks());

            RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(minecraft.getMainRenderTarget().getDepthTexture(), 1.0);
            meteor$flushGuiState();
        }
    }

    @Inject(method = "displayItemActivation", at = @At("HEAD"), cancellable = true)
    private void onDisplayItemActivation(ItemStack itemStack, CallbackInfo ci) {
        if (itemStack.getItem() == Items.TOTEM_OF_UNDYING && Modules.get().get(NoRender.class).noTotemAnimation()) {
            ci.cancel();
        }
    }

    @ModifyExpressionValue(method = "renderLevel", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F", ordinal = 0))
    private float applyCameraTransformationsMathHelperLerpProxy(float original) {
        return Modules.get().get(NoRender.class).noNausea() ? 0 : original;
    }

    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void renderItemInHand(CameraRenderState cameraState, float deltaPartialTick, Matrix4fc modelViewMatrix, CallbackInfo ci) {
        if (!Modules.get().get(Freecam.class).renderHands() || !Modules.get().get(Zoom.class).renderHands()) {
            ci.cancel();
        }
    }

    @Override
    public void meteor$flushGuiState() {
        guiRenderer.render(fogRenderer.getBuffer(FogRenderer.FogMode.NONE));
        guiRenderer.endFrame();
    }
}
