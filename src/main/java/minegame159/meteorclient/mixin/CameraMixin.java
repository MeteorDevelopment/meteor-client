/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.mixin;

import minegame159.meteorclient.mixininterface.IVec3d;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.render.CameraClip;
import minegame159.meteorclient.modules.render.Freecam;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow private Vec3d pos;

    @Shadow private boolean thirdPerson;

    @Shadow protected abstract void setRotation(float yaw, float pitch);

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
            ((IVec3d) pos).set(freecam.getX(tickDelta), freecam.getY(tickDelta), freecam.getZ(tickDelta));
            setRotation(freecam.getYaw(tickDelta), freecam.getPitch(tickDelta));
            this.thirdPerson = true;
        }
    }
}
