/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.utils.misc.Names;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Language;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;

@Mixin(TranslatableTextContent.class)
public class TranslatableTextContentMixin {
    @Unique private boolean forceEnglish = false;

    private static final Language EN_US = LanguageAccessor.create();

    @Redirect(method = "updateTranslations", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Language;getInstance()Lnet/minecraft/util/Language;"))
    private Language onUpdateTranslation() {
        return forceEnglish ? EN_US : Language.getInstance();
    }

    // Force English translation in Names.get() as Custom Fonts currently doesn't support English
    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(String key, String fallback, Object[] args, CallbackInfo info) {
        if (Arrays.stream(Thread.currentThread().getStackTrace()).anyMatch(e -> e.getClassName().equals(Names.class.getName())))
            forceEnglish = true;
    }
}
