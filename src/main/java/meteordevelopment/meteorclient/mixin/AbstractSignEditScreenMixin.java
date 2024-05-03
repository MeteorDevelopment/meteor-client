/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.gui.screen.ingame.AbstractSignEditScreen;
import net.minecraft.text.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.stream.Stream;

@Mixin(AbstractSignEditScreen.class)
public abstract class AbstractSignEditScreenMixin {
    @ModifyExpressionValue(method = "<init>(Lnet/minecraft/block/entity/SignBlockEntity;ZZLnet/minecraft/text/Text;)V", at = @At(value = "INVOKE", target = "Ljava/util/stream/IntStream;mapToObj(Ljava/util/function/IntFunction;)Ljava/util/stream/Stream;"))
    private Stream<Text> modifyTranslatableText(Stream<Text> original) {
        return original.map(this::modifyText);
    }

    // based on https://github.com/JustAlittleWolf/ModDetectionPreventer
    @Unique
    private Text modifyText(Text message) {
        MutableText modified = MutableText.of(message.getContent());

        if (message.getContent() instanceof KeybindTextContent content) {
            String key = content.getKey();

            if (key.contains("meteor-client")) modified = MutableText.of(new PlainTextContent.Literal(key));
        }
        if (message.getContent() instanceof TranslatableTextContent content) {
            String key = content.getKey();

            if (key.contains("meteor-client")) modified = MutableText.of(new PlainTextContent.Literal(key));
        }

        modified.setStyle(message.getStyle());
        for (Text sibling : message.getSiblings()) {
            modified.append(modifyText(sibling));
        }

        return modified;
    }
}
