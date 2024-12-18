/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixininterface;

import net.minecraft.client.gl.Framebuffer;

public interface IWorldRenderer {
    void meteor$pushEntityOutlineFramebuffer(Framebuffer framebuffer);

    void meteor$popEntityOutlineFramebuffer();
}
