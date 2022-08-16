/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.Ambience;
import net.minecraft.client.color.world.FoliageColors;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FoliageColors.class)
public class FoliageColorsMixin {
    @Inject(method = "getBirchColor", at = @At("HEAD"), cancellable = true)
    private static void onGetBirchColor(CallbackInfoReturnable<Integer> cir) {
        Ambience ambience = Modules.get().get(Ambience.class);
        if(ambience.isActive() && ambience.customFoliageColor.get()) {
            cir.setReturnValue(ambience.foliageColor.get().getPacked());
            cir.cancel();
        }
    }

    @Inject(method = "getSpruceColor", at = @At("HEAD"), cancellable = true)
    private static void onGetSpruceColor(CallbackInfoReturnable<Integer> cir) {
        Ambience ambience = Modules.get().get(Ambience.class);
        if(ambience.isActive() && ambience.customFoliageColor.get()) {
            cir.setReturnValue(ambience.foliageColor.get().getPacked());
            cir.cancel();
        }
    }
}
