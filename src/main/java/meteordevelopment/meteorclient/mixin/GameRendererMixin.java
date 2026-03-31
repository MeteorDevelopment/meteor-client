/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import meteordevelopment.meteorclient.MixinPlugin;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.GetFovEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.render.RenderAfterWorldEvent;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.mixininterface.IGameRenderer;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.renderer.MeteorRenderPipelines;
import meteordevelopment.meteorclient.renderer.Renderer3D;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import meteordevelopment.meteorclient.systems.modules.render.Zoom;
import meteordevelopment.meteorclient.systems.modules.world.HighwayBuilder;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.CustomBannerGuiElementRenderer;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.fog.FogRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.util.profiling.Profiler;
import org.joml.Matrix4f;
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

// TODO(Ravel): can not resolve target class net.minecraft.client.renderer.GameRenderer
// TODO(Ravel): can not resolve target class GameRenderer
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin implements IGameRenderer {
    // TODO(Ravel): Could not determine a single target
// TODO(Ravel): Could not determine a single target
    @Shadow
    @Final
    private Minecraft client;

    // TODO(Ravel): Could not determine a single target
// TODO(Ravel): Could not determine a single target
    @Shadow
    public abstract void updateCrosshairTarget(float tickDelta);

    // TODO(Ravel): Could not determine a single target
// TODO(Ravel): Could not determine a single target
    @Shadow
    public abstract void reset();

    // TODO(Ravel): Could not determine a single target
// TODO(Ravel): Could not determine a single target
    @Shadow
    @Final
    private Camera camera;

    @Unique
    private Renderer3D renderer;

    @Unique
    private Renderer3D depthRenderer;

    @Unique
    private final PoseStack matrices = new MatrixStack();

    // TODO(Ravel): Could not determine a single target
// TODO(Ravel): Could not determine a single target
    @Shadow
    protected abstract void bobView(PoseStack matrices, float tickDelta);

    // TODO(Ravel): Could not determine a single target
// TODO(Ravel): Could not determine a single target
    @Shadow
    protected abstract void tiltViewWhenHurt(PoseStack matrices, float tickDelta);

    // TODO(Ravel): Could not determine a single target
// TODO(Ravel): Could not determine a single target
    @Shadow
    @Final
    private RenderBuffers buffers;

    // TODO(Ravel): Could not determine a single target
// TODO(Ravel): Could not determine a single target
    @Shadow
    @Final
    private GuiRenderer guiRenderer;

    // TODO(Ravel): Could not determine a single target
// TODO(Ravel): Could not determine a single target
    @Shadow
    @Final
    private FogRenderer fogRenderer;

    // TODO(Ravel): Could not determine a single target
// TODO(Ravel): Could not determine a single target
    @Shadow
    @Final
    GuiRenderState guiState;

    // TODO(Ravel): no target class
// TODO(Ravel): no target class
    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;<init>(Lnet/minecraft/client/gui/render/state/GuiRenderState;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher;Ljava/util/List;)V"))
    private List<PictureInPictureRenderer<?>> meteor$addSpecialRenderers(List<PictureInPictureRenderer<?>> list) {
        list = new ArrayList<>(list);
        list.add(new CustomBannerGuiElementRenderer(buffers.getEntityVertexConsumers(), client.getAtlasManager()));

        return List.of(list.toArray(new SpecialGuiElementRenderer<?>[0]));
    }

    // TODO(Ravel): @At.args is not supported
// TODO(Ravel): no target class
// TODO(Ravel): @At.args is not supported
// TODO(Ravel): no target class
    @Inject(method = "renderWorld", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", args = {"ldc=hand"}))
    private void onRenderWorld(DeltaTracker tickCounter, CallbackInfo ci, @Local(ordinal = 0) Matrix4f projection, @Local(ordinal = 1) Matrix4f position, @Local(ordinal = 0) float tickDelta, @Local PoseStack matrixStack) {
        if (!Utils.canUpdate()) return;

        Profiler.get().push(MeteorClient.MOD_ID + "_render");

        // Create renderer and event

        if (renderer == null)
            renderer = new Renderer3D(MeteorRenderPipelines.WORLD_COLORED_LINES, MeteorRenderPipelines.WORLD_COLORED);
        if (depthRenderer == null)
            depthRenderer = new Renderer3D(MeteorRenderPipelines.WORLD_COLORED_LINES_DEPTH, MeteorRenderPipelines.WORLD_COLORED_DEPTH);
        Render3DEvent event = Render3DEvent.get(matrixStack, renderer, depthRenderer, tickDelta, camera.getCameraPos().x, camera.getCameraPos().y, camera.getCameraPos().z);

        // Update model view matrix

        RenderSystem.getModelViewStack().pushMatrix().mul(position);

        matrices.push();
        tiltViewWhenHurt(matrices, camera.getLastTickProgress());
        if (client.options.getBobView().getValue())
            bobView(matrices, camera.getLastTickProgress());

        Matrix4f inverseBob = new Matrix4f(matrices.peek().getPositionMatrix()).invert();
        RenderSystem.getModelViewStack().mul(inverseBob);
        matrices.pop();

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
    }

    // TODO(Ravel): no target class
// TODO(Ravel): no target class
    @Inject(method = "renderWorld", at = @At("TAIL"))
    private void onRenderWorldTail(CallbackInfo info) {
        MeteorClient.EVENT_BUS.post(RenderAfterWorldEvent.get());
    }

    // TODO(Ravel): no target class
// TODO(Ravel): no target class
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", shift = At.Shift.AFTER))
    private void onRenderGui(DeltaTracker tickCounter, boolean tick, CallbackInfo info) {
        if (client.currentScreen instanceof WidgetScreen widgetScreen) {
            guiState.clear();
            var mouseX = (int) client.mouse.getScaledX(client.getWindow());
            var mouseY = (int) client.mouse.getScaledY(client.getWindow());

            var context = new DrawContext(client, guiState, mouseX, mouseY);

            widgetScreen.renderCustom(context, mouseX, mouseY, tickCounter.getDynamicDeltaTicks());

            RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(client.getFramebuffer().getDepthAttachment(), 1.0);
            meteor$flushGuiState();
        }
    }

    // TODO(Ravel): no target class
// TODO(Ravel): no target class
    @Inject(method = "showFloatingItem", at = @At("HEAD"), cancellable = true)
    private void onShowFloatingItem(ItemStack floatingItem, CallbackInfo info) {
        if (floatingItem.getItem() == Items.TOTEM_OF_UNDYING && Modules.get().get(NoRender.class).noTotemAnimation()) {
            info.cancel();
        }
    }

    // TODO(Ravel): no target class
// TODO(Ravel): no target class
    @ModifyExpressionValue(method = "renderWorld", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F", ordinal = 0))
    private float applyCameraTransformationsMathHelperLerpProxy(float original) {
        return Modules.get().get(NoRender.class).noNausea() ? 0 : original;
    }

    // TODO(Ravel): no target class
// TODO(Ravel): no target class
    @ModifyReturnValue(method = "getFov", at = @At("RETURN"))
    private float modifyFov(float original) {
        return MeteorClient.EVENT_BUS.post(GetFovEvent.get(original)).fov;
    }

    // Freecam

    @Unique
    private boolean freecamSet = false;

    // TODO(Ravel): no target class
// TODO(Ravel): no target class
    @Inject(method = "updateCrosshairTarget", at = @At("HEAD"), cancellable = true)
    private void updateTargetedEntityInvoke(float tickDelta, CallbackInfo info) {
        Freecam freecam = Modules.get().get(Freecam.class);
        boolean highwayBuilder = Modules.get().isActive(HighwayBuilder.class);

        if ((freecam.isActive() || highwayBuilder) && client.getCameraEntity() != null && !freecamSet) {
            info.cancel();
            Entity cameraE = client.getCameraEntity();

            double x = cameraE.getX();
            double y = cameraE.getY();
            double z = cameraE.getZ();
            double lastX = cameraE.lastX;
            double lastY = cameraE.lastY;
            double lastZ = cameraE.lastZ;
            float yaw = cameraE.getYaw();
            float pitch = cameraE.getPitch();
            float lastYaw = cameraE.lastYaw;
            float lastPitch = cameraE.lastPitch;

            if (highwayBuilder) {
                cameraE.setYaw(camera.getYaw());
                cameraE.setPitch(camera.getPitch());
            } else {
                ((IVec3d) cameraE.getEntityPos()).meteor$set(freecam.pos.x, freecam.pos.y - cameraE.getEyeHeight(cameraE.getPose()), freecam.pos.z);
                cameraE.lastX = freecam.prevPos.x;
                cameraE.lastY = freecam.prevPos.y - cameraE.getEyeHeight(cameraE.getPose());
                cameraE.lastZ = freecam.prevPos.z;
                cameraE.setYaw(freecam.yaw);
                cameraE.setPitch(freecam.pitch);
                cameraE.lastYaw = freecam.lastYaw;
                cameraE.lastPitch = freecam.lastPitch;
            }

            freecamSet = true;
            updateCrosshairTarget(tickDelta);
            freecamSet = false;

            ((IVec3d) cameraE.getEntityPos()).meteor$set(x, y, z);
            cameraE.lastX = lastX;
            cameraE.lastY = lastY;
            cameraE.lastZ = lastZ;
            cameraE.setYaw(yaw);
            cameraE.setPitch(pitch);
            cameraE.lastYaw = lastYaw;
            cameraE.lastPitch = lastPitch;
        }
    }

    // TODO(Ravel): no target class
// TODO(Ravel): no target class
    @Inject(method = "renderHand", at = @At("HEAD"), cancellable = true)
    private void renderHand(float tickProgress, boolean sleeping, Matrix4f positionMatrix, CallbackInfo ci) {
        if (!Modules.get().get(Freecam.class).renderHands() ||
            !Modules.get().get(Zoom.class).renderHands())
            ci.cancel();
    }

    @Override
    public void meteor$flushGuiState() {
        guiRenderer.render(fogRenderer.getFogBuffer(FogRenderer.FogMode.NONE));
        guiRenderer.incrementFrame();
    }
}
