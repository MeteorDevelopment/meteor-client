/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.EntityControl;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.passive.StriderEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({AbstractHorseEntity.class, PigEntity.class, StriderEntity.class})
public abstract class RideableMixin {
    @Inject(method = "isSaddled", at = @At("HEAD"), cancellable = true)
    private void isSaddled(CallbackInfoReturnable<Boolean> info) {
        if (Modules.get().get(EntityControl.class).saddleSpoof()) info.setReturnValue(true);
    }
}
