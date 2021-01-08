/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.mixin;

import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.render.CameraClip;
import minegame159.meteorclient.modules.render.FreeRotate;
import minegame159.meteorclient.modules.render.Freecam;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow private boolean thirdPerson;

    @Shadow protected abstract void setRotation(float yaw, float pitch);

    @Shadow protected abstract void setPos(double x, double y, double z);

    @Shadow
    private float pitch;
    @Shadow
    private float yaw;

    @Inject(method = "clipToSpace", at = @At("HEAD"), cancellable = true)
    private void onClipToSpace(double desiredCameraDistance, CallbackInfoReturnable<Double> info) {
        if (ModuleManager.INSTANCE.isActive(CameraClip.class)) {
            info.setReturnValue(desiredCameraDistance);
        }
    }

    @Inject(method = "update", at = @At("TAIL"))
    private void update(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo info) {
        Freecam freecam = ModuleManager.INSTANCE.get(Freecam.class);

        if (freecam.isActive()) {
            setPos(freecam.getX(tickDelta), freecam.getY(tickDelta), freecam.getZ(tickDelta));
            setRotation(freecam.getYaw(tickDelta), freecam.getPitch(tickDelta));
            this.thirdPerson = true;
        }
    }

    @Inject(method = "update", at = @At(value = "INVOKE", target = "net/minecraft/client/render/Camera.moveBy(DDD)V", ordinal = 0))
    private void perspectiveUpdatePitchYaw(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo info) {
        if (ModuleManager.INSTANCE.get(FreeRotate.class).shouldRotate()) ModuleManager.INSTANCE.get(FreeRotate.class).setRotation(yaw, pitch);
    }

    @ModifyArgs(method = "update", at = @At(value = "INVOKE", target = "net/minecraft/client/render/Camera.setRotation(FF)V", ordinal = 0))
    private void fixRotation(Args args) {
        FreeRotate module = ModuleManager.INSTANCE.get(FreeRotate.class);

        if (module.shouldRotate()) {
            args.set(0, module.getYaw());
            args.set(1, module.getPitch());
        }
    }
}
