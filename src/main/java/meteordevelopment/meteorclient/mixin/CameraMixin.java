/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.GetFovEvent;
import meteordevelopment.meteorclient.mixininterface.ICamera;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.CameraTweaks;
import meteordevelopment.meteorclient.systems.modules.render.FreeLook;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import meteordevelopment.meteorclient.systems.modules.world.HighwayBuilder;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Camera.class)
public abstract class CameraMixin implements ICamera {
    @Shadow
    private boolean detached;

    @Shadow
    private float yRot;
    @Shadow
    private float xRot;

    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @Inject(method = "getFluidInCamera", at = @At("HEAD"), cancellable = true)
    private void getSubmergedFluidState(CallbackInfoReturnable<FogType> cir) {
        if (Modules.get().get(NoRender.class).noLiquidOverlay()) cir.setReturnValue(FogType.NONE);
    }

    @ModifyVariable(method = "getMaxZoom", at = @At("HEAD"), argsOnly = true, name = "cameraDist")
    private float modifyGetMaxZoom(float cameraDist) {
        if (Modules.get().get(Freecam.class).isActive()) return 0;

        CameraTweaks cameraTweaks = Modules.get().get(CameraTweaks.class);
        return cameraTweaks.isActive() ? (float) cameraTweaks.distance : cameraDist;
    }

    @Inject(method = "getMaxZoom", at = @At("HEAD"), cancellable = true)
    private void onGetMaxZoom(float desiredCameraDistance, CallbackInfoReturnable<Float> cir) {
        if (Modules.get().get(CameraTweaks.class).clip()) {
            cir.setReturnValue(desiredCameraDistance);
        }
    }

    @Inject(method = "alignWithEntity", at = @At("TAIL"))
    private void onAlignWithEntityTail(float partialTicks, CallbackInfo ci) {
        if (Modules.get().isActive(Freecam.class)) {
            this.detached = true;
        }
    }

    @ModifyArgs(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setPosition(DDD)V"))
    private void onAlignSetPosArgs(Args args, @Local(argsOnly = true, name = "partialTicks") float partialTicks) {
        Freecam freecam = Modules.get().get(Freecam.class);

        if (freecam.isActive()) {
            args.set(0, freecam.getX(partialTicks));
            args.set(1, freecam.getY(partialTicks));
            args.set(2, freecam.getZ(partialTicks));
        }
    }

    @ModifyArgs(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V"))
    private void onAlignSetRotationArgs(Args args, @Local(argsOnly = true, name = "partialTicks") float partialTicks) {
        Freecam freecam = Modules.get().get(Freecam.class);
        FreeLook freeLook = Modules.get().get(FreeLook.class);

        if (freecam.isActive()) {
            args.set(0, (float) freecam.getYaw(partialTicks));
            args.set(1, (float) freecam.getPitch(partialTicks));
        } else if (Modules.get().isActive(HighwayBuilder.class)) {
            args.set(0, yRot);
            args.set(1, xRot);
        } else if (freeLook.isActive()) {
            args.set(0, freeLook.cameraYaw);
            args.set(1, freeLook.cameraPitch);
        }
    }

    @Override
    public void meteor$setRot(double yaw, double pitch) {
        setRotation((float) yaw, (float) Mth.clamp(pitch, -90, 90));
    }

    @ModifyReturnValue(method = "getFov", at = @At("RETURN"))
    private float modifyFov(float original) {
        return MeteorClient.EVENT_BUS.post(GetFovEvent.get(original)).fov;
    }
}
