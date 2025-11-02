/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.utils.misc.MeteorTranslations;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(KeyBinding.Category.class)
public class KeyBindingCategoryMixin {
    @Shadow
    @Final
    private Identifier id;

    @ModifyReturnValue(method = "getLabel", at = @At("RETURN"))
    private Text modifyLabel(Text original) {
        if (id.getNamespace().equals(MeteorClient.MOD_ID)) return Text.literal(MeteorTranslations.translate(id.getPath()));
        return original;
    }
}
