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
import net.minecraft.world.level.material.FogType;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

// TODO(Ravel): can not resolve target class net.minecraft.client.Camera
// TODO(Ravel): can not resolve target class Camera
@Mixin(Camera.class)
public abstract class CameraMixin implements ICamera {
    // TODO(Ravel): Could not determine a single target
// TODO(Ravel): Could not determine a single target
    @Shadow
    private boolean thirdPerson;

    // TODO(Ravel): Could not determine a single target
// TODO(Ravel): Could not determine a single target
    @Shadow
    private float yaw;
    // TODO(Ravel): Could not determine a single target
// TODO(Ravel): Could not determine a single target
    @Shadow
    private float pitch;

    // TODO(Ravel): Could not determine a single target
// TODO(Ravel): Could not determine a single target
    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    // TODO(Ravel): no target class
// TODO(Ravel): no target class
    @Inject(method = "getSubmersionType", at = @At("HEAD"), cancellable = true)
    private void getSubmergedFluidState(CallbackInfoReturnable<FogType> ci) {
        if (Modules.get().get(NoRender.class).noLiquidOverlay()) ci.setReturnValue(FogType.NONE);
    }

    // TODO(Ravel): no target class
// TODO(Ravel): no target class
    @ModifyVariable(method = "clipToSpace", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private float modifyClipToSpace(float d) {
        if (Modules.get().get(Freecam.class).isActive()) return 0;

        CameraTweaks cameraTweaks = Modules.get().get(CameraTweaks.class);
        return cameraTweaks.isActive() ? (float) cameraTweaks.distance : d;
    }

    // TODO(Ravel): no target class
// TODO(Ravel): no target class
    @Inject(method = "clipToSpace", at = @At("HEAD"), cancellable = true)
    private void onClipToSpace(float desiredCameraDistance, CallbackInfoReturnable<Float> info) {
        if (Modules.get().get(CameraTweaks.class).clip()) {
            info.setReturnValue(desiredCameraDistance);
        }
    }

    // TODO(Ravel): no target class
// TODO(Ravel): no target class
    @Inject(method = "update", at = @At("TAIL"))
    private void onUpdateTail(Level area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickProgress, CallbackInfo ci) {
        if (Modules.get().isActive(Freecam.class)) {
            this.thirdPerson = true;
        }
    }

    // TODO(Ravel): no target class
// TODO(Ravel): no target class
    @ModifyArgs(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setPosition(DDD)V"))
    private void onUpdateSetPosArgs(Args args, @Local(argsOnly = true) float tickDelta) {
        Freecam freecam = Modules.get().get(Freecam.class);

        if (freecam.isActive()) {
            args.set(0, freecam.getX(tickDelta));
            args.set(1, freecam.getY(tickDelta));
            args.set(2, freecam.getZ(tickDelta));
        }
    }

    // TODO(Ravel): no target class
// TODO(Ravel): no target class
    @ModifyArgs(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V"))
    private void onUpdateSetRotationArgs(Args args, @Local(argsOnly = true) float tickDelta) {
        Freecam freecam = Modules.get().get(Freecam.class);
        FreeLook freeLook = Modules.get().get(FreeLook.class);

        if (freecam.isActive()) {
            args.set(0, (float) freecam.getYaw(tickDelta));
            args.set(1, (float) freecam.getPitch(tickDelta));
        } else if (Modules.get().isActive(HighwayBuilder.class)) {
            args.set(0, yaw);
            args.set(1, pitch);
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
