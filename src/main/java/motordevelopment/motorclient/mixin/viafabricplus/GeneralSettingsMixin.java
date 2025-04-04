/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.mixin.viafabricplus;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.viaversion.viafabricplus.settings.impl.GeneralSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GeneralSettings.class)
public abstract class GeneralSettingsMixin {
    // specifies the '2' value on this line:
    // public final ModeSetting multiplayerScreenButtonOrientation = new ModeSetting(this, Text.translatable("general_settings.viafabricplus.multiplayer_screen_button_orientation"), 2, ORIENTATION_OPTIONS);
    @ModifyExpressionValue(method = "<init>", at = @At(value = "CONSTANT", args = "intValue=2", ordinal = 1), remap = false)
    private int modifyDefaultPosition(int original) {
        return 4;
    }
}
