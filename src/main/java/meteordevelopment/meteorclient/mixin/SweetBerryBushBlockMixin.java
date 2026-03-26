/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.NoSlow;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(SweetBerryBushBlock.class)
public abstract class SweetBerryBushBlockMixin {
    @Dynamic("Explicit 1.21.9 Support")
    @Inject(method = {
        "entityInside"
    }, at = @At("HEAD"), cancellable = true)
    private void onEntityCollision(CallbackInfo ci, @Local(argsOnly = true) Entity entity) {
        if (entity == mc.player && Modules.get().get(NoSlow.class).berryBush()) ci.cancel();
    }
}
