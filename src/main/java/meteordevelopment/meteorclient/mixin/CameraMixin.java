/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import meteordevelopment.meteorclient.mixininterface.ICamera;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.CameraTweaks;
import meteordevelopment.meteorclient.systems.modules.render.FreeLook;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import meteordevelopment.meteorclient.systems.modules.world.HighwayBuilder;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
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
    protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "getFluidInCamera", at = @At("HEAD"), cancellable = true)
    private void getSubmergedFluidState(CallbackInfoReturnable<FogType> cir) {
        if (Modules.get().get(NoRender.class).noLiquidOverlay()) cir.setReturnValue(FogType.NONE);
    }

    @ModifyVariable(method = "getMaxZoom", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private float modifyGetMaxZoom(float d) {
        if (Modules.get().get(Freecam.class).isActive()) return 0;

        CameraTweaks cameraTweaks = Modules.get().get(CameraTweaks.class);
        return cameraTweaks.isActive() ? (float) cameraTweaks.distance : d;
    }

    @Inject(method = "getMaxZoom", at = @At("HEAD"), cancellable = true)
    private void onGetMaxZoom(float desiredCameraDistance, CallbackInfoReturnable<Float> cir) {
        if (Modules.get().get(CameraTweaks.class).clip()) {
            cir.setReturnValue(desiredCameraDistance);
        }
    }

    @Inject(method = "setup", at = @At("TAIL"))
    private void onSetupTail(Level area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickProgress, CallbackInfo ci) {
        if (Modules.get().isActive(Freecam.class)) {
            this.detached = true;
        }
    }

    @ModifyArgs(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setPosition(DDD)V"))
    private void onDetupSetPosArgs(Args args, @Local(argsOnly = true) float tickDelta) {
        Freecam freecam = Modules.get().get(Freecam.class);

        if (freecam.isActive()) {
            args.set(0, freecam.getX(tickDelta));
            args.set(1, freecam.getY(tickDelta));
            args.set(2, freecam.getZ(tickDelta));
        }
    }

    @ModifyArgs(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V"))
    private void onSetupSetRotationArgs(Args args, @Local(argsOnly = true) float tickDelta) {
        Freecam freecam = Modules.get().get(Freecam.class);
        FreeLook freeLook = Modules.get().get(FreeLook.class);

        if (freecam.isActive()) {
            args.set(0, (float) freecam.getYaw(tickDelta));
            args.set(1, (float) freecam.getPitch(tickDelta));
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
}
