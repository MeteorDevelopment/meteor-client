/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.renderer.texture;

import net.minecraft.util.Identifier;

public record StaticImageData(Identifier imageId, int width, int height) implements IImageData {
    @Override
    public boolean isAnimated() {
        return false;
    }
}
