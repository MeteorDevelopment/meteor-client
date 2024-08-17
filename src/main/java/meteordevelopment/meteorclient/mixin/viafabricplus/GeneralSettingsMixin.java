/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin.viafabricplus;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import de.florianmichael.viafabricplus.settings.impl.GeneralSettings;
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
