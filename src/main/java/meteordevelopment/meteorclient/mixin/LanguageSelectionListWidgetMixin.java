/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.resource.language.LanguageDefinition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Locale;
import java.util.Map;

@Mixin(targets = "net.minecraft.client.gui.screen.option.LanguageOptionsScreen$LanguageSelectionListWidget", remap = false)
public class LanguageSelectionListWidgetMixin {
    @ModifyReturnValue(method = "method_76282", at = @At("RETURN"))
    private static boolean modifySearch(boolean original, String string, Map.Entry<String, LanguageDefinition> entry) {
        return original || entry.getKey().toLowerCase(Locale.ROOT).contains(string.toLowerCase(Locale.ROOT));
    }
}
