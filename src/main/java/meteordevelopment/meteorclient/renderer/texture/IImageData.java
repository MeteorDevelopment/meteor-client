/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.renderer.texture;

import net.minecraft.util.Identifier;

public sealed interface IImageData permits StaticImageData, AnimatedImageData {
    boolean isAnimated();
    int width();
    int height();
    Identifier imageId();
}
