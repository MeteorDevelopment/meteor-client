/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.mixin;

import motordevelopment.motorclient.MotorClient;
import motordevelopment.motorclient.events.entity.BoatMoveEvent;
import motordevelopment.motorclient.systems.modules.Modules;
import motordevelopment.motorclient.systems.modules.movement.BoatFly;
import net.minecraft.entity.vehicle.AbstractBoatEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractBoatEntity.class)
public abstract class AbstractBoatEntityMixin {
    @Shadow
    private boolean pressingLeft;

    @Shadow
    private boolean pressingRight;

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/vehicle/AbstractBoatEntity;move(Lnet/minecraft/entity/MovementType;Lnet/minecraft/util/math/Vec3d;)V"))
    private void onTickInvokeMove(CallbackInfo info) {
        if ((Object) this instanceof AbstractBoatEntity boatEntity) {
            MotorClient.EVENT_BUS.post(BoatMoveEvent.get(boatEntity));
        }
    }

    @Redirect(method = "updatePaddles", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/vehicle/AbstractBoatEntity;pressingLeft:Z"))
    private boolean onUpdatePaddlesPressingLeft(AbstractBoatEntity instance) {
        if (Modules.get().isActive(BoatFly.class)) return false;
        return pressingLeft;
    }

    @Redirect(method = "updatePaddles", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/vehicle/AbstractBoatEntity;pressingRight:Z"))
    private boolean onUpdatePaddlesPressingRight(AbstractBoatEntity instance) {
        if (Modules.get().isActive(BoatFly.class)) return false;
        return pressingRight;
    }
}
