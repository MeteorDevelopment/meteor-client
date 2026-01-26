/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.renderer.texture;

import java.nio.ByteBuffer;

public interface ITextureGetter {
    int getWidth();
    int getHeight();
    void upload(ByteBuffer buffer);
}
