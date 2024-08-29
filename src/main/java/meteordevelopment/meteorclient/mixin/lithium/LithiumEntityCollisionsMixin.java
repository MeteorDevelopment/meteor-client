/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin.lithium;

import me.jellysquid.mods.lithium.common.entity.LithiumEntityCollisions;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.Collisions;
import net.minecraft.util.math.Box;
import net.minecraft.world.border.WorldBorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LithiumEntityCollisions.class)
public abstract class LithiumEntityCollisionsMixin {
    @Inject(method = "isWithinWorldBorder", at = @At("HEAD"), cancellable = true)
    private static void onIsWithinWorldBorder(WorldBorder border, Box box, CallbackInfoReturnable<Boolean> cir) {
        if (Modules.get().get(Collisions.class).ignoreBorder()) {
            cir.setReturnValue(true);
        }
    }
}
