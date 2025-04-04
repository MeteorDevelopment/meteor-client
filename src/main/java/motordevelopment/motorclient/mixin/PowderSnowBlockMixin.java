/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import motordevelopment.motorclient.systems.modules.Modules;
import motordevelopment.motorclient.systems.modules.movement.Jesus;
import net.minecraft.block.PowderSnowBlock;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import static motordevelopment.motorclient.MotorClient.mc;

@Mixin(PowderSnowBlock.class)
public abstract class PowderSnowBlockMixin {
    @ModifyReturnValue(method = "canWalkOnPowderSnow", at = @At("RETURN"))
    private static boolean onCanWalkOnPowderSnow(boolean original, Entity entity) {
        if (entity == mc.player && Modules.get().get(Jesus.class).canWalkOnPowderSnow()) return true;
        return original;
    }
}
