/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.utils.misc.Names;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Language;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TranslatableTextContent.class)
public class TranslatableTextContentMixin {
    private static final Language EN_US = LanguageAccessor.create();

    // Force English translation in Names.get() as Custom Fonts currently doesn't support English
    @Redirect(method = "updateTranslations", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Language;getInstance()Lnet/minecraft/util/Language;"))
    @SuppressWarnings("removal")
    private Language onUpdateTranslation() {
        for (Class<?> clazz : new SecurityManager() {
            @Override
            public Class<?>[] getClassContext() {
                return super.getClassContext();
            }
        }.getClassContext()) {
            if (clazz.equals(Names.class)) {
                return EN_US;
            }
        }
        return Language.getInstance();
    }
}
