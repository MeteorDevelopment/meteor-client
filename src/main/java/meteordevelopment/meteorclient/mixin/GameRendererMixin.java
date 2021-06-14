/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.renderer.Blur;
import meteordevelopment.meteorclient.renderer.Renderer3D;
import meteordevelopment.meteorclient.rendering.Matrices;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.player.LiquidInteract;
import meteordevelopment.meteorclient.systems.modules.player.NoMiningTrace;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import meteordevelopment.meteorclient.systems.modules.render.UnfocusedCPU;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow @Final private MinecraftClient client;

    @Shadow public abstract void updateTargetedEntity(float tickDelta);

    @Shadow public abstract void reset();

    @Unique private boolean a = false;
    @Unique private Renderer3D renderer;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRenderHead(float tickDelta, long startTime, boolean tick, CallbackInfo info) {
        if (Modules.get().isActive(UnfocusedCPU.class) && !client.isWindowFocused()) info.cancel();

        a = false;
    }

    @Inject(method = "renderWorld", at = @At("HEAD"))
    private void onRenderWorldHead(float tickDelta, long limitTime, MatrixStack matrix, CallbackInfo info) {
        Matrices.begin(matrix);
        Matrices.push();
        // TODO: Fix
        //RenderSystem.pushMatrix();

        a = true;
    }

    @Inject(method = "renderWorld", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V", args = { "ldc=hand" }), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void onRenderWorld(float tickDelta, long limitTime, MatrixStack matrices, CallbackInfo info, boolean bl, Camera camera, MatrixStack matrixStack, double d, Matrix4f matrix4f) {
        if (!Utils.canUpdate()) return;

        client.getProfiler().push("meteor-client_render");

        if (renderer == null) renderer = new Renderer3D();
        Render3DEvent event = Render3DEvent.get(matrices, renderer, tickDelta, camera.getPos().x, camera.getPos().y, camera.getPos().z);

        RenderUtils.updateScreenCenter();
        NametagUtils.onRender(matrices, matrix4f);

        renderer.begin();
        MeteorClient.EVENT_BUS.post(event);
        renderer.render(matrices);

        RenderSystem.applyModelViewMatrix();
        client.getProfiler().pop();
    }

    @Inject(method = "renderWorld", at = @At("TAIL"))
    private void onRenderWorldTail(CallbackInfo info) {
        Blur.render();
    }

    @Inject(method = "updateTargetedEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/projectile/ProjectileUtil;raycast(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;D)Lnet/minecraft/util/hit/EntityHitResult;"), cancellable = true)
    private void onUpdateTargetedEntity(float tickDelta, CallbackInfo info) {
        if (Modules.get().get(NoMiningTrace.class).canWork() && client.crosshairTarget.getType() == HitResult.Type.BLOCK) {
            client.getProfiler().pop();
            info.cancel();
        }
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;clear(IZ)V", ordinal = 0))
    private void onRenderBeforeGuiRender(float tickDelta, long startTime, boolean tick, CallbackInfo info) {
        if (a) {
            Matrices.pop();
            //RenderSystem.popMatrix();
        }
    }

    @Redirect(method = "updateTargetedEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;raycast(DFZ)Lnet/minecraft/util/hit/HitResult;"))
    private HitResult updateTargetedEntityEntityRayTraceProxy(Entity entity, double maxDistance, float tickDelta, boolean includeFluids) {
        if (Modules.get().isActive(LiquidInteract.class)) {
            HitResult result = entity.raycast(maxDistance, tickDelta, includeFluids);
            if (result.getType() != HitResult.Type.MISS) return result;

            return entity.raycast(maxDistance, tickDelta, true);
        }
        return entity.raycast(maxDistance, tickDelta, includeFluids);
    }

    @Inject(method = "bobViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void onBobViewWhenHurt(MatrixStack matrixStack, float f, CallbackInfo info) {
        if (Modules.get().get(NoRender.class).noHurtCam()) info.cancel();
    }

    @Inject(method = "showFloatingItem", at = @At("HEAD"), cancellable = true)
    private void onShowFloatingItem(ItemStack floatingItem, CallbackInfo info) {
        if (floatingItem.getItem() == Items.TOTEM_OF_UNDYING && Modules.get().get(NoRender.class).noTotemAnimation()) {
            info.cancel();
        }
    }

    @Redirect(method = "renderWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;lerp(FFF)F"))
    private float applyCameraTransformationsMathHelperLerpProxy(float delta, float first, float second) {
        if (Modules.get().get(NoRender.class).noNausea()) return 0;
        return MathHelper.lerp(delta, first, second);
    }

    // Freecam

    private boolean freecamSet = false;

    @Inject(method = "updateTargetedEntity", at = @At("INVOKE"), cancellable = true)
    private void updateTargetedEntityInvoke(float tickDelta, CallbackInfo info) {
        Freecam freecam = Modules.get().get(Freecam.class);

        if (freecam.isActive() && client.getCameraEntity() != null && !freecamSet) {
            info.cancel();
            Entity camera = client.getCameraEntity();

            double x = camera.getX();
            double y = camera.getY();
            double z = camera.getZ();
            double prevX = camera.prevX;
            double prevY = camera.prevY;
            double prevZ = camera.prevZ;
            float yaw = camera.getYaw();
            float pitch = camera.getPitch();
            float prevYaw = camera.prevYaw;
            float prevPitch = camera.prevPitch;

            ((IVec3d) camera.getPos()).set(freecam.pos.x, freecam.pos.y - camera.getEyeHeight(camera.getPose()), freecam.pos.z);
            camera.prevX = freecam.prevPos.x;
            camera.prevY = freecam.prevPos.y - camera.getEyeHeight(camera.getPose());
            camera.prevZ = freecam.prevPos.z;
            camera.setYaw(freecam.yaw);
            camera.setPitch(freecam.pitch);
            camera.prevYaw = freecam.prevYaw;
            camera.prevPitch = freecam.prevPitch;

            freecamSet = true;
            updateTargetedEntity(tickDelta);
            freecamSet = false;

            ((IVec3d) camera.getPos()).set(x, y, z);
            camera.prevX = prevX;
            camera.prevY = prevY;
            camera.prevZ = prevZ;
            camera.setYaw(yaw);
            camera.setPitch(pitch);
            camera.prevYaw = prevYaw;
            camera.prevPitch = prevPitch;
        }
    }

    @Inject(method = "renderHand", at = @At("INVOKE"), cancellable = true)
    private void renderHand(MatrixStack matrices, Camera camera, float tickDelta, CallbackInfo info) {
        if (!Modules.get().get(Freecam.class).renderHands()) info.cancel();
    }
}
