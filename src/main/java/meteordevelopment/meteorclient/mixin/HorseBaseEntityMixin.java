/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.mixininterface.IHorseBaseEntity;
import net.minecraft.entity.passive.HorseBaseEntity;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.EntityControl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HorseBaseEntity.class)
public abstract class HorseBaseEntityMixin implements IHorseBaseEntity {
    @Shadow protected abstract void setHorseFlag(int bitmask, boolean flag);

    @Override
    public void setSaddled(boolean saddled) {
        setHorseFlag(4, saddled);
    }
    
	@Inject(method = "getJumpStrength", at = @At("HEAD"), cancellable = true)
	private void mountJumpMultiplier(CallbackInfoReturnable<Double> cir) {
		EntityControl ec = Modules.get().get(EntityControl.class);
		if (ec.isActive())
		cir.setReturnValue(ec.getJumpHeight() / 5);
	}
}
