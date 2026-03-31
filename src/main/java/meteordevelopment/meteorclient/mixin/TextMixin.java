/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.mixininterface.IText;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;

// TODO(Ravel): can not resolve target class Text
// TODO(Ravel): can not resolve target class Text
@Mixin(Text.class)
public interface TextMixin extends IText {
    @Override
    default void meteor$invalidateCache() {
    }
}
