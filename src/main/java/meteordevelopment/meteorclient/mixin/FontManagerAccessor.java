/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import net.minecraft.client.font.FontManager;
import net.minecraft.client.font.FontStorage;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(FontManager.class)
public interface FontManagerAccessor {
    @Accessor("fontStorages")
    Map<Identifier, FontStorage> meteor$getFontStorages();
}
