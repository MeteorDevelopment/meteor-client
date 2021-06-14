/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Fullbright;
import net.minecraft.client.render.SkyProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SkyProperties.class)
public class SkyPropertiesMixin {
    @Inject(method = "shouldBrightenLighting", at = @At(value = "HEAD"), cancellable = true)
    private void onShouldBrightenLighting(CallbackInfoReturnable<Boolean> cir) {
        Fullbright fullbright = Modules.get().get(Fullbright.class);

        if((fullbright.mode.get() == Fullbright.Mode.Luminance) && Fullbright.isEnabled()) {
            cir.setReturnValue(true);
        }
    }

}
