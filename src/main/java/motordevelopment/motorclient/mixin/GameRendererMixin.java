/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import motordevelopment.motorclient.MotorClient;
import motordevelopment.motorclient.events.render.GetFovEvent;
import motordevelopment.motorclient.events.render.Render3DEvent;
import motordevelopment.motorclient.events.render.RenderAfterWorldEvent;
import motordevelopment.motorclient.mixininterface.IVec3d;
import motordevelopment.motorclient.renderer.Renderer3D;
import motordevelopment.motorclient.systems.modules.Modules;
import motordevelopment.motorclient.systems.modules.player.LiquidInteract;
import motordevelopment.motorclient.systems.modules.player.NoMiningTrace;
import motordevelopment.motorclient.systems.modules.render.Freecam;
import motordevelopment.motorclient.systems.modules.render.NoRender;
import motordevelopment.motorclient.systems.modules.render.Zoom;
import motordevelopment.motorclient.systems.modules.world.HighwayBuilder;
import motordevelopment.motorclient.utils.Utils;
import motordevelopment.motorclient.utils.render.NametagUtils;
import motordevelopment.motorclient.utils.render.RenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.profiler.Profilers;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

    @Shadow
    protected abstract void bobView(MatrixStack matrices, float tickDelta);

    @Shadow
    protected abstract void tiltViewWhenHurt(MatrixStack matrices, float tickDelta);

    @Unique
    private Renderer3D renderer;

    @Unique
    private final MatrixStack matrices = new MatrixStack();

    @Inject(method = "renderWorld", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V", args = {"ldc=hand"}))
    private void onRenderWorld(RenderTickCounter tickCounter, CallbackInfo ci, @Local(ordinal = 0) Matrix4f projection, @Local(ordinal = 2) Matrix4f view, @Local(ordinal = 1) float tickDelta, @Local MatrixStack matrixStack) {
        if (!Utils.canUpdate()) return;

        Profilers.get().push(MotorClient.MOD_ID + "_render");

        // Create renderer and event

        if (renderer == null) renderer = new Renderer3D();
        Render3DEvent event = Render3DEvent.get(matrixStack, renderer, tickDelta, camera.getPos().x, camera.getPos().y, camera.getPos().z);

        // Call utility classes

        RenderUtils.updateScreenCenter(projection, view);
        NametagUtils.onRender(view);

        // Update model view matrix

        RenderSystem.getModelViewStack().pushMatrix().mul(view);

        matrices.push();

        tiltViewWhenHurt(matrices, camera.getLastTickDelta());
        if (client.options.getBobView().getValue()) bobView(matrices, camera.getLastTickDelta());

        RenderSystem.getModelViewStack().mul(matrices.peek().getPositionMatrix().invert());
        matrices.pop();

        // Render

        renderer.begin();
        MotorClient.EVENT_BUS.post(event);
        renderer.render(matrixStack);

        // Revert model view matrix

        RenderSystem.getModelViewStack().popMatrix();

        Profilers.get().pop();
    }

    @Inject(method = "renderWorld", at = @At("TAIL"))
    private void onRenderWorldTail(CallbackInfo info) {
        MotorClient.EVENT_BUS.post(RenderAfterWorldEvent.get());
    }

    @ModifyReturnValue(method = "findCrosshairTarget", at = @At("RETURN"))
    private HitResult onUpdateTargetedEntity(HitResult original, @Local HitResult hitResult) {
        if (Modules.get().get(NoMiningTrace.class).canWork(original instanceof EntityHitResult ehr ? ehr.getEntity() : null) && hitResult.getType() == HitResult.Type.BLOCK) {
            return hitResult;
        }
        return original;
    }

    @Redirect(method = "findCrosshairTarget", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;raycast(DFZ)Lnet/minecraft/util/hit/HitResult;"))
    private HitResult updateTargetedEntityEntityRayTraceProxy(Entity entity, double maxDistance, float tickDelta, boolean includeFluids) {
        if (Modules.get().isActive(LiquidInteract.class)) {
            HitResult result = entity.raycast(maxDistance, tickDelta, includeFluids);
            if (result.getType() != HitResult.Type.MISS) return result;

            return entity.raycast(maxDistance, tickDelta, true);
        }
        return entity.raycast(maxDistance, tickDelta, includeFluids);
    }

    @Inject(method = "showFloatingItem", at = @At("HEAD"), cancellable = true)
    private void onShowFloatingItem(ItemStack floatingItem, CallbackInfo info) {
        if (floatingItem.getItem() == Items.TOTEM_OF_UNDYING && Modules.get().get(NoRender.class).noTotemAnimation()) {
            info.cancel();
        }
    }

    @ModifyExpressionValue(method = "renderWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;lerp(FFF)F"))
    private float applyCameraTransformationsMathHelperLerpProxy(float original) {
        return Modules.get().get(NoRender.class).noNausea() ? 0 : original;
    }

    @ModifyReturnValue(method = "getFov",at = @At("RETURN"))
    private float modifyFov(float original) {
        return MotorClient.EVENT_BUS.post(GetFovEvent.get(original)).fov;
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
            double prevX = cameraE.prevX;
            double prevY = cameraE.prevY;
            double prevZ = cameraE.prevZ;
            float yaw = cameraE.getYaw();
            float pitch = cameraE.getPitch();
            float prevYaw = cameraE.prevYaw;
            float prevPitch = cameraE.prevPitch;

            if (highwayBuilder) {
                cameraE.setYaw(camera.getYaw());
                cameraE.setPitch(camera.getPitch());
            } else {
                ((IVec3d) cameraE.getPos()).motor$set(freecam.pos.x, freecam.pos.y - cameraE.getEyeHeight(cameraE.getPose()), freecam.pos.z);
                cameraE.prevX = freecam.prevPos.x;
                cameraE.prevY = freecam.prevPos.y - cameraE.getEyeHeight(cameraE.getPose());
                cameraE.prevZ = freecam.prevPos.z;
                cameraE.setYaw(freecam.yaw);
                cameraE.setPitch(freecam.pitch);
                cameraE.prevYaw = freecam.prevYaw;
                cameraE.prevPitch = freecam.prevPitch;
            }

            freecamSet = true;
            updateCrosshairTarget(tickDelta);
            freecamSet = false;

            ((IVec3d) cameraE.getPos()).motor$set(x, y, z);
            cameraE.prevX = prevX;
            cameraE.prevY = prevY;
            cameraE.prevZ = prevZ;
            cameraE.setYaw(yaw);
            cameraE.setPitch(pitch);
            cameraE.prevYaw = prevYaw;
            cameraE.prevPitch = prevPitch;
        }
    }

    @Inject(method = "renderHand", at = @At("HEAD"), cancellable = true)
    private void renderHand(Camera camera, float tickDelta, Matrix4f matrix4f, CallbackInfo ci) {
        if (!Modules.get().get(Freecam.class).renderHands() ||
            !Modules.get().get(Zoom.class).renderHands())
            ci.cancel();
    }
}
