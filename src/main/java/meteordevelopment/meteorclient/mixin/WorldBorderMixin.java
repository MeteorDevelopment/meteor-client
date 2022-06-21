/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2022 Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.player.IgnoreBorder;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.border.WorldBorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldBorder.class)
public abstract class WorldBorderMixin {
    @Inject(method = "canCollide", at = @At("HEAD"), cancellable = true)
    private void canCollide(Entity entity, Box box, CallbackInfoReturnable<Boolean> infoR) {
        if (Modules.get().isActive(IgnoreBorder.class)) {
            infoR.setReturnValue(false);
        }
    }
    @Inject(method = "contains(Lnet/minecraft/util/math/BlockPos;)Z", at = @At("HEAD"), cancellable = true)
    private void contains(BlockPos pos, CallbackInfoReturnable<Boolean> infoR) {
        if (Modules.get().isActive(IgnoreBorder.class)) {
            infoR.setReturnValue(true);
        }
    }
}
