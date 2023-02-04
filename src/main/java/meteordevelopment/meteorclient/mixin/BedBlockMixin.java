/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.NoFall;
import net.minecraft.block.BedBlock;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(BedBlock.class)
public class BedBlockMixin {
    @Inject(method = "bounceEntity", at = @At("HEAD"), cancellable = true)
    private void onBounceEntity(Entity entity, CallbackInfo info) {
        if (Modules.get().get(NoFall.class).cancelBounce() && entity == mc.player) info.cancel();
    }
}
