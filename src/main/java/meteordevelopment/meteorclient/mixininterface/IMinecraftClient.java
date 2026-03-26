/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixininterface;

import com.mojang.blaze3d.pipeline.RenderTarget;

public interface IMinecraftClient {
    void meteor$rightClick();

    void meteor$setFramebuffer(RenderTarget framebuffer);
}
