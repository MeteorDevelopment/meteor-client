/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.BoatMoveEvent;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.BoatFly;
import net.minecraft.entity.vehicle.AbstractBoatEntity;
import net.minecraft.entity.vehicle.BoatEntity;
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
        if ((Object) this instanceof BoatEntity boatEntity) {
            MeteorClient.EVENT_BUS.post(BoatMoveEvent.get(boatEntity));
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
