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
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.SpecialGuiElementRenderer;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.fog.FogRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.profiler.Profilers;
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

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    public abstract void updateCrosshairTarget(float tickDelta);

    @Shadow
    public abstract void reset();

    @Shadow
    @Final
    private Camera camera;

    @Unique
    private Renderer3D renderer;

    @Unique
    private Renderer3D depthRenderer;

    @Unique
    private final MatrixStack matrices = new MatrixStack();

    @Shadow
    protected abstract void bobView(MatrixStack matrices, float tickDelta);

    @Shadow
    protected abstract void tiltViewWhenHurt(MatrixStack matrices, float tickDelta);

    @Shadow
    @Final
    private BufferBuilderStorage buffers;

    @Shadow
    @Final
    private GuiRenderer guiRenderer;

    @Shadow
    @Final
    private FogRenderer fogRenderer;

    @Shadow
    @Final
    GuiRenderState guiState;

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;<init>(Lnet/minecraft/client/gui/render/state/GuiRenderState;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/command/RenderDispatcher;Ljava/util/List;)V"))
    private List<SpecialGuiElementRenderer<?>> meteor$addSpecialRenderers(List<SpecialGuiElementRenderer<?>> list) {
        list = new ArrayList<>(list);
        list.add(new CustomBannerGuiElementRenderer(buffers.getEntityVertexConsumers(), client.getAtlasManager()));

        return List.of(list.toArray(new SpecialGuiElementRenderer<?>[0]));
    }

    @Inject(method = "renderWorld", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V", args = {"ldc=hand"}))
    private void onRenderWorld(RenderTickCounter tickCounter, CallbackInfo ci, @Local(ordinal = 0) Matrix4f projection, @Local(ordinal = 1) Matrix4f position, @Local(ordinal = 0) float tickDelta, @Local MatrixStack matrixStack) {
        if (!Utils.canUpdate()) return;

        Profilers.get().push(MeteorClient.MOD_ID + "_render");

        // Create renderer and event

        if (renderer == null) renderer = new Renderer3D(MeteorRenderPipelines.WORLD_COLORED_LINES, MeteorRenderPipelines.WORLD_COLORED);
        if (depthRenderer == null) depthRenderer = new Renderer3D(MeteorRenderPipelines.WORLD_COLORED_LINES_DEPTH, MeteorRenderPipelines.WORLD_COLORED_DEPTH);
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

        Profilers.get().pop();
    }

    @Inject(method = "renderWorld", at = @At("TAIL"))
    private void onRenderWorldTail(CallbackInfo info) {
        MeteorClient.EVENT_BUS.post(RenderAfterWorldEvent.get());
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", shift = At.Shift.AFTER))
    private void onRenderGui(RenderTickCounter tickCounter, boolean tick, CallbackInfo info) {
        if (client.currentScreen instanceof WidgetScreen widgetScreen) {
            guiState.clear();
            var mouseX = (int) client.mouse.getScaledX(client.getWindow());
            var mouseY = (int) client.mouse.getScaledY(client.getWindow());

            var context = new DrawContext(client, guiState, mouseX, mouseY);

            widgetScreen.renderCustom(context, mouseX, mouseY, tickCounter.getDynamicDeltaTicks());

            RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(client.getFramebuffer().getDepthAttachment(), 1.0);
            guiRenderer.render(fogRenderer.getFogBuffer(FogRenderer.FogType.NONE));
            guiRenderer.incrementFrame();
        }
    }

    @Inject(method = "showFloatingItem", at = @At("HEAD"), cancellable = true)
    private void onShowFloatingItem(ItemStack floatingItem, CallbackInfo info) {
        if (floatingItem.getItem() == Items.TOTEM_OF_UNDYING && Modules.get().get(NoRender.class).noTotemAnimation()) {
            info.cancel();
        }
    }

    @ModifyExpressionValue(method = "renderWorld", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F", ordinal = 0))
    private float applyCameraTransformationsMathHelperLerpProxy(float original) {
        return Modules.get().get(NoRender.class).noNausea() ? 0 : original;
    }

    @ModifyReturnValue(method = "getFov", at = @At("RETURN"))
    private float modifyFov(float original) {
        return MeteorClient.EVENT_BUS.post(GetFovEvent.get(original)).fov;
    }

    // Freecam

    @Unique
    private boolean freecamSet = false;

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

    @Inject(method = "renderHand", at = @At("HEAD"), cancellable = true)
    private void renderHand(float tickProgress, boolean sleeping, Matrix4f positionMatrix, CallbackInfo ci) {
        if (!Modules.get().get(Freecam.class).renderHands() ||
            !Modules.get().get(Zoom.class).renderHands())
            ci.cancel();
    }
}
